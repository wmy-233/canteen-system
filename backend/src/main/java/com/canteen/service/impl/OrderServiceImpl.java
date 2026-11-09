package com.canteen.service.impl;

import com.canteen.common.BizException;
import com.canteen.common.PageResult;
import com.canteen.common.SecurityUtil;
import com.canteen.dto.OrderCreateDTO;
import com.canteen.entity.OrderInfo;
import com.canteen.entity.Stall;
import com.canteen.mapper.CouponMapper;
import com.canteen.mapper.DishMapper;
import com.canteen.mapper.OrderInfoMapper;
import com.canteen.mapper.OrderItemMapper;
import com.canteen.mapper.StallMapper;
import com.canteen.mapper.UserCouponMapper;
import com.canteen.service.OrderService;
import com.canteen.service.OrderTxService;
import com.canteen.service.RedisStockService;
import com.canteen.service.RedisSupport;
import com.canteen.vo.CouponVO;
import com.canteen.vo.DishVO;
import com.canteen.vo.OrderCreateVO;
import com.canteen.vo.OrderItemVO;
import com.canteen.vo.OrderVO;
import com.github.pagehelper.PageHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 订单实现（FR-STU-04 下单 / FR-STU-05 防超卖 / FR-MER-03 出餐）
 *
 * 防超卖三重保险执行顺序（UC-02）：
 *   1) Redis DECR 预减库存（结果 < 0 直接"已抢完"，不穿透数据库）
 *   2) Redisson 分布式锁（菜品维度，按 id 排序防死锁）
 *   3) MySQL 条件更新（WHERE daily_stock >= 数量，影响行数=0 回滚）
 *   Redis 不可用时：1) 跳过、2) 自动退化为无锁，3) 依旧零超卖（H6 降级）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final DishMapper dishMapper;
    private final StallMapper stallMapper;
    private final OrderInfoMapper orderInfoMapper;
    private final OrderItemMapper orderItemMapper;
    private final CouponMapper couponMapper;
    private final UserCouponMapper userCouponMapper;
    private final OrderTxService orderTxService;
    private final RedisStockService redisStockService;
    private final RedisSupport redisSupport;

    @Override
    public OrderCreateVO create(OrderCreateDTO dto) {
        Long userId = SecurityUtil.getUserId();
        List<OrderCreateDTO.Item> items = dto.getItems();
        if (items == null || items.isEmpty()) {
            throw new BizException("下单明细不能为空");
        }
        if (dto.getDeliveryType() == null) {
            dto.setDeliveryType(0);
        }

        // 1. 菜品校验：在售、同档口（跨档口按 O-1 口径提示拆单）
        List<Long> dishIds = items.stream().map(OrderCreateDTO.Item::getDishId)
                .distinct().collect(Collectors.toList());
        List<DishVO> dishes = dishMapper.selectByIds(dishIds);
        if (dishes.size() != dishIds.size()) {
            throw new BizException("存在无效或已删除的菜品");
        }
        Long stallId = dishes.get(0).getStallId();
        for (DishVO dish : dishes) {
            if (dish.getStatus() == null || dish.getStatus() != 1) {
                throw new BizException("《" + dish.getName() + "》已下架");
            }
            if (!dish.getStallId().equals(stallId)) {
                throw new BizException("跨档口商品不能一起下单，请分开提交");
            }
        }
        Stall stall = stallMapper.selectById(stallId);
        if (stall == null) {
            throw new BizException("档口不存在");
        }
        if (stall.getStatus() != 1) {
            throw new BizException("档口未营业，不可下单");
        }

        // 2. 算价（金额一律 BigDecimal）
        BigDecimal totalCalc = BigDecimal.ZERO;
        for (DishVO dish : dishes) {
            totalCalc = totalCalc.add(dish.getPrice().multiply(BigDecimal.valueOf(quantityOf(dto, dish.getId()))));
        }
        final BigDecimal total = totalCalc;

        // 3. 优惠券：手动指定优先（校验属人/未用/门槛），未传则自动取满足门槛最高一张（O-2）
        CouponVO coupon;
        Long couponRecordId;
        if (dto.getCouponId() != null) {
            coupon = userCouponMapper.selectMine(userId).stream()
                    .filter(c -> c.getId().equals(dto.getCouponId()))
                    .findFirst()
                    .orElseThrow(() -> new BizException("优惠券不存在"));
            validateCouponStatus(coupon, total);
            couponRecordId = userCouponMapper.selectUnusedRecordId(userId, coupon.getId());
            if (couponRecordId == null) {
                throw new BizException("优惠券不可用");
            }
        } else {
            coupon = couponMapper.selectBestForUser(userId, total);
            couponRecordId = null;
        }
        BigDecimal discount = coupon != null ? coupon.getAmount() : BigDecimal.ZERO;
        BigDecimal payCalc = total.subtract(discount);
        if (payCalc.compareTo(BigDecimal.ZERO) < 0) {
            payCalc = BigDecimal.ZERO;
        }
        final BigDecimal pay = payCalc;

        // 4. 第一重：Redis 预减库存（仅限量菜；抢完直接返回，不进事务）
        List<com.canteen.entity.Dish> limitedDishes = dishes.stream()
                .filter(d -> d.getDailyStock() > 0)
                .map(d -> {
                    com.canteen.entity.Dish entity = new com.canteen.entity.Dish();
                    entity.setId(d.getId());
                    entity.setDailyStock(d.getDailyStock());
                    return entity;
                })
                .collect(Collectors.toList());
        for (com.canteen.entity.Dish dish : limitedDishes) {
            boolean ok = redisStockService.tryDecrease(dish.getId(), quantityOf(dto, dish.getId()));
            if (!ok) {
                restorePreviouslyDecreased(limitedDishes, dto, dish.getId());
                String name = dishes.stream().filter(d -> d.getId().equals(dish.getId()))
                        .map(DishVO::getName).findFirst().orElse("菜品");
                throw new BizException("《" + name + "》已抢完");
            }
        }

        // 5. 第二重：菜品维度分布式锁（排序防死锁）→ 第三重：事务内条件更新兜底
        List<Long> lockIds = limitedDishes.stream().map(com.canteen.entity.Dish::getId)
                .sorted(Comparator.naturalOrder()).collect(Collectors.toList());
        final CouponVO finalCoupon = coupon;
        final Long finalCouponRecordId = couponRecordId;
        OrderInfo order;
        try {
            order = runWithLocks(lockIds, 0, () -> orderTxService.createOrderInTransaction(
                    userId, dto, dishes, finalCoupon, finalCouponRecordId, total, discount, pay));
        } catch (RuntimeException e) {
            // 事务整体回滚 → Redis 预减的余量全部补回（已在锁外的 DB 回滚恢复的是数据库侧）
            restoreAll(limitedDishes, dto);
            if (e instanceof BizException) {
                throw (BizException) e;
            }
            log.error("下单事务失败: userId={}, 原因: {}", userId, e.getMessage(), e);
            throw e;
        }

        // 6. 热销榜加分（H2：Redis ZSet；纯增强器，失败不影响订单）
        for (DishVO dish : dishes) {
            try {
                addScoreForRank(dish.getId(), quantityOf(dto, dish.getId()));
            } catch (Exception ignored) {
            }
        }

        OrderCreateVO vo = new OrderCreateVO();
        vo.setOrderId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setPickupCode(order.getPickupCode());
        vo.setTotalAmount(total);
        vo.setDiscountAmount(discount);
        vo.setPayAmount(pay);
        vo.setDeliveryType(order.getDeliveryType());
        log.info("下单成功: userId={}, orderNo={}, pay={}", userId, order.getOrderNo(), pay);
        return vo;
    }

    @Override
    public PageResult<OrderVO> myOrders(Integer status, Integer pageNum, Integer pageSize) {
        Long userId = SecurityUtil.getUserId();
        pageNum = pageNum == null ? 1 : pageNum;
        pageSize = pageSize == null ? 10 : pageSize;
        PageHelper.startPage(pageNum, pageSize);
        List<OrderVO> list = orderInfoMapper.selectPageByUser(userId, status);
        fillItems(list);
        return PageResult.build(list, pageNum, pageSize);
    }

    @Override
    public OrderVO detail(Long orderId) {
        OrderVO vo = orderInfoMapper.selectVoById(orderId);
        if (vo == null) {
            throw new BizException("订单不存在");
        }
        // 学生端只可查看本人订单；商家/管理员侧走各自接口
        Long userId = SecurityUtil.getUserId();
        if (!userId.equals(vo.getUserId())) {
            throw new BizException("订单不存在");
        }
        vo.setItems(orderItemMapper.selectByOrderId(orderId).stream()
                .map(this::toItemVo).collect(Collectors.toList()));
        return vo;
    }

    @Override
    public void cancel(Long orderId) {
        Long userId = SecurityUtil.getUserId();
        OrderInfo order = orderInfoMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BizException("订单不存在");
        }
        if (order.getStatus() != 0) {
            throw new BizException("仅待处理的订单可取消");
        }
        if (order.getDeliveryType() != null && order.getDeliveryType() == 1
                && order.getDeliveryStatus() != null && order.getDeliveryStatus() >= 1) {
            throw new BizException("配送单已指派骑手，请联系管理员取消");
        }
        orderTxService.cancelOrderInTransaction(order);
        log.info("取消订单并退款: userId={}, orderNo={}", userId, order.getOrderNo());
    }

    @Override
    public PageResult<OrderVO> ordersForMerchant(Long merchantUserId, Integer status,
                                                 Integer pageNum, Integer pageSize) {
        List<Long> stallIds = stallMapper.selectByMerchantUserId(merchantUserId).stream()
                .map(Stall::getId).collect(Collectors.toList());
        if (stallIds.isEmpty()) {
            throw new BizException("您还没有档口");
        }
        pageNum = pageNum == null ? 1 : pageNum;
        pageSize = pageSize == null ? 10 : pageSize;
        PageHelper.startPage(pageNum, pageSize);
        List<OrderVO> list = orderInfoMapper.selectPageByStallIds(stallIds, status);
        fillItems(list);
        return PageResult.build(list, pageNum, pageSize);
    }

    @Override
    public void verify(Long merchantUserId, Long orderId) {
        OrderInfo order = orderInfoMapper.selectById(orderId);
        if (order == null) {
            throw new BizException("订单不存在");
        }
        Stall stall = stallMapper.selectById(order.getStallId());
        if (stall == null || !stall.getMerchantUserId().equals(merchantUserId)) {
            log.warn("核销越权被拦截: orderId={}, operator={}", orderId, merchantUserId);
            throw new BizException("只能核销本档口订单");
        }
        if (order.getDeliveryType() != null && order.getDeliveryType() == 1) {
            throw new BizException("配送单由管理员侧流转，无需档口核销");
        }
        int rows = orderInfoMapper.updateStatusConditional(orderId, 0, 1);
        if (rows == 0) {
            throw new BizException("订单状态已变化，核销失败");
        }
        log.info("核销出餐: orderId={}, 取餐码={}, operator={}", orderId, order.getPickupCode(), merchantUserId);
    }

    // ---------------- internal ----------------

    private void validateCouponStatus(CouponVO coupon, BigDecimal total) {
        if (coupon.getStatus() == null || coupon.getStatus() != 1) {
            throw new BizException("优惠券已停发");
        }
        if (coupon.getExpireTime() != null && coupon.getExpireTime().isBefore(LocalDateTime.now())) {
            throw new BizException("优惠券已过期");
        }
        if (coupon.getThreshold().compareTo(total) > 0) {
            throw new BizException("未达到满减门槛（需满 " + coupon.getThreshold() + " 元）");
        }
    }

    private void addScoreForRank(Long dishId, int quantity) {
        if (!redisSupport.isAvailable()) {
            return;    // 降级模式：展示口径走数据库 sale_count
        }
        redisSupport.getClient().getScoredSortedSet(DishServiceImpl.RANK_KEY)
                .addScore(String.valueOf(dishId), quantity);
    }

    /** 抢完：恢复本单中在该菜品之前已预减成功的余量 */
    private void restorePreviouslyDecreased(List<com.canteen.entity.Dish> limitedDishes,
                                            OrderCreateDTO dto, Long failedDishId) {
        for (com.canteen.entity.Dish dish : limitedDishes) {
            if (dish.getId().equals(failedDishId)) {
                break;
            }
            redisStockService.restore(dish.getId(), quantityOf(dto, dish.getId()));
        }
    }

    /** 事务回滚：本单所有限量菜余量补偿（数据库侧已随回滚恢复） */
    private void restoreAll(List<com.canteen.entity.Dish> limitedDishes, OrderCreateDTO dto) {
        for (com.canteen.entity.Dish dish : limitedDishes) {
            redisStockService.restore(dish.getId(), quantityOf(dto, dish.getId()));
        }
    }

    /** 多把 Redisson 锁按序获取（经 RedisSupport.withLock 保持降级兼容） */
    private OrderInfo runWithLocks(List<Long> lockIds, int index, Supplier<OrderInfo> action) {
        if (index >= lockIds.size()) {
            return action.get();
        }
        Long dishId = lockIds.get(index);
        return redisSupport.withLock("lock:dish:stock:" + dishId, 8,
                () -> runWithLocks(lockIds, index + 1, action));
    }

    private Integer quantityOf(OrderCreateDTO dto, Long dishId) {
        for (OrderCreateDTO.Item item : dto.getItems()) {
            if (item.getDishId().equals(dishId)) {
                return item.getQuantity();
            }
        }
        return 0;
    }

    private void fillItems(List<OrderVO> orders) {
        for (OrderVO order : orders) {
            order.setItems(orderItemMapper.selectByOrderId(order.getId()).stream()
                    .map(this::toItemVo).collect(Collectors.toList()));
        }
    }

    private OrderItemVO toItemVo(com.canteen.entity.OrderItem item) {
        OrderItemVO vo = new OrderItemVO();
        vo.setId(item.getId());
        vo.setDishId(item.getDishId());
        vo.setDishName(item.getDishName());
        vo.setPrice(item.getPrice());
        vo.setQuantity(item.getQuantity());
        vo.setSubtotal(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        return vo;
    }
}

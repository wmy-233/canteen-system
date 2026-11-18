package com.canteen.service.impl;

import com.canteen.common.BizException;
import com.canteen.common.PageResult;
import com.canteen.common.SecurityUtil;
import com.canteen.dto.OrderCreateDTO;
import com.canteen.entity.Dish;
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
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 订单实现：防超卖三重保险 + 跨档口原子批量下单
 *
 * 1) Redis DECR 预减库存（结果 < 0 直接"已抢完"，不穿透数据库）
 * 2) Redisson 分布式锁（菜品维度，按 id 排序防死锁）
 * 3) MySQL 条件更新（WHERE daily_stock >= 数量，影响行数=0 回滚）
 * Redis 不可用时自动降级，数据库条件更新依旧零超卖。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final DishMapper dishMapper;
    private final StallMapper stallMapper;
    private final OrderInfoMapper orderInfoMapper;
    private final OrderItemMapper orderItemMapper;
    private final CouponMapper couponMapper;
    private final UserCouponMapper userCouponMapper;
    private final OrderTxService orderTxService;
    private final RedisStockService redisStockService;
    private final RedisSupport redisSupport;
    private final com.canteen.common.MessageSender messageSender;

    @Override
    public OrderCreateVO create(OrderCreateDTO dto) {
        Long userId = SecurityUtil.getUserId();
        OrderTxService.OrderContext ctx = prepare(dto, userId, true);

        // 第一重：Redis 预减库存（仅限量菜）
        List<Dish> limited = limitedDishes(ctx.dishes);
        List<Long> preDecreased = new ArrayList<>();
        for (Dish dish : limited) {
            int qty = quantityOf(dto, dish.getId());
            if (!redisStockService.tryDecrease(dish.getId(), qty)) {
                for (Long id : preDecreased) {
                    redisStockService.restore(id, quantityOf(dto, id));
                }
                String name = ctx.dishes.stream().filter(d -> d.getId().equals(dish.getId()))
                        .map(DishVO::getName).findFirst().orElse("菜品");
                throw new BizException("《" + name + "》已抢完");
            }
            preDecreased.add(dish.getId());
        }

        // 第二重锁 + 第三重条件更新（事务内）
        List<Long> lockIds = limited.stream().map(Dish::getId)
                .sorted(Comparator.naturalOrder()).collect(Collectors.toList());
        OrderInfo order;
        try {
            order = runWithLocks(lockIds, 0, () -> orderTxService.createOrderInTransaction(
                    userId, dto, ctx.dishes, null,
                    ctx.couponUserCouponId, ctx.total, ctx.discount, ctx.pay));
        } catch (RuntimeException e) {
            for (Long id : preDecreased) {
                redisStockService.restore(id, quantityOf(dto, id));
            }
            if (e instanceof BizException) throw (BizException) e;
            log.error("下单事务失败: userId={}, 原因: {}", userId, e.getMessage(), e);
            throw e;
        }

        addRankScores(ctx);
        OrderCreateVO vo = toVO(order, ctx);
        log.info("下单成功: userId={}, orderNo={}, pay={}", userId, order.getOrderNo(), ctx.pay);
        return vo;
    }

    /**
     * 跨档口原子批量下单：所有子单在同一个事务内落库。
     * 前端一次性提交多个档口的 items，任一档口失败 → 全部回滚，
     * 彻底消除"逐单提交导致 A 成功 B 失败、钱付了一半"的问题。
     */
    @Override
    public List<OrderCreateVO> createBatch(List<OrderCreateDTO> dtos) {
        Long userId = SecurityUtil.getUserId();
        if (dtos == null || dtos.isEmpty()) {
            throw new BizException("下单明细不能为空");
        }
        if (dtos.size() > 10) {
            throw new BizException("一次最多提交 10 个档口");
        }
        String batchNo = "B" + LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + String.format("%04d", RANDOM.nextInt(10000));

        // 1. 逐组校验 + 算价 + 选券（纯内存，不落库）
        // 多档口时不自动选券：同一张券不能在同批次里被核销两次
        java.util.Set<Long> usedCoupons = new java.util.HashSet<>();
        List<OrderTxService.OrderContext> contexts = new ArrayList<>();
        for (OrderCreateDTO dto : dtos) {
            OrderTxService.OrderContext ctx = prepare(dto, userId, dtos.size() == 1);
            if (ctx.couponUserCouponId != null && !usedCoupons.add(ctx.couponUserCouponId)) {
                ctx = new OrderTxService.OrderContext(ctx.dto, ctx.dishes, null,
                        ctx.total, BigDecimal.ZERO, ctx.total);
            }
            contexts.add(ctx);
        }

        // 2. Redis 预减按 dishId 聚合（跨组同一道菜只初始化一次）
        Map<Long, Integer> need = new LinkedHashMap<>();
        for (OrderTxService.OrderContext ctx : contexts) {
            for (Dish dish : limitedDishes(ctx.dishes)) {
                need.merge(dish.getId(), quantityOf(ctx.dto, dish.getId()), Integer::sum);
            }
        }
        List<Long> preDecreased = new ArrayList<>();
        for (Map.Entry<Long, Integer> e : need.entrySet()) {
            if (!redisStockService.tryDecrease(e.getKey(), e.getValue())) {
                for (Long id : preDecreased) {
                    redisStockService.restore(id, need.get(id));
                }
                throw new BizException("部分菜品已抢完，请检查购物车库存后重试");
            }
            preDecreased.add(e.getKey());
        }

        // 3. 所有菜加一把锁，一个事务创建全部子单
        List<Long> lockIds = need.keySet().stream().sorted(Comparator.naturalOrder())
                .collect(Collectors.toList());
        List<OrderInfo> orders;
        try {
            orders = runWithLocks(lockIds, 0, () ->
                    orderTxService.createBatchInTransaction(userId, contexts, batchNo));
        } catch (RuntimeException ex) {
            for (Long id : preDecreased) {
                redisStockService.restore(id, need.get(id));
            }
            if (ex instanceof BizException) throw (BizException) ex;
            log.error("批量下单事务失败: userId={}, batch={}, 原因: {}", userId, batchNo, ex.getMessage(), ex);
            throw ex;
        }

        // 4. 热销榜加分
        for (OrderTxService.OrderContext ctx : contexts) {
            addRankScores(ctx);
        }
        List<OrderCreateVO> result = new ArrayList<>();
        for (int i = 0; i < orders.size(); i++) {
            result.add(toVO(orders.get(i), contexts.get(i)));
        }
        log.info("批量下单成功: userId={}, batch={}, 子单数={}", userId, batchNo, result.size());
        return result;
    }

    /** 校验 + 算价 + 选券，供单笔与批量共用 */
    private OrderTxService.OrderContext prepare(OrderCreateDTO dto, Long userId, boolean allowAutoCoupon) {
        List<OrderCreateDTO.Item> items = dto.getItems();
        if (items == null || items.isEmpty()) {
            throw new BizException("下单明细不能为空");
        }
        if (dto.getDeliveryType() == null) {
            dto.setDeliveryType(0);
        }

        // 菜品校验：在售、同档口、档口营业
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
            if (dish.getDailyStock() != null && dish.getDailyStock() > 0
                    && quantityOf(dto, dish.getId()) > dish.getDailyStock()) {
                throw new BizException("《" + dish.getName() + "》库存不足");
            }
        }
        Stall stall = stallMapper.selectById(stallId);
        if (stall == null) throw new BizException("档口不存在");
        if (stall.getStatus() != 1) throw new BizException("档口未营业，不可下单");

        // 算价
        BigDecimal total = BigDecimal.ZERO;
        for (DishVO dish : dishes) {
            total = total.add(dish.getPrice().multiply(BigDecimal.valueOf(quantityOf(dto, dish.getId()))));
        }

        // 选券：手动优先，否则自动匹配最高可用券；同时反查 user_coupon 记录ID完成真正的核销
        CouponVO coupon;
        Long couponRecordId;
        if (dto.getCouponId() != null) {
            coupon = userCouponMapper.selectMine(userId).stream()
                    .filter(c -> c.getId().equals(dto.getCouponId()))
                    .findFirst()
                    .orElseThrow(() -> new BizException("优惠券不存在"));
            validateCouponStatus(coupon, total);
            couponRecordId = userCouponMapper.selectUnusedRecordId(userId, coupon.getId());
            if (couponRecordId == null) throw new BizException("优惠券不可用");
        } else if (allowAutoCoupon) {
            coupon = couponMapper.selectBestForUser(userId, total);
            couponRecordId = coupon == null ? null : userCouponMapper.selectUnusedRecordId(userId, coupon.getId());
        } else {
            coupon = null;
            couponRecordId = null;
        }
        BigDecimal discount = coupon != null ? coupon.getAmount() : BigDecimal.ZERO;
        BigDecimal pay = total.subtract(discount).max(BigDecimal.ZERO);

        return new OrderTxService.OrderContext(dto, dishes, couponRecordId, total, discount, pay);
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
        if (vo == null) throw new BizException("订单不存在");
        Long userId = SecurityUtil.getUserId();
        if (!userId.equals(vo.getUserId())) throw new BizException("订单不存在");
        vo.setItems(orderItemMapper.selectByOrderId(orderId).stream()
                .map(this::toItemVo).collect(Collectors.toList()));
        return vo;
    }

    @Override
    public void cancel(Long orderId) {
        Long userId = SecurityUtil.getUserId();
        OrderInfo order = orderInfoMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) throw new BizException("订单不存在");
        if (order.getStatus() != 0) throw new BizException("仅待处理的订单可取消");
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
        if (stallIds.isEmpty()) throw new BizException("您还没有档口");
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
        if (order == null) throw new BizException("订单不存在");
        Stall stall = stallMapper.selectById(order.getStallId());
        if (stall == null || !stall.getMerchantUserId().equals(merchantUserId)) {
            log.warn("核销越权被拦截: orderId={}, operator={}", orderId, merchantUserId);
            throw new BizException("只能核销本档口订单");
        }
        if (order.getDeliveryType() != null && order.getDeliveryType() == 1) {
            throw new BizException("配送单由骑手流转，无需档口核销");
        }
        if (orderInfoMapper.updateStatusConditional(orderId, 0, 1) == 0) {
            throw new BizException("订单状态已变化，核销失败");
        }
        messageSender.send(order.getUserId(), "你的订单已出餐 ✅",
                "档口已完成订单 " + order.getOrderNo() + "，取餐码 "
                        + order.getPickupCode() + "，请在取餐处出示该码",
                com.canteen.common.MessageSender.TYPE_ORDER);
        log.info("核销出餐: orderId={}, 取餐码={}, operator={}", orderId, order.getPickupCode(), merchantUserId);
    }

    // ---------------- internal ----------------

    private List<Dish> limitedDishes(List<DishVO> dishes) {
        return dishes.stream().filter(d -> d.getDailyStock() != null && d.getDailyStock() > 0)
                .map(d -> {
                    Dish e = new Dish();
                    e.setId(d.getId());
                    e.setDailyStock(d.getDailyStock());
                    return e;
                }).collect(Collectors.toList());
    }

    private OrderCreateVO toVO(OrderInfo order, OrderTxService.OrderContext ctx) {
        OrderCreateVO vo = new OrderCreateVO();
        vo.setOrderId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setPickupCode(order.getPickupCode());
        vo.setTotalAmount(ctx.total);
        vo.setDiscountAmount(ctx.discount);
        vo.setPayAmount(ctx.pay);
        vo.setDeliveryType(order.getDeliveryType());
        return vo;
    }

    private void addRankScores(OrderTxService.OrderContext ctx) {
        for (DishVO dish : ctx.dishes) {
            try {
                addScoreForRank(dish.getId(), quantityOf(ctx.dto, dish.getId()));
            } catch (Exception ignored) {
            }
        }
    }

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
        if (!redisSupport.isAvailable()) return;
        redisSupport.getClient().getScoredSortedSet(DishServiceImpl.RANK_KEY)
                .addScore(String.valueOf(dishId), quantity);
    }

    /** 多把 Redisson 锁按序获取（降级时直跑） */
    private <T> T runWithLocks(List<Long> lockIds, int index, Supplier<T> action) {
        if (index >= lockIds.size()) return action.get();
        Long dishId = lockIds.get(index);
        return redisSupport.withLock("lock:dish:stock:" + dishId, 8,
                () -> runWithLocks(lockIds, index + 1, action));
    }

    private Integer quantityOf(OrderCreateDTO dto, Long dishId) {
        for (OrderCreateDTO.Item item : dto.getItems()) {
            if (item.getDishId().equals(dishId)) return item.getQuantity();
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

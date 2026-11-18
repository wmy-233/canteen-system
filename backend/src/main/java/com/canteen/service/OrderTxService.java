package com.canteen.service;

import com.canteen.dto.OrderCreateDTO;
import com.canteen.entity.OrderInfo;
import com.canteen.entity.OrderItem;
import com.canteen.entity.WalletRecord;
import com.canteen.mapper.CartItemMapper;
import com.canteen.mapper.CouponMapper;
import com.canteen.mapper.DishMapper;
import com.canteen.mapper.OrderInfoMapper;
import com.canteen.mapper.OrderItemMapper;
import com.canteen.mapper.UserCouponMapper;
import com.canteen.mapper.UserMapper;
import com.canteen.mapper.WalletRecordMapper;
import com.canteen.vo.CouponVO;
import com.canteen.vo.DishVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 订单事务落库：建单 + 快照 + 取餐码 + 扣款 + 流水 + 券核销 + 库存/销量
 *
 * 高分增强：
 * 1) 订单记录 user_coupon_id / batch_no / limited，支持精确退券与退款回滚
 * 2) createBatchInTransaction：跨档口多子单在【同一个事务】内落库，消除"付了一半"问题
 * 3) rollbackOrderEffects：取消/投诉退款统一回滚 余额+券+库存+销量+Redis 余量+热销榜
 */
@Service
public class OrderTxService {

    private final OrderInfoMapper orderInfoMapper;
    private final OrderItemMapper orderItemMapper;
    private final UserMapper userMapper;
    private final WalletRecordMapper walletRecordMapper;
    private final UserCouponMapper userCouponMapper;
    private final CouponMapper couponMapper;
    private final DishMapper dishMapper;
    private final CartItemMapper cartItemMapper;
    private final com.canteen.mapper.StallMapper stallMapper;
    private final com.canteen.common.MessageSender messageSender;
    private final RedisStockService redisStockService;
    private final RedisSupport redisSupport;

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final DateTimeFormatter NO_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    public OrderTxService(OrderInfoMapper orderInfoMapper, OrderItemMapper orderItemMapper,
                          UserMapper userMapper, WalletRecordMapper walletRecordMapper,
                          UserCouponMapper userCouponMapper, CouponMapper couponMapper,
                          DishMapper dishMapper, CartItemMapper cartItemMapper,
                          com.canteen.mapper.StallMapper stallMapper,
                          com.canteen.common.MessageSender messageSender,
                          RedisStockService redisStockService, RedisSupport redisSupport) {
        this.orderInfoMapper = orderInfoMapper;
        this.orderItemMapper = orderItemMapper;
        this.userMapper = userMapper;
        this.walletRecordMapper = walletRecordMapper;
        this.userCouponMapper = userCouponMapper;
        this.couponMapper = couponMapper;
        this.dishMapper = dishMapper;
        this.cartItemMapper = cartItemMapper;
        this.stallMapper = stallMapper;
        this.messageSender = messageSender;
        this.redisStockService = redisStockService;
        this.redisSupport = redisSupport;
    }

    /** 单笔下单（兼容原接口） */
    @Transactional(rollbackFor = Exception.class)
    public OrderInfo createOrderInTransaction(Long userId, OrderCreateDTO dto,
                                              List<DishVO> dishList,
                                              CouponVO selectedCoupon, Long couponUserCouponId,
                                              BigDecimal total, BigDecimal discount, BigDecimal pay,
                                              BigDecimal deliveryFee) {
        return createOne(userId, dto, dishList, couponUserCouponId, total, discount, pay, deliveryFee, null);
    }

    /** 跨档口批量下单：所有子单共用一个事务，任一失败全部回滚 */
    @Transactional(rollbackFor = Exception.class)
    public List<OrderInfo> createBatchInTransaction(Long userId, List<OrderContext> contexts, String batchNo) {
        List<OrderInfo> created = new ArrayList<>();
        for (OrderContext c : contexts) {
            created.add(createOne(userId, c.dto, c.dishes, c.couponUserCouponId,
                    c.total, c.discount, c.pay, c.deliveryFee, batchNo));
        }
        return created;
    }

    /** 预校验后的下单上下文（由 OrderServiceImpl 组装） */
    public static class OrderContext {
        public final OrderCreateDTO dto;
        public final List<DishVO> dishes;
        public final Long couponUserCouponId;
        public final BigDecimal total;
        public final BigDecimal discount;
        public final BigDecimal pay;
        public final BigDecimal deliveryFee;

        public OrderContext(OrderCreateDTO dto, List<DishVO> dishes, Long couponUserCouponId,
                            BigDecimal total, BigDecimal discount, BigDecimal pay, BigDecimal deliveryFee) {
            this.dto = dto;
            this.dishes = dishes;
            this.couponUserCouponId = couponUserCouponId;
            this.total = total;
            this.discount = discount;
            this.pay = pay;
            this.deliveryFee = deliveryFee;
        }
    }

    private OrderInfo createOne(Long userId, OrderCreateDTO dto, List<DishVO> dishList,
                                Long couponUserCouponId, BigDecimal total, BigDecimal discount,
                                BigDecimal pay, BigDecimal deliveryFee, String batchNo) {
        Long stallId = dishList.get(0).getStallId();
        boolean delivery = dto.getDeliveryType() != null && dto.getDeliveryType() == 1;

        // 1. 限量菜条件更新扣库（第三重保险：影响行数=0 即失败回滚）
        for (DishVO dish : dishList) {
            int quantity = quantityOf(dto, dish.getId());
            if (dish.getDailyStock() != null && dish.getDailyStock() > 0) {
                int rows = dishMapper.decreaseStockConditional(dish.getId(), quantity);
                if (rows == 0) {
                    throw new com.canteen.common.BizException("《" + dish.getName() + "》已售罄");
                }
            }
        }

        // 2. 订单主表（精确记录券记录ID与批次号）
        OrderInfo order = new OrderInfo();
        order.setOrderNo(buildOrderNo());
        order.setUserId(userId);
        order.setStallId(stallId);
        order.setTotalAmount(total);
        order.setDiscountAmount(discount);
        order.setPayAmount(pay);
        order.setDeliveryFee(deliveryFee == null ? BigDecimal.ZERO : deliveryFee);
        order.setUserCouponId(couponUserCouponId);
        order.setBatchNo(batchNo);
        order.setStatus(0);
        order.setDeliveryType(dto.getDeliveryType());
        order.setRiderId(null);
        order.setDeliveryStatus(delivery ? 0 : null);
        order.setPickupCode(delivery ? null : uniquePickupCode(stallId));
        orderInfoMapper.insert(order);

        // 3. 明细快照 + 是否限量标记（退款时据此精确补库存）
        List<OrderItem> items = new ArrayList<>();
        for (DishVO dish : dishList) {
            OrderItem item = new OrderItem();
            item.setOrderId(order.getId());
            item.setDishId(dish.getId());
            item.setDishName(dish.getName());
            item.setPrice(dish.getPrice());
            item.setQuantity(quantityOf(dto, dish.getId()));
            item.setLimited(dish.getDailyStock() != null && dish.getDailyStock() > 0 ? 1 : 0);
            items.add(item);
        }
        orderItemMapper.insertBatch(items);

        // 4. 钱包扣款 + 流水
        int payRows = userMapper.deductBalance(userId, pay);
        if (payRows == 0) {
            throw new com.canteen.common.BizException("余额不足，请先充值");
        }
        WalletRecord record = new WalletRecord();
        record.setUserId(userId);
        record.setType(2);
        record.setAmount(pay);
        record.setRefOrderNo(order.getOrderNo());
        walletRecordMapper.insert(record);

        // 5. 优惠券核销（条件更新防并发重复核销）
        if (couponUserCouponId != null) {
            int used = userCouponMapper.markUsed(couponUserCouponId, userId);
            if (used == 0) {
                throw new com.canteen.common.BizException("优惠券不可用或已使用");
            }
        }

        // 6. 销量累加 + 清购物车
        for (DishVO dish : dishList) {
            dishMapper.increaseSaleCount(dish.getId(), quantityOf(dto, dish.getId()));
        }
        cartItemMapper.deleteByUserAndDishIds(userId,
                dishList.stream().map(DishVO::getId).collect(Collectors.toList()));

        // 7. 站内消息通知商家
        com.canteen.entity.Stall orderStall = stallMapper.selectById(stallId);
        if (orderStall != null && orderStall.getMerchantUserId() != null) {
            messageSender.send(orderStall.getMerchantUserId(), "收到新订单 🍜",
                    "来自同学 #" + userId + "，实付 ¥" + pay
                            + (delivery ? "（配送单，校园配送员可在跑腿大厅接单）" : "（取餐码 " + order.getPickupCode() + "），请及时备餐"),
                    com.canteen.common.MessageSender.TYPE_ORDER);
        }
        return order;
    }

    /**
     * 取消退款事务：0待处理 -> 2已取消；余额 + 券 + 库存 + 销量全部回滚
     */
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrderInTransaction(OrderInfo order) {
        int rows = orderInfoMapper.updateStatusConditional(order.getId(), 0, 2);
        if (rows == 0) {
            throw new com.canteen.common.BizException("订单状态已变化，取消失败");
        }
        refundBalance(order);
        rollbackOrderEffects(order);

        messageSender.send(order.getUserId(), "退款成功 💰",
                "订单 " + order.getOrderNo() + " 已取消，退款 ¥" + order.getPayAmount() + " 已原路退回校园卡",
                com.canteen.common.MessageSender.TYPE_ORDER);
        com.canteen.entity.Stall cancelStall = stallMapper.selectById(order.getStallId());
        if (cancelStall != null && cancelStall.getMerchantUserId() != null) {
            messageSender.send(cancelStall.getMerchantUserId(), "订单已取消",
                    "同学取消了订单 " + order.getOrderNo() + "（¥" + order.getPayAmount() + "），无需备餐",
                    com.canteen.common.MessageSender.TYPE_ORDER);
        }
    }

    /** 退余额 + 写退款流水（投诉裁定与取消共用） */
    public void refundBalance(OrderInfo order) {
        userMapper.addBalance(order.getUserId(), order.getPayAmount());
        WalletRecord refund = new WalletRecord();
        refund.setUserId(order.getUserId());
        refund.setType(3);
        refund.setAmount(order.getPayAmount());
        refund.setRefOrderNo(order.getOrderNo());
        walletRecordMapper.insert(refund);
    }

    /**
     * 订单副作用总回滚：库存 / 销量 / 优惠券 / Redis 余量 / 热销榜
     * 取消订单与"投诉成立退款"都调用，保证口径一致、不留下脏数据。
     */
    public void rollbackOrderEffects(OrderInfo order) {
        if (order == null) return;

        // 1) 精确退券：优先用订单上记录的 user_coupon_id（老订单退化到按金额匹配）
        Long ucId = order.getUserCouponId();
        if (ucId == null && order.getDiscountAmount() != null
                && order.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
            ucId = userCouponMapper.selectLatestUsedIdForRestore(order.getUserId(), order.getDiscountAmount());
        }
        if (ucId != null && ucId > 0) {
            userCouponMapper.restore(ucId, order.getUserId());
        }

        // 2) 按明细逐项回滚库存与销量（limited 标记来自下单时刻，避免商家改库存后口径失真）
        List<OrderItem> items = orderItemMapper.selectByOrderId(order.getId());
        for (OrderItem item : items) {
            Integer qty = item.getQuantity() == null ? 0 : item.getQuantity();
            if (qty <= 0) continue;
            if (Integer.valueOf(1).equals(item.getLimited())) {
                dishMapper.increaseStock(item.getDishId(), qty);
                redisStockService.restore(item.getDishId(), qty);
            }
            dishMapper.decreaseSaleCount(item.getDishId(), qty);
            if (redisSupport.isAvailable()) {
                try {
                    redisSupport.getClient().getScoredSortedSet(com.canteen.service.impl.DishServiceImpl.RANK_KEY)
                            .addScore(String.valueOf(item.getDishId()), -qty);
                } catch (Exception ignored) {
                }
            }
        }
    }

    private Integer quantityOf(OrderCreateDTO dto, Long dishId) {
        for (OrderCreateDTO.Item item : dto.getItems()) {
            if (item.getDishId().equals(dishId)) {
                return item.getQuantity();
            }
        }
        throw new com.canteen.common.BizException("下单明细与菜品不一致");
    }

    private String buildOrderNo() {
        return "CT" + LocalDateTime.now().format(NO_FORMATTER)
                + String.format("%04d", RANDOM.nextInt(10000));
    }

    /** 取餐码：4 位数字，当日档口内唯一；万一冲突就重试 */
    private String uniquePickupCode(Long stallId) {
        Set<String> used = new HashSet<>(orderInfoMapper.selectTodayPickupCodes(stallId));
        for (int i = 0; i < 50; i++) {
            String code = String.format("%04d", 1000 + RANDOM.nextInt(9000));
            if (!used.contains(code)) {
                return code;
            }
        }
        throw new com.canteen.common.BizException("取餐码生成失败，请重试");
    }
}

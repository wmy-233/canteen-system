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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 下单事务落库（A4 主链的"同一事务"：建单+明细快照+取餐码+扣款+流水+券核销+销量/库存）
 * 独立 Bean 是为了让 Spring 事务代理生效（外层 OrderServiceImpl 负责锁与 Redis 预减）
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

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final DateTimeFormatter NO_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    public OrderTxService(OrderInfoMapper orderInfoMapper, OrderItemMapper orderItemMapper,
                          UserMapper userMapper, WalletRecordMapper walletRecordMapper,
                          UserCouponMapper userCouponMapper, CouponMapper couponMapper,
                          DishMapper dishMapper, CartItemMapper cartItemMapper,
                          com.canteen.mapper.StallMapper stallMapper, com.canteen.common.MessageSender messageSender) {
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
    }

    /**
     * 事务内完成下单。调用方保证：菜品已校验、限量菜已过 Redis 预减并持锁。
     *
     * @param couponUserCouponId 自动/手动选中的 user_coupon 记录ID（可为 null）
     */
    @Transactional(rollbackFor = Exception.class)
    public OrderInfo createOrderInTransaction(Long userId, OrderCreateDTO dto,
                                              List<com.canteen.vo.DishVO> dishList,
                                              CouponVO selectedCoupon, Long couponUserCouponId,
                                              BigDecimal total, BigDecimal discount, BigDecimal pay) {
        Long stallId = dishList.get(0).getStallId();

        // 1. 限量菜条件更新扣库（第三重保险：影响行数=0 即失败回滚）
        for (com.canteen.vo.DishVO dish : dishList) {
            int quantity = quantityOf(dto, dish.getId());
            if (dish.getDailyStock() > 0) {
                int rows = dishMapper.decreaseStockConditional(dish.getId(), quantity);
                if (rows == 0) {
                    throw new com.canteen.common.BizException("《" + dish.getName() + "》已售罄");
                }
            }
        }

        // 2. 订单主表（取餐码：当日档口内唯一，4 位数字，O-4 口径）
        OrderInfo order = new OrderInfo();
        order.setOrderNo(buildOrderNo());
        order.setUserId(userId);
        order.setStallId(stallId);
        order.setTotalAmount(total);
        order.setDiscountAmount(discount);
        order.setPayAmount(pay);
        order.setStatus(0);
        order.setDeliveryType(dto.getDeliveryType());
        boolean delivery = dto.getDeliveryType() != null && dto.getDeliveryType() == 1;
        order.setRiderId(null);
        order.setDeliveryStatus(delivery ? 0 : null);
        order.setPickupCode(delivery ? null : uniquePickupCode(stallId));
        orderInfoMapper.insert(order);

        // 3. 明细快照：菜名/价格按下单时刻冻结（防改价影响历史订单）
        List<OrderItem> items = new ArrayList<>();
        for (com.canteen.vo.DishVO dish : dishList) {
            OrderItem item = new OrderItem();
            item.setOrderId(order.getId());
            item.setDishId(dish.getId());
            item.setDishName(dish.getName());
            item.setPrice(dish.getPrice());
            item.setQuantity(quantityOf(dto, dish.getId()));
            items.add(item);
        }
        orderItemMapper.insertBatch(items);

        // 4. 钱包扣款（原子条件更新：余额不足影响行数=0）+ 流水留痕
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

        // 6. 销量累加 + 清对应购物车
        for (com.canteen.vo.DishVO dish : dishList) {
            dishMapper.increaseSaleCount(dish.getId(), quantityOf(dto, dish.getId()));
        }
        cartItemMapper.deleteByUserAndDishIds(userId,
                dishList.stream().map(com.canteen.vo.DishVO::getId).collect(Collectors.toList()));

        // 7. 站内消息：通知商家端"接单了"
        com.canteen.entity.Stall orderStall = stallMapper.selectById(stallId);
        if (orderStall != null && orderStall.getMerchantUserId() != null) {
            String brief = "来自同学 #" + userId + "，";
            messageSender.send(orderStall.getMerchantUserId(),
                    "收到新订单 🍜",
                    brief + "实付 ¥" + pay + (delivery ? "（配送单，等待管理员指派骑手）" : "（取餐码 " + order.getPickupCode() + "），请及时备餐"),
                    com.canteen.common.MessageSender.TYPE_ORDER);
        }
        return order;
    }

    /**
     * 取消退款事务：0待处理 -> 2已取消；退钱+写退款流水+退回优惠券
     */
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrderInTransaction(OrderInfo order) {
        int rows = orderInfoMapper.updateStatusConditional(order.getId(), 0, 2);
        if (rows == 0) {
            throw new com.canteen.common.BizException("订单状态已变化，取消失败");
        }
        userMapper.addBalance(order.getUserId(), order.getPayAmount());
        WalletRecord refund = new WalletRecord();        refund.setUserId(order.getUserId());
        refund.setType(3);
        refund.setAmount(order.getPayAmount());
        refund.setRefOrderNo(order.getOrderNo());
        walletRecordMapper.insert(refund);

        // 优惠券退回（按减免金额匹配本人最近一张已用券，演示口径）
        if (order.getDiscountAmount() != null
                && order.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
            Long ucId = userCouponMapper.selectLatestUsedIdForRestore(order.getUserId(), order.getDiscountAmount());
            if (ucId != null) {
                userCouponMapper.restore(ucId, order.getUserId());
            }
        }
        // 站内消息：学生退款成功 + 商家订单被取消
        messageSender.send(order.getUserId(), "退款成功 💰",
                "订单 " + order.getOrderNo() + " 已取消，退款 ¥" + order.getPayAmount() + " 已原路退回校园卡",
                com.canteen.common.MessageSender.TYPE_ORDER);
        com.canteen.entity.Stall cancelStall = stallMapper.selectById(order.getStallId());
        if (cancelStall != null && cancelStall.getMerchantUserId() != null) {
            messageSender.send(cancelStall.getMerchantUserId(), "订单已取消",
                    "同学取消了订单 " + order.getOrderNo() + "（¥" + order.getPayAmount() + "），无需备餐",
                    com.canteen.common.MessageSender.TYPE_ORDER);
        }

        // 配送单取消：上未指派则复位（已指派/配送中不允许学生取消，Service 外层已拦截）
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

    /**
     * 取餐码（UC-02/O-4）：4 位数字，当日档口内唯一；并发重复极少但万一冲突就重试
     */
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

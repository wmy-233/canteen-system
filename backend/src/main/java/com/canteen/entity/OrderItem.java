package com.canteen.entity;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 订单明细实体：菜名/价格为下单时快照（防改价影响历史订单）
 */
@Data
public class OrderItem {

    private Long id;

    private Long orderId;

    private Long dishId;

    /** 菜名快照 */
    private String dishName;

    /** 价格快照 */
    private BigDecimal price;

    private Integer quantity;
}

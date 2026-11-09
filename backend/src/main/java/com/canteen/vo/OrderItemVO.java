package com.canteen.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 订单明细则 VO：菜名/价格均为下单时快照
 */
@Data
public class OrderItemVO {

    private Long id;

    private Long dishId;

    private String dishName;

    private BigDecimal price;

    private Integer quantity;

    /** 小计 = price * quantity */
    private BigDecimal subtotal;
}

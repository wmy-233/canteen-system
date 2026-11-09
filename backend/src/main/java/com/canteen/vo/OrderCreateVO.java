package com.canteen.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 下单结果 VO：成功页展示取餐码 + 订单号 + 金额
 */
@Data
public class OrderCreateVO {

    private Long orderId;

    private String orderNo;

    /** 取餐码（自取单；配送单为 null） */
    private String pickupCode;

    private BigDecimal totalAmount;

    private BigDecimal discountAmount;

    private BigDecimal payAmount;

    /** 0自取 1配送 */
    private Integer deliveryType;
}

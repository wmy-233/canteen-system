package com.canteen.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 优惠券实体
 */
@Data
public class Coupon {

    private Long id;

    private String name;

    /** 满减门槛 */
    private BigDecimal threshold;

    /** 减免金额 */
    private BigDecimal amount;

    /** 发放总量（减的是库存） */
    private Integer stock;

    /** 过期时间：过期后自动停发、不可使用 */
    private LocalDateTime expireTime;

    /** 0停发 1发放中 */
    private Integer status;
}

package com.canteen.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 优惠券 VO：领券页/我的券页通用（含属人使用状态）
 */
@Data
public class CouponVO {

    private Long id;

    private String name;

    private BigDecimal threshold;

    private BigDecimal amount;

    private Integer stock;

    private LocalDateTime expireTime;

    /** 0停发 1发放中 */
    private Integer status;

    /** 我的券页使用：0未使用 1已使用（领券页接口该字段为 null） */
    private Integer used;
}

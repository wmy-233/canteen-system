package com.canteen.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户领券记录：属人限量（一个用户同券只领一次）与使用状态
 */
@Data
public class UserCoupon {

    private Long id;

    private Long userId;

    private Long couponId;

    /** 0未使用 1已使用 */
    private Integer used;

    private LocalDateTime createTime;
}

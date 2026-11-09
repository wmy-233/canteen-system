package com.canteen.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单主表实体（含取餐码、状态机与配送三列）
 */
@Data
public class OrderInfo {

    private Long id;

    /** 订单号 CT+时间戳+随机 */
    private String orderNo;

    /** 下单学生 */
    private Long userId;

    /** 档口 */
    private Long stallId;

    private BigDecimal totalAmount;

    private BigDecimal discountAmount;

    private BigDecimal payAmount;

    /** 取餐码（自取单） */
    private String pickupCode;

    /** 0待取餐/待处理 1已完成 2已取消 */
    private Integer status;

    /** 0自取 1配送 */
    private Integer deliveryType;

    /** 配送骑手（自取单为 NULL） */
    private Long riderId;

    /** 0未分配 1配送中 2已送达（自取单为 NULL） */
    private Integer deliveryStatus;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}

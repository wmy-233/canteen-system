package com.canteen.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单列表/详情 VO（学生端与商家端通用，商家侧多档口字段已经合入）
 */
@Data
public class OrderVO {

    private Long id;

    private String orderNo;

    /** 下单学生昵称（商家端展示） */
    private String nickname;

    private Long userId;

    private Long stallId;

    /** 档口名称 */
    private String stallName;

    private BigDecimal totalAmount;

    private BigDecimal discountAmount;

    private BigDecimal payAmount;

    private String pickupCode;

    /** 0待处理 1已完成 2已取消 */
    private Integer status;

    /** 0自取 1配送 */
    private Integer deliveryType;

    private Long riderId;

    /** 骑手姓名（配送单展示） */
    private String riderName;

    /** 0未分配 1配送中 2已送达（自取单 null） */
    private Integer deliveryStatus;

    private LocalDateTime createTime;

    private List<OrderItemVO> items;
}

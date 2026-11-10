package com.canteen.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 投诉工单：0待处理 →(商家回应)→ 1已回应 →(管理员裁定)→ 2已裁定
 */
@Data
public class Complaint {

    private Long id;
    private Long orderId;
    private Long studentUserId;
    private Long stallId;
    /** 1菜品质量 2漏发缺件 3配送问题 4服务态度 */
    private Integer type;
    private String content;
    private Integer status;
    private String reply;
    private LocalDateTime replyTime;
    private String verdict;
    /** 1成立 0不成立 */
    private Integer result;
    private BigDecimal refundAmount;
    private Long adminId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

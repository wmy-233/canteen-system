package com.canteen.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 管理端全局统计（数据大屏卡片 + 档口排行）
 */
@Data
public class StallStatVO {

    private Long stallId;

    private String stallName;

    /** 累计 GMV（不含已取消） */
    private BigDecimal gmv;

    private Integer orderCount;
}

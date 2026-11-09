package com.canteen.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 销量趋势点（商家端 ECharts：日/周/月销量折线）
 */
@Data
public class DailyStatVO {

    /** 统计日 yyyy-MM-dd */
    private String statDate;

    /** 营收 = SUM(明细 price*quantity) */
    private BigDecimal turnover;

    /** 订单量（去重订单数） */
    private Integer orderCount;
}

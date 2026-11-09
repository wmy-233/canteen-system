package com.canteen.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 管理端全局统计总览 VO
 */
@Data
public class OverviewVO {

    /** 累计 GMV（不含已取消订单） */
    private BigDecimal totalGmv;

    /** 累计订单量（不含已取消） */
    private Integer totalOrders;

    /** 用户总数 */
    private Integer totalUsers;

    /** 营业中档口数 */
    private Integer openStalls;

    /** 今日 GMV */
    private BigDecimal todayGmv;

    /** 今日订单 */
    private Integer todayOrders;

    /** 今日菜品榜（全局口径，复用热销榜降级查询） */
    private List<SaleRankVO> topDishes;
}

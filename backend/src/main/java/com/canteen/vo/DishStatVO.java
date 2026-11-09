package com.canteen.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 菜品销量排行（商家端排行科室/管理端全局）
 */
@Data
public class DishStatVO {

    private Long dishId;

    private String dishName;

    /** 卖出份数 = SUM(quantity) */
    private Integer sold;

    /** 金额 = SUM(price*quantity) */
    private BigDecimal turnover;
}

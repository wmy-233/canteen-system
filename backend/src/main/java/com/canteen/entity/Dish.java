package com.canteen.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 菜品实体：daily_stock 是防超卖核心字段（0 表示不限量）
 */
@Data
public class Dish {

    private Long id;

    /** 所属档口 */
    private Long stallId;

    /** 分类 */
    private Long categoryId;

    private String name;

    private String description;

    private BigDecimal price;

    /** 图片URL */
    private String image;

    /** 每日限量数（0 不限量） */
    private Integer dailyStock;

    /** 累计销量 */
    private Integer saleCount;

    /** 0下架 1上架 */
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}

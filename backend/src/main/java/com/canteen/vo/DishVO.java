package com.canteen.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 学生端菜品 VO：叠加评分聚合展示（FR-STU-01/09）
 */
@Data
public class DishVO {

    private Long id;

    /** 所属档口 */
    private Long stallId;

    /** 档口名称 */
    private String stallName;

    /** 食堂ID（前端按食堂→档口→分类导航） */
    private Long canteenId;

    private Long categoryId;

    private String name;

    private String description;

    private BigDecimal price;

    private String image;

    /** 每日限量数（0 不限量） */
    private Integer dailyStock;

    private Integer saleCount;

    /** 0下架 1上架 */
    private Integer status;

    /** 评分 1-5（评价聚合，无评价为 null） */
    private Double avgScore;
}

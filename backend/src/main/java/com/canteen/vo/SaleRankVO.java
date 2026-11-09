package com.canteen.vo;

import lombok.Data;

/**
 * 热销榜 VO（Redis ZSet，Redis 不可用时降级为数据库 sale_count 排序）
 */
@Data
public class SaleRankVO {

    private Long dishId;

    private String name;

    /** 数据库累计销量（展示口径，O-3 决议：按累计销量） */
    private Integer saleCount;
}

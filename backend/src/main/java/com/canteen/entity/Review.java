package com.canteen.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 评价实体：仅本人已完成订单，同一订单同一菜品只能评一次
 */
@Data
public class Review {

    private Long id;

    private Long orderId;

    private Long userId;

    /** 被评价的菜品 */
    private Long dishId;

    /** 评分 1-5 */
    private Integer score;

    private String content;

    private LocalDateTime createTime;
}

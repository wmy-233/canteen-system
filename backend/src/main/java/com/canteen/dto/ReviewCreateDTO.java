package com.canteen.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 评价 DTO（FR-STU-09：仅本人已完成订单）
 */
@Data
public class ReviewCreateDTO {

    @NotNull(message = "订单不能为空")
    private Long orderId;

    @NotNull(message = "菜品不能为空")
    private Long dishId;

    @NotNull(message = "评分不能为空")
    @Min(value = 1, message = "评分范围 1-5")
    @Max(value = 5, message = "评分范围 1-5")
    private Integer score;

    @Size(max = 255, message = "评价内容过长")
    private String content;
}

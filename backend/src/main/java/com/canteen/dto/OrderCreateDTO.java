package com.canteen.dto;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 下单 DTO：未传 items 时可从购物车结算（前端传明确明细，契约更稳）
 */
@Data
public class OrderCreateDTO {

    /** 0自取 1配送（骑手单） */
    @NotNull(message = "就餐方式不能为空")
    @Min(value = 0, message = "就餐方式非法")
    @Max(value = 1, message = "就餐方式非法")
    private Integer deliveryType;

    /** 用户领券记录ID（可不传，不传时按门槛自动选择可用券中最高面额） */
    private Long couponId;

    @NotEmpty(message = "下单明细不能为空")
    @Valid
    private List<Item> items;

    @Data
    public static class Item {

        @NotNull(message = "菜品不能为空")
        private Long dishId;

        @NotNull(message = "数量不能为空")
        @Min(value = 1, message = "数量至少 1")
        @Max(value = 99, message = "单品数量过大")
        private Integer quantity;
    }
}

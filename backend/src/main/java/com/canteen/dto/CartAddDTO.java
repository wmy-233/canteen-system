package com.canteen.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * 购物车加购 DTO（同菜品重复加购走数量累加）
 */
@Data
public class CartAddDTO {

    @NotNull(message = "菜品不能为空")
    private Long dishId;

    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "数量至少 1")
    @Max(value = 99, message = "单次加购数量过多")
    private Integer quantity;
}

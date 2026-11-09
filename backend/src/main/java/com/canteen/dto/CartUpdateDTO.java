package com.canteen.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * 购物车改量 DTO
 */
@Data
public class CartUpdateDTO {

    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "数量至少 1")
    @Max(value = 99, message = "数量过大")
    private Integer quantity;
}

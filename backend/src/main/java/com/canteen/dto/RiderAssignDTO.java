package com.canteen.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 配送单指派 DTO（state 0未分配 -> 1配送中）
 */
@Data
public class RiderAssignDTO {

    @NotNull(message = "订单不能为空")
    private Long orderId;

    @NotNull(message = "骑手不能为空")
    private Long riderId;
}

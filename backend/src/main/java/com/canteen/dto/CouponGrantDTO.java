package com.canteen.dto;

import lombok.Data;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 管理端优惠券投放 DTO（FR-ADM-03）
 */
@Data
public class CouponGrantDTO {

    @NotNull(message = "券名不能为空")
    private String name;

    @NotNull(message = "门槛不能为空")
    @DecimalMin(value = "0.00", message = "门槛不合法")
    private BigDecimal threshold;

    @NotNull(message = "减免金额不能为空")
    @DecimalMin(value = "0.01", message = "减免金额必须大于 0")
    private BigDecimal amount;

    @NotNull(message = "发放总量不能为空")
    @Min(value = 1, message = "发放总量至少 1")
    @Max(value = 100000, message = "发放总量过大")
    private Integer stock;

    @NotNull(message = "过期时间不能为空")
    private LocalDateTime expireTime;
}

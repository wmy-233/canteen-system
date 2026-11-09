package com.canteen.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * 管理员用户状态 DTO：0禁用 1启用
 */
@Data
public class UserStatusDTO {

    @NotNull(message = "状态不能为空")
    @Min(value = 0, message = "状态非法")
    @Max(value = 1, message = "状态非法")
    private Integer status;
}

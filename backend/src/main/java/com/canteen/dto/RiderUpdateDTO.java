package com.canteen.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 骑手修改 DTO（姓名/手机号/停用启用）
 */
@Data
public class RiderUpdateDTO {

    @Size(max = 30, message = "姓名过长")
    private String name;

    @Size(max = 20, message = "手机号过长")
    private String phone;

    @NotNull(message = "状态不能为空")
    @Min(value = 0, message = "状态非法")
    @Max(value = 1, message = "状态非法")
    private Integer status;
}

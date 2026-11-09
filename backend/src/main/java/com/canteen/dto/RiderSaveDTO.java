package com.canteen.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 骑手新增 DTO（管理员创建登录账号并绑定 RIDER 角色）
 */
@Data
public class RiderSaveDTO {

    @NotNull(message = "账号不能为空")
    @Size(min = 2, max = 50, message = "账号长度 2-50")
    private String username;

    @NotNull(message = "初始密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度 6-20")
    private String password;

    @NotNull(message = "姓名不能为空")
    @Size(max = 30, message = "姓名过长")
    private String name;

    @Size(max = 20, message = "手机号过长")
    private String phone;
}

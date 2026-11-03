package com.canteen.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * 注册请求体（默认注册为 STUDENT 角色，管理员/商家账号由管理员录入或改角色）
 */
@Data
@Schema(description = "注册请求")
public class RegisterRequest {

    @Schema(description = "登录账号", example = "student1")
    @NotBlank(message = "账号不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9_]{3,20}$", message = "账号为3-20位字母数字下划线")
    private String username;

    @Schema(description = "密码", example = "123456")
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度6-20位")
    private String password;

    @Schema(description = "昵称（可不填，默认同账号）", example = "张同学")
    @Size(max = 50, message = "昵称最长50字符")
    private String nickname;
}

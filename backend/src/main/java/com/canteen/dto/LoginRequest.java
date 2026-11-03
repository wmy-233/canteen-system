package com.canteen.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 登录请求体
 */
@Data
@Schema(description = "登录请求")
public class LoginRequest {

    @Schema(description = "登录账号", example = "student1")
    @NotBlank(message = "账号不能为空")
    private String username;

    @Schema(description = "密码", example = "123456")
    @NotBlank(message = "密码不能为空")
    private String password;
}

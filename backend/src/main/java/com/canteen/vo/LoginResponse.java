package com.canteen.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 登录/注册成功返回体
 */
@Data
@Schema(description = "登录返回")
public class LoginResponse {

    @Schema(description = "JWT token（前端存起来，请求头 Authorization: Bearer xxx）")
    private String token;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "账号")
    private String username;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "角色编码：ADMIN / MERCHANT / STUDENT")
    private String role;
}

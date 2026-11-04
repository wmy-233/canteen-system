package com.canteen.security;

import lombok.Value;

import java.util.List;

/**
 * 当前登录用户信息（JwtAuthenticationFilter 解析 token 后放入 SecurityContext）
 * Controller 中可通过 SecurityContextHolder 取用，也可经 /api/user/me 返回给前端
 */
@Value
public class LoginUser {

    /** 用户ID */
    Long userId;

    /** 登录账号 */
    String username;

    /** 角色编码列表，如 ADMIN / MERCHANT / STUDENT */
    List<String> roles;
}

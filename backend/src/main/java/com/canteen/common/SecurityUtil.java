package com.canteen.common;

import com.canteen.security.LoginUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 获取当前登录用户的工具类（从 SecurityContext 解析，JWT 过滤器已写入 LoginUser）
 */
public class SecurityUtil {

    /** 当前登录用户；未通过 JWT 过滤器抛 IllegalStateException */
    public static LoginUser getLoginUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof LoginUser) {
            return (LoginUser) authentication.getPrincipal();
        }
        throw new IllegalStateException("未获取到登录用户");
    }

    /** 当前用户ID */
    public static Long getUserId() {
        return getLoginUser().getUserId();
    }
}

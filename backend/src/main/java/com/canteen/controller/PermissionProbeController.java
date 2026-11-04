package com.canteen.controller;

import com.canteen.common.Result;
import com.canteen.security.LoginUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 权限自检接口（联调用，组员 C 维护）
 *
 * 用途：验证 RBAC 鉴权生效（D7 验收"401/403 证据"）+ 前端导航栏/个人中心取当前用户
 *   GET /api/user/me      登录即可：返回当前登录用户（任何角色）
 *   GET /api/admin/ping   仅 ADMIN：其他角色访问返回 403
 *   GET /api/merchant/ping 仅 MERCHANT：其他角色访问返回 403
 *
 * 说明：后续业务接口（用户管理/档口管理等）就绪后，本控制器可由后端负责人调整或移除
 */
@Tag(name = "权限自检")
@RestController
@RequestMapping("/api")
public class PermissionProbeController {

    @Operation(summary = "当前登录用户（登录即可访问，前端个人中心用）")
    @GetMapping("/user/me")
    public Result<LoginUser> me() {
        return Result.success(currentUser());
    }

    @Operation(summary = "ADMIN 专用探针：学生/商家访问应返回 403")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/ping")
    public Result<String> adminPing() {
        return Result.success("hello, " + currentUser().getUsername() + "（ADMIN）");
    }

    @Operation(summary = "MERCHANT 专用探针：学生/管理员访问应返回 403")
    @PreAuthorize("hasRole('MERCHANT')")
    @GetMapping("/merchant/ping")
    public Result<String> merchantPing() {
        return Result.success("hello, " + currentUser().getUsername() + "（MERCHANT）");
    }

    /** 从 SecurityContext 取当前登录用户（未通过 JWT 过滤器时 principal 不是 LoginUser） */
    private LoginUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof LoginUser) {
            return (LoginUser) authentication.getPrincipal();
        }
        throw new IllegalStateException("未获取到登录用户");
    }
}

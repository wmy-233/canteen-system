package com.canteen.security;

import com.canteen.mapper.RoleMapper;
import com.canteen.util.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * JWT 认证过滤器（组员 C 维护）
 *
 * 职责划分：
 *   1. 无 token         -> 不设置认证信息，交给 SecurityConfig 的路径规则判定（放行或 401）
 *   2. token 有效       -> 从数据库加载最新角色后写入 SecurityContext
 *                          （角色以数据库为准：管理员禁用账号/调整角色后，旧 token 立即失效）
 *   3. token 无效/过期  -> 不在此处写响应，交给 SecurityConfig 的认证入口点统一返回 401
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final RoleMapper roleMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (!StringUtils.hasText(header) || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            Claims claims = jwtUtil.parseToken(header.substring(7));
            Long userId = claims.get("userId", Long.class);
            String username = claims.getSubject();

            if (userId != null) {
                // 角色以数据库为准，token 中的 role 仅作兜底
                List<String> roles = roleMapper.selectRoleCodesByUserId(userId);
                if (roles.isEmpty()) {
                    String tokenRole = claims.get("role", String.class);
                    roles = tokenRole != null ? List.of(tokenRole) : Collections.emptyList();
                }

                List<SimpleGrantedAuthority> authorities = roles.stream()
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                        .collect(Collectors.toList());

                LoginUser loginUser = new LoginUser(userId, username, roles);
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(loginUser, null, authorities);
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } else {
                log.warn("token 缺少 userId 声明，按未认证处理: {}", request.getRequestURI());
            }
        } catch (JwtException | IllegalArgumentException e) {
            // 过期/篡改/格式错误：不在此处写响应，由认证入口点统一返回 401
            log.warn("token 校验失败: {} - {}", request.getRequestURI(), e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // 登录/注册/健康检查无需解析 token，直接跳过减少无谓开销
        String path = request.getRequestURI();
        return path.startsWith("/api/user/login")
                || path.startsWith("/api/user/register")
                || path.startsWith("/api/health");
    }
}

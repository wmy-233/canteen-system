package com.canteen.config;

import com.canteen.common.Result;
import com.canteen.security.JwtAuthenticationFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import javax.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;

/**
 * Spring Security 安全配置（RBAC，组员 C 维护）
 *
 * 鉴权规则：
 *   放行：/api/user/login、/api/user/register、/api/health、knife4j 接口文档静态资源
 *   /api/admin/**    -> ROLE_ADMIN
 *   /api/merchant/** -> ROLE_MERCHANT
 *   其余路径         -> 登录即可（学生端接口、/api/user/** 等）
 *
 * 统一响应（与前端 request.js 拦截器对齐——前端按 HTTP 状态码处理）：
 *   未认证   -> HTTP 401 + Result JSON（前端自动清 token 跳登录页）
 *   越权访问 -> HTTP 403 + Result JSON
 */
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)  // 开启 @PreAuthorize 方法级鉴权
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf().disable()  // 前后端分离 + JWT 无 Cookie，无需 CSRF
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .authorizeRequests()
                // 登录注册与健康检查放行（含商家入驻注册）；食堂列表是入驻表单的公开只读基础数据
                .antMatchers("/api/user/login", "/api/user/register", "/api/user/register-merchant",
                        "/api/canteen/list", "/api/health").permitAll()
                // knife4j / OpenAPI 接口文档放行
                .antMatchers("/doc.html", "/webjars/**", "/v3/api-docs/**",
                        "/swagger-ui/**", "/swagger-resources/**", "/favicon.ico").permitAll()
                // 管理端仅 ADMIN，商家端仅 MERCHANT
                .antMatchers("/api/admin/**").hasRole("ADMIN")
                .antMatchers("/api/merchant/**").hasRole("MERCHANT")
                // 其余接口登录即可
                .anyRequest().authenticated()
            .and()
            .exceptionHandling()
                // 未认证：HTTP 401（前端拦截器据此清 token 跳登录页）
                .authenticationEntryPoint((request, response, ex) ->
                        writeJson(response, HttpServletResponse.SC_UNAUTHORIZED, 401, "未登录或登录已过期"))
                // 已认证但无权限：HTTP 403
                .accessDeniedHandler((request, response, ex) ->
                        writeJson(response, HttpServletResponse.SC_FORBIDDEN, 403, "无权限访问该资源"))
            .and()
            .formLogin().disable()
            .httpBasic().disable()
            // JWT 过滤器插在用户名密码过滤器之前
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /** 统一写出 401/403 JSON，结构与业务返回体 Result 一致 */
    private void writeJson(HttpServletResponse response, int httpStatus, int code, String message) {
        try {
            response.setStatus(httpStatus);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getWriter().write(objectMapper.writeValueAsString(Result.fail(code, message)));
        } catch (Exception ignored) {
            // 响应已提交等极端情况，无需处理
        }
    }

    /**
     * 密码加密器（用户表密码存 BCrypt 密文，禁止明文）
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

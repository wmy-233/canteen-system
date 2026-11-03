package com.canteen.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security 安全配置
 *
 * TODO(第1周 D5)：加入 JWT 过滤器、按 RBAC 规则细化鉴权路径
 * 目标规则示例：
 *   /api/admin/**    -> ADMIN 角色
 *   /api/merchant/** -> MERCHANT 角色
 *   /api/user/**     -> 登录即可
 *   /api/login, /api/register, /api/health -> 放行
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf().disable()  // 前后端分离，使用 JWT，无需 CSRF
            .authorizeRequests()
                // 初始骨架阶段：全部放行，方便先跑通；第1周 D5 由组员 C 收紧为 JWT+RBAC 规则
                .anyRequest().permitAll()
            .and()
            .formLogin().disable()
            .httpBasic().disable();
        return http.build();
    }

    /**
     * 密码加密器（用户表密码存 BCrypt 密文，禁止明文）
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

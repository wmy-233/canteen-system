package com.canteen.service.impl;

import com.canteen.dto.LoginRequest;
import com.canteen.dto.RegisterRequest;
import com.canteen.entity.User;
import com.canteen.mapper.RoleMapper;
import com.canteen.mapper.UserMapper;
import com.canteen.service.UserService;
import com.canteen.util.JwtUtil;
import com.canteen.vo.LoginResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * 用户业务实现：注册（BCrypt 加密）、登录校验、JWT 签发
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    /** 学生角色编码（即默认注册角色） */
    private static final String DEFAULT_ROLE = "STUDENT";

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginResponse register(RegisterRequest request) {
        // 1. 账号占用检查
        if (userMapper.selectByUsername(request.getUsername()) != null) {
            throw new IllegalArgumentException("账号已被注册");
        }

        // 2. 创建用户：密码 BCrypt 加密入库
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname() != null ? request.getNickname() : request.getUsername());
        user.setBalance(new BigDecimal("0.00"));   // 金额一律 BigDecimal
        user.setStatus(1);
        userMapper.insert(user);

        // 3. 绑定默认角色（RBAC：用户-角色关联表）
        Long roleId = roleMapper.selectIdByCode(DEFAULT_ROLE);
        roleMapper.insertUserRole(user.getId(), roleId);
        log.info("新用户注册: username={}, role={}", user.getUsername(), DEFAULT_ROLE);

        // 4. 直接返回登录态
        return buildLoginResponse(user, DEFAULT_ROLE);
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        User user = userMapper.selectByUsername(request.getUsername());
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("账号或密码错误");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new IllegalArgumentException("账号已被禁用，请联系管理员");
        }

        List<String> roles = roleMapper.selectRoleCodesByUserId(user.getId());
        String role = roles.isEmpty() ? DEFAULT_ROLE : roles.get(0);
        log.info("用户登录成功: username={}, role={}", user.getUsername(), role);
        return buildLoginResponse(user, role);
    }

    /**
     * 组装登录返回：生成 JWT，携带 userId / username / role
     */
    private LoginResponse buildLoginResponse(User user, String role) {
        LoginResponse resp = new LoginResponse();
        resp.setToken(jwtUtil.createToken(user.getId(), user.getUsername(), role));
        resp.setUserId(user.getId());
        resp.setUsername(user.getUsername());
        resp.setNickname(user.getNickname());
        resp.setRole(role);
        return resp;
    }
}

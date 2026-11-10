package com.canteen.service.impl;

import com.canteen.dto.LoginRequest;
import com.canteen.dto.RegisterMerchantRequest;
import com.canteen.dto.RegisterRequest;
import com.canteen.entity.Canteen;
import com.canteen.entity.Stall;
import com.canteen.entity.User;
import com.canteen.common.BizException;
import com.canteen.mapper.CanteenMapper;
import com.canteen.mapper.RoleMapper;
import com.canteen.mapper.StallMapper;
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

    /** 商家角色编码 */
    private static final String MERCHANT_ROLE = "MERCHANT";

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final StallMapper stallMapper;
    private final CanteenMapper canteenMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    /**
     * 商家入驻注册（事务）：
     * 1) 账号占用检查 → 2) 建 MERCHANT 账号 → 3) 创建待审核档口(status=2) → 4) 返回登录态
     * 审核通过前：档口不能营业、供应商不可上架营业（StallServiceImpl 已有状态防线）；
     * 管理员在「档口审核」通过后 start 营业。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginResponse registerMerchant(RegisterMerchantRequest request) {
        // 1. 账号占用检查
        if (userMapper.selectByUsername(request.getUsername()) != null) {
            throw new IllegalArgumentException("账号已被注册");
        }
        // 2. 食堂真实存在（防脏数据 / 伪造 canteenId）
        boolean canteenOk = canteenMapper.selectAll().stream()
                .anyMatch(c -> c.getId().equals(request.getCanteenId()));
        if (!canteenOk) {
            throw new BizException("所选食堂不存在，请重新选择");
        }
        // 3. 同食堂档口名不重复（避免审核员看到重名档口）
        boolean nameDup = stallMapper.selectAll(null).stream()
                .anyMatch(s -> request.getCanteenId().equals(s.getCanteenId())
                        && s.getName() != null && s.getName().equals(request.getStallName().trim()));
        if (nameDup) {
            throw new BizException("该食堂已有同名档口，请换一个名字");
        }

        // 4. 创建商家账号：BCrypt 加密 + 联系电话
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname() != null ? request.getNickname() : request.getUsername());
        user.setPhone(request.getPhone());
        user.setBalance(new BigDecimal("0.00"));
        user.setStatus(1);
        userMapper.insert(user);

        // 5. 绑定 MERCHANT 角色
        Long roleId = roleMapper.selectIdByCode(MERCHANT_ROLE);
        roleMapper.insertUserRole(user.getId(), roleId);

        // 6. 创建档口申请：状态=2 待审核
        Stall stall = new Stall();
        stall.setCanteenId(request.getCanteenId());
        stall.setMerchantUserId(user.getId());
        stall.setName(request.getStallName().trim());
        stall.setStatus(2);
        stallMapper.insert(stall);
        log.info("商家入驻申请: username={}, stallName={}, canteenId={}, 请管理员审核",
                user.getUsername(), stall.getName(), stall.getCanteenId());

        // 7. 直接返回登录态（可进商家端完善菜品，出餐审核通过后即可营业）
        return buildLoginResponse(user, MERCHANT_ROLE);
    }

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

package com.canteen.service.impl;

import com.canteen.common.BizException;
import com.canteen.common.PageResult;
import com.canteen.dto.UserStatusDTO;
import com.canteen.entity.User;
import com.canteen.mapper.RoleMapper;
import com.canteen.mapper.UserMapper;
import com.canteen.service.AdminUserService;
import com.canteen.vo.UserAdminVO;
import com.github.pagehelper.PageHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 管理端用户管理实现（FR-ADM-01）
 * 注意：角色实时查库的下线链路已在 JWT 过滤器实现——禁用生效不影响核验
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    /** 管理员可分配的角色范围（管理员本人不可被自助改，防误操作） */
    private static final List<String> ASSIGNABLE_ROLES = List.of("STUDENT", "MERCHANT", "RIDER");

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;

    @Override
    public PageResult<UserAdminVO> page(String keyword, Integer pageNum, Integer pageSize) {
        pageNum = pageNum == null ? 1 : pageNum;
        pageSize = pageSize == null ? 10 : pageSize;
        PageHelper.startPage(pageNum, pageSize);
        List<UserAdminVO> list = userMapper.selectPage(keyword);
        fillRoles(list);
        return PageResult.build(list, pageNum, pageSize);
    }

    @Override
    public void updateStatus(Long userId, UserStatusDTO dto) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException("用户不存在");
        }
        if (user.getStatus() != null && user.getStatus().equals(dto.getStatus())) {
            return;
        }
        userMapper.updateStatus(userId, dto.getStatus());
        log.info("用户{}: userId={}, operator={}",
                dto.getStatus() == 0 ? "禁用" : "启用", userId, com.canteen.common.SecurityUtil.getUserId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetRole(Long userId, String roleCode) {
        if (!ASSIGNABLE_ROLES.contains(roleCode)) {
            throw new BizException("不允许设置该角色");
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException("用户不存在");
        }
        roleMapper.deleteUserRoles(userId);
        Long roleId = roleMapper.selectIdByCode(roleCode);
        roleMapper.insertUserRole(userId, roleId);
        log.info("重置用户角色: userId={}, role={}, operator={}"
                , userId, roleCode, com.canteen.common.SecurityUtil.getUserId());
    }

    /** 用一次 JOIN 查询把角色码填进列表（一人多角色取第一个） */
    private void fillRoles(List<UserAdminVO> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        List<Long> ids = list.stream().map(UserAdminVO::getId).collect(Collectors.toList());
        Map<Long, String> roleMap = new HashMap<>();
        for (Map<String, Object> row : roleMapper.selectRoleCodesByUserIds(ids)) {
            Long userId = ((Number) row.get("userId")).longValue();
            if (!roleMap.containsKey(userId)) {
                roleMap.put(userId, (String) row.get("roleCode"));
            }
        }
        for (UserAdminVO vo : list) {
            vo.setRole(roleMap.get(vo.getId()));
        }
    }
}

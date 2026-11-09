package com.canteen.service;

import com.canteen.common.PageResult;
import com.canteen.dto.UserStatusDTO;
import com.canteen.vo.UserAdminVO;

/**
 * 管理端用户管理业务（FR-ADM-01：列表/搜索/禁用/启用/重置角色）
 */
public interface AdminUserService {

    PageResult<UserAdminVO> page(String keyword, Integer pageNum, Integer pageSize);

    void updateStatus(Long userId, UserStatusDTO dto);

    void resetRole(Long userId, String roleCode);
}

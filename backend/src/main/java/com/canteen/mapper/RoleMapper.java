package com.canteen.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 角色表 Mapper（RBAC）
 */
@Mapper
public interface RoleMapper {

    /** 查询某用户的角色编码列表（如 ADMIN/MERCHANT/STUDENT） */
    List<String> selectRoleCodesByUserId(@Param("userId") Long userId);

    /** 按角色编码查角色ID */
    Long selectIdByCode(@Param("roleCode") String roleCode);

    /** 给用户绑定角色 */
    int insertUserRole(@Param("userId") Long userId, @Param("roleId") Long roleId);

    /** 管理端重置角色：先清原有绑定 */
    int deleteUserRoles(@Param("userId") Long userId);

    /** 每个用户的角色码映射（用户管理列表展示用：userId -> role_code） */
    List<Map<String, Object>> selectRoleCodesByUserIds(@Param("userIds") List<Long> userIds);
}

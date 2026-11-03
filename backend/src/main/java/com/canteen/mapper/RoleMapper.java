package com.canteen.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

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
}

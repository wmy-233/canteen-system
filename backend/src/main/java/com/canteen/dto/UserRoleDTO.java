package com.canteen.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 管理员重置用户角色 DTO（FR-ADM-01）
 */
@Data
public class UserRoleDTO {

    @NotNull(message = "角色编码不能为空")
    private String roleCode;
}

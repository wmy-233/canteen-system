package com.canteen.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理员端用户列表 VO（防密码泄露：不返回 password）
 */
@Data
public class UserAdminVO {

    private Long id;

    private String username;

    private String nickname;

    private String phone;

    private java.math.BigDecimal balance;

    /** 0禁用 1正常 */
    private Integer status;

    /** 首个角色编码 STUDENT/MERCHANT/ADMIN/RIDER（演示用） */
    private String role;

    private LocalDateTime createTime;
}

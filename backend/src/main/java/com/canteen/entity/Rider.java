package com.canteen.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 骑手实体：管理员端"骑手管理"功能维护（骑手账号由管理员创建并绑定 RIDER 角色）
 */
@Data
public class Rider {

    private Long id;

    /** 绑定的登录账号（user 表，RIDER 角色） */
    private Long userId;

    private String name;

    private String phone;

    /** 0停用 1在岗 */
    private Integer status;

    private LocalDateTime createTime;
}

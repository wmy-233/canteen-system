package com.canteen.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户表实体（对应数据库表 user）
 * 注意：user 是 MySQL 关键字，SQL 中书写要用反引号 `user`
 */
@Data
public class User {

    /** 用户ID */
    private Long id;

    /** 登录账号 */
    private String username;

    /** 密码（BCrypt 加密存储，禁止明文） */
    private String password;

    /** 昵称 */
    private String nickname;

    /** 手机号 */
    private String phone;

    /** 头像URL */
    private String avatar;

    /** 钱包余额 */
    private BigDecimal balance;

    /** 状态 0禁用 1正常 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}

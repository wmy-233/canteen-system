package com.canteen.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 骑手 VO（管理端"骑手管理"页，含登录账号名）
 */
@Data
public class RiderVO {

    private Long id;

    private Long userId;

    /** 登录账号 */
    private String username;

    private String name;

    private String phone;

    /** 0停用 1在岗 */
    private Integer status;

    private LocalDateTime createTime;
}

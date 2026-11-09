package com.canteen.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 档口实体：学生表
 */
@Data
public class Stall {

    /** 档口ID */
    private Long id;

    /** 所属食堂 */
    private Long canteenId;

    /** 商家的 user 表账号 */
    private Long merchantUserId;

    /** 档口名称 */
    private String name;

    /** 状态 0停业 1营业 2待审核 */
    private Integer status;

    private LocalDateTime createTime;
}

package com.canteen.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 档口 VO：学生/管理端展示（多带食堂名与商家账号）
 */
@Data
public class StallVO {

    private Long id;

    private Long canteenId;

    /** 食堂名称 */
    private String canteenName;

    private Long merchantUserId;

    /** 商家账号名 */
    private String merchantUsername;

    private String name;

    /** 0停业 1营业 2待审核 */
    private Integer status;

    private LocalDateTime createTime;
}

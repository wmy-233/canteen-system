package com.canteen.entity;

import lombok.Data;

/**
 * 食堂表实体（对应数据库表 canteen）
 */
@Data
public class Canteen {

    /** 食堂ID */
    private Long id;

    /** 食堂名称 */
    private String name;

    /** 位置 */
    private String location;
}

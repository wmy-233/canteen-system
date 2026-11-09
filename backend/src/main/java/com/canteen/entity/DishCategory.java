package com.canteen.entity;

import lombok.Data;

/**
 * 菜品分类实体
 */
@Data
public class DishCategory {

    private Long id;

    /** 分类名称 */
    private String name;

    /** 排序值（越小越靠前） */
    private Integer sort;
}

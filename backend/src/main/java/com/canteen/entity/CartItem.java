package com.canteen.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 购物车实体：同一用户同一菜品仅一条（uk_user_dish 唯一键）
 */
@Data
public class CartItem {

    private Long id;

    private Long userId;

    private Long dishId;

    private Integer quantity;

    private LocalDateTime createTime;
}

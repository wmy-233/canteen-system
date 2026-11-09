package com.canteen.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 购物车 VO（条目多带菜品信息：学生端加购列表直接渲染）
 */
@Data
public class CartVO {

    private Long id;

    private Long dishId;

    private String dishName;

    private String image;

    private BigDecimal price;

    private Integer quantity;

    /** 档口ID与名称（同一档口内加购，跨档口下单时前端据此提示拆单） */
    private Long stallId;

    private String stallName;
}

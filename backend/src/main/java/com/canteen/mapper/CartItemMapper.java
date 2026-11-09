package com.canteen.mapper;

import com.canteen.entity.CartItem;
import com.canteen.vo.CartVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 购物车 Mapper
 */
@Mapper
public interface CartItemMapper {

    /** 带菜品/档口信息的购物车列表 */
    List<CartVO> selectCartList(@Param("userId") Long userId);

    Long selectQuantityByUserAndDish(@Param("userId") Long userId, @Param("dishId") Long dishId);

    int insert(CartItem cartItem);

    int updateQuantity(@Param("userId") Long userId, @Param("dishId") Long dishId, @Param("quantity") Integer quantity);

    int deleteByIdAndUser(@Param("id") Long id, @Param("userId") Long userId);

    int deleteByUserId(@Param("userId") Long userId);

    /** 成功下单后清掉对应菜品购物车项 */
    int deleteByUserAndDishIds(@Param("userId") Long userId, @Param("dishIds") List<Long> dishIds);
}

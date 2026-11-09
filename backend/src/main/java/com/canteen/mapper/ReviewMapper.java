package com.canteen.mapper;

import com.canteen.entity.Review;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 评价 Mapper
 */
@Mapper
public interface ReviewMapper {

    /** 同订单同菜品是否已评（一人一单菜品只评一次） */
    int countByOrderAndDish(@Param("userId") Long userId, @Param("orderId") Long orderId, @Param("dishId") Long dishId);

    int insert(Review review);

    List<Review> selectByOrderId(@Param("orderId") Long orderId);

    /** 菜品评分聚合（DishVO 展示用） */
    List<Map<String, Object>> selectAvgByDishIds(@Param("dishIds") List<Long> dishIds);
}

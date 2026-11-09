package com.canteen.mapper;

import com.canteen.entity.DishCategory;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 菜品分类 Mapper
 */
@Mapper
public interface DishCategoryMapper {

    List<DishCategory> selectAll();
}

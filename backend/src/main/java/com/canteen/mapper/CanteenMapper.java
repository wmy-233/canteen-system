package com.canteen.mapper;

import com.canteen.entity.Canteen;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 食堂表 Mapper
 */
@Mapper
public interface CanteenMapper {

    List<Canteen> selectAll();
}

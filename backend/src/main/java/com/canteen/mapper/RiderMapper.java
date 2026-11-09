package com.canteen.mapper;

import com.canteen.entity.Rider;
import com.canteen.vo.RiderVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 骑手 Mapper（管理端"骑手管理"）
 */
@Mapper
public interface RiderMapper {

    int insert(Rider rider);

    Rider selectById(@Param("id") Long id);

    Rider selectByUserId(@Param("userId") Long userId);

    /** 分页（keyword 模糊匹配姓名/手机号/登录账号） */
    List<RiderVO> selectPage(@Param("keyword") String keyword);

    int updateSelective(Rider rider);
}

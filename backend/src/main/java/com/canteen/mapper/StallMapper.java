package com.canteen.mapper;

import com.canteen.entity.Stall;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 档口表 Mapper
 */
@Mapper
public interface StallMapper {

    /** 学生端：营业中档口（可按食堂过滤） */
    List<Stall> selectOpen(@Param("canteenId") Long canteenId);

    /** 管理端：全部档口（可按状态过滤，null 全部） */
    List<Stall> selectAll(@Param("status") Integer status);

    /** 商家端：本人档口 */
    List<Stall> selectByMerchantUserId(@Param("merchantUserId") Long merchantUserId);

    Stall selectById(@Param("id") Long id);

    /** 商家维护名称/营业状态（限本档口，Service 校验归属） */
    int updateSelective(Stall stall);

    /** 管理员审核改状态 */
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);
}

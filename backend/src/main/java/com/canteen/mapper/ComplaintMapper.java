package com.canteen.mapper;

import com.canteen.entity.Complaint;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 投诉工单 Mapper
 */
@Mapper
public interface ComplaintMapper {

    int insert(Complaint complaint);

    Complaint selectById(@Param("id") Long id);

    /** 学生：我的投诉（带档口名） */
    List<Map<String, Object>> selectMine(@Param("userId") Long userId);

    /** 商家：本店投诉（带学生昵称/订单号），可按状态过滤 */
    List<Map<String, Object>> selectByStall(@Param("stallIds") List<Long> stallIds, @Param("status") Integer status);

    /** 管理端：全部投诉（可按状态过滤） */
    List<Map<String, Object>> selectAll(@Param("status") Integer status);

    int updateReply(@Param("id") Long id, @Param("reply") String reply);

    int updateVerdict(@Param("id") Long id, @Param("verdict") String verdict,
                      @Param("result") Integer result, @Param("refundAmount") java.math.BigDecimal refundAmount,
                      @Param("adminId") Long adminId);
}

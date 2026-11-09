package com.canteen.mapper;

import com.canteen.entity.UserCoupon;
import com.canteen.vo.CouponVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户领券 Mapper
 */
@Mapper
public interface UserCouponMapper {

    /** 是否已领过（属人限量） */
    int countOwned(@Param("userId") Long userId, @Param("couponId") Long couponId);

    UserCoupon selectById(@Param("id") Long id);

    /** 本人未使用的某券记录ID（下单核销用） */
    Long selectUnusedRecordId(@Param("userId") Long userId, @Param("couponId") Long couponId);

    int insert(UserCoupon userCoupon);

    /** 我的券列表（带使用状态） */
    List<CouponVO> selectMine(@Param("userId") Long userId);

    /** 下单核销：影响行数=0 即券已使用/非本人 */
    int markUsed(@Param("id") Long id, @Param("userId") Long userId);

    /** 取消订单退回券（按前一次核销退返） */
    int restore(@Param("id") Long id, @Param("userId") Long userId);

    /**
     * 取消订单回滚券：按“减免金额=该单 discount_amount”匹配本人最近一张已用券
     */
    Long selectLatestUsedIdForRestore(@Param("userId") Long userId, @Param("discountAmount") java.math.BigDecimal discountAmount);
}

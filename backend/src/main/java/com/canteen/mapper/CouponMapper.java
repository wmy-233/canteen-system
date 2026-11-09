package com.canteen.mapper;

import com.canteen.entity.Coupon;
import com.canteen.vo.CouponVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

/**
 * 优惠券 Mapper
 */
@Mapper
public interface CouponMapper {

    /** 可领券：发放中+有库存+未过期 */
    List<CouponVO> selectIssuing();

    /** 管理端：全部券 */
    List<Coupon> selectAll();

    Coupon selectById(@Param("id") Long id);

    /** 发券扣库存：影响行数=0 即库存不足 */
    int decrementStock(@Param("id") Long id);

    /**
     * O-2 自动选券：本人未使用+未过期+发放中+门槛满足，按面额最高一张
     */
    CouponVO selectBestForUser(@Param("userId") Long userId, @Param("totalAmount") BigDecimal totalAmount);

    int insert(Coupon coupon);
}

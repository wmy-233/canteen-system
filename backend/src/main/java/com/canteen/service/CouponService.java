package com.canteen.service;

import com.canteen.dto.CouponGrantDTO;
import com.canteen.entity.Coupon;
import com.canteen.vo.CouponVO;

import java.util.List;

/**
 * 优惠券业务：领券页/我的券（FR-STU-07）、管理端投放（FR-ADM-03）
 */
public interface CouponService {

    /** 可领券列表 */
    List<CouponVO> issuing();

    /** 我的券（含使用状态） */
    List<CouponVO> mine();

    /** 领券（属人限量：同一券一个用户只领一次） */
    void claim(Long couponId);

    /** 管理端：投放新券 */
    void grant(CouponGrantDTO dto);

    /** 管理端：全部券 */
    List<Coupon> all();
}

package com.canteen.service.impl;

import com.canteen.common.BizException;
import com.canteen.common.SecurityUtil;
import com.canteen.dto.CouponGrantDTO;
import com.canteen.entity.Coupon;
import com.canteen.entity.UserCoupon;
import com.canteen.mapper.CouponMapper;
import com.canteen.mapper.UserCouponMapper;
import com.canteen.service.CouponService;
import com.canteen.vo.CouponVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 优惠券实现（FR-STU-07 领券 / FR-ADM-03 投放；O-2：不叠加，下单匹配取满足门槛最高一张）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CouponServiceImpl implements CouponService {

    private final CouponMapper couponMapper;
    private final UserCouponMapper userCouponMapper;

    @Override
    public List<CouponVO> issuing() {
        return couponMapper.selectIssuing();
    }

    @Override
    public List<CouponVO> mine() {
        return userCouponMapper.selectMine(SecurityUtil.getUserId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void claim(Long couponId) {
        Long userId = SecurityUtil.getUserId();
        Coupon coupon = couponMapper.selectById(couponId);
        if (coupon == null || coupon.getStatus() != 1) {
            throw new BizException("券不存在或已停发");
        }
        if (coupon.getExpireTime() != null && coupon.getExpireTime().isBefore(java.time.LocalDateTime.now())) {
            throw new BizException("券已过期");
        }
        if (userCouponMapper.countOwned(userId, couponId) > 0) {
            throw new BizException("该券已领取过");
        }
        // 扣库存：影响行数=0 即抢空（简单计数型乐观）
        int rows = couponMapper.decrementStock(couponId);
        if (rows == 0) {
            throw new BizException("券已被抢完");
        }
        UserCoupon userCoupon = new UserCoupon();
        userCoupon.setUserId(userId);
        userCoupon.setCouponId(couponId);
        userCouponMapper.insert(userCoupon);
        log.info("领券: userId={}, couponId={}", userId, couponId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void grant(CouponGrantDTO dto) {
        Coupon coupon = new Coupon();
        coupon.setName(dto.getName());
        coupon.setThreshold(dto.getThreshold());
        coupon.setAmount(dto.getAmount());
        // 防呆：减免金额大于门槛没有意义
        if (coupon.getAmount().compareTo(coupon.getThreshold()) > 0) {
            throw new BizException("减免金额不能大于满减门槛");
        }
        coupon.setStock(dto.getStock());
        coupon.setExpireTime(dto.getExpireTime());
        if (coupon.getExpireTime().isBefore(java.time.LocalDateTime.now())) {
            throw new BizException("过期时间必须晚于当前时间");
        }
        couponMapper.insert(coupon);
        log.info("投放新券: id={}, name={}, stock={}", coupon.getId(), coupon.getName(), coupon.getStock());
    }

    @Override
    public List<Coupon> all() {
        return couponMapper.selectAll();
    }
}

package com.canteen.controller;

import com.canteen.aspect.OpLog;
import com.canteen.common.Result;
import com.canteen.dto.CouponGrantDTO;
import com.canteen.entity.Coupon;
import com.canteen.service.CouponService;
import com.canteen.vo.CouponVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 优惠券：学生领券（FR-STU-07）与管理端投放（FR-ADM-03）
 */
@Tag(name = "优惠券")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    @Operation(summary = "可领取的券（发放中+有库存+未过期）")
    @GetMapping("/coupon/issuing")
    public Result<List<CouponVO>> issuing() {
        return Result.success(couponService.issuing());
    }

    @Operation(summary = "我的券（含使用状态）")
    @GetMapping("/coupon/mine")
    public Result<List<CouponVO>> mine() {
        return Result.success(couponService.mine());
    }

    @Operation(summary = "领取（属人限量，同一券只领一次）")
    @OpLog(module = "优惠券", action = "领券")
    @PostMapping("/coupon/{couponId}/claim")
    public Result<Void> claim(@PathVariable Long couponId) {
        couponService.claim(couponId);
        return Result.success();
    }

    // ---------------- 管理端（/api/admin/** 仅 ADMIN） ----------------

    @Operation(summary = "管理端：投放新券")
    @OpLog(module = "优惠券", action = "投放")
    @PostMapping("/admin/coupon")
    public Result<Void> grant(@Validated @RequestBody CouponGrantDTO dto) {
        couponService.grant(dto);
        return Result.success();
    }

    @Operation(summary = "管理端：全部券")
    @GetMapping("/admin/coupon/list")
    public Result<List<Coupon>> all() {
        return Result.success(couponService.all());
    }
}

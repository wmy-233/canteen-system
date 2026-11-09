package com.canteen.controller;

import com.canteen.common.Result;
import com.canteen.common.SecurityUtil;
import com.canteen.service.StatsService;
import com.canteen.vo.DailyStatVO;
import com.canteen.vo.DishStatVO;
import com.canteen.vo.OverviewVO;
import com.canteen.vo.StallStatVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 统计：商家报表（FR-MER-04，ECharts 数据源）与管理端全局统计（FR-ADM-04）
 */
@Tag(name = "统计报表")
@RestController
@RequestMapping
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    // ---------------- 商家端 ----------------

    @Operation(summary = "商家端：营收/订单量按日趋势（days 默认 7，最大 90）")
    @GetMapping("/api/merchant/stats/trend")
    public Result<List<DailyStatVO>> trend(@RequestParam(required = false, defaultValue = "7") Integer days) {
        return Result.success(statsService.trendForMerchant(SecurityUtil.getUserId(), days));
    }

    @Operation(summary = "商家端：菜品销量排行")
    @GetMapping("/api/merchant/stats/top-dishes")
    public Result<List<DishStatVO>> topDishes(@RequestParam(required = false, defaultValue = "7") Integer days,
                                              @RequestParam(required = false, defaultValue = "10") Integer limit) {
        return Result.success(statsService.topDishesForMerchant(SecurityUtil.getUserId(), days, limit));
    }

    // ---------------- 管理端 ----------------

    @Operation(summary = "管理端：全局总览（GMV/订单/用户/营业档口/今日）")
    @GetMapping("/api/admin/stats/overview")
    public Result<OverviewVO> overview() {
        return Result.success(statsService.overview());
    }

    @Operation(summary = "管理端：档口营收排行")
    @GetMapping("/api/admin/stats/stall-ranking")
    public Result<List<StallStatVO>> stallRanking() {
        return Result.success(statsService.stallRanking());
    }

    @Operation(summary = "管理端：全局每日趋势")
    @GetMapping("/api/admin/stats/trend")
    public Result<List<DailyStatVO>> trendAll(@RequestParam(required = false, defaultValue = "7") Integer days) {
        return Result.success(statsService.trendAll(days));
    }
}

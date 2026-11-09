package com.canteen.service;

import com.canteen.vo.DailyStatVO;
import com.canteen.vo.DishStatVO;
import com.canteen.vo.OverviewVO;
import com.canteen.vo.StallStatVO;

import java.util.List;

/**
 * 统计业务：商家报表（FR-MER-04）、管理端全局统计（FR-ADM-04）
 */
public interface StatsService {

    /** 商家端：营收/订单量按日趋势 + 菜品排行 */
    List<DailyStatVO> trendForMerchant(Long merchantUserId, Integer days);

    List<DishStatVO> topDishesForMerchant(Long merchantUserId, Integer days, Integer limit);

    /** 管理端：全局总览 + 档口排行 + 全局每日趋势 */
    OverviewVO overview();

    List<StallStatVO> stallRanking();

    List<DailyStatVO> trendAll(Integer days);
}

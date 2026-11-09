package com.canteen.service.impl;

import com.canteen.common.BizException;
import com.canteen.entity.Stall;
import com.canteen.mapper.StatsMapper;
import com.canteen.mapper.StallMapper;
import com.canteen.service.DishService;
import com.canteen.service.StatsService;
import com.canteen.vo.DailyStatVO;
import com.canteen.vo.DishStatVO;
import com.canteen.vo.OverviewVO;
import com.canteen.vo.SaleRankVO;
import com.canteen.vo.StallStatVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 统计实现（FR-MER-04 商家报表 / FR-ADM-04 全局统计）
 */
@Service
@RequiredArgsConstructor
public class StatsServiceImpl implements StatsService {

    private final StatsMapper statsMapper;
    private final StallMapper stallMapper;
    private final DishService dishServiceForRank;

    @Override
    public List<DailyStatVO> trendForMerchant(Long merchantUserId, Integer days) {
        List<Long> stallIds = ownStallIds(merchantUserId);
        if (stallIds.isEmpty()) {
            throw new BizException("您还没有档口");
        }
        return statsMapper.selectTrendByStalls(stallIds, validDays(days));
    }

    @Override
    public List<DishStatVO> topDishesForMerchant(Long merchantUserId, Integer days, Integer limit) {
        List<Long> stallIds = ownStallIds(merchantUserId);
        if (stallIds.isEmpty()) {
            throw new BizException("您还没有档口");
        }
        return statsMapper.selectTopDishesByStalls(stallIds, validDays(days), limit(limit));
    }

    @Override
    public OverviewVO overview() {
        Map<String, Object> total = statsMapper.selectOverview();
        Map<String, Object> today = statsMapper.selectTodayOverview();

        OverviewVO vo = new OverviewVO();
        vo.setTotalGmv(toBigDecimal(total != null ? total.get("totalGmv") : null));
        vo.setTotalOrders(toInteger(total != null ? total.get("totalOrders") : null));
        vo.setTodayGmv(toBigDecimal(today != null ? today.get("todayGmv") : null));
        vo.setTodayOrders(toInteger(today != null ? today.get("todayOrders") : null));
        vo.setTotalUsers(statsMapper.countUsers());
        vo.setOpenStalls(statsMapper.countOpenStalls());
        // 全局菜品榜复用热销榜逻辑（Redis 或数据库降级同口径，O-3：累计销量）
        List<SaleRankVO> top = dishServiceForRank.hotRank(5);
        vo.setTopDishes(top);
        return vo;
    }

    @Override
    public List<StallStatVO> stallRanking() {
        return statsMapper.selectStallRanking();
    }

    @Override
    public List<DailyStatVO> trendAll(Integer days) {
        return statsMapper.selectTrendAll(validDays(days));
    }

    private List<Long> ownStallIds(Long merchantUserId) {
        return stallMapper.selectByMerchantUserId(merchantUserId).stream()
                .map(Stall::getId).collect(Collectors.toList());
    }

    private Integer validDays(Integer days) {
        if (days == null || days <= 0) {
            return 7;
        }
        return Math.min(days, 90);
    }

    private Integer limit(Integer limit) {
        if (limit == null || limit <= 0) {
            return 10;
        }
        return Math.min(limit, 50);
    }

    private java.math.BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return java.math.BigDecimal.ZERO;
        }
        if (value instanceof java.math.BigDecimal) {
            return (java.math.BigDecimal) value;
        }
        return new java.math.BigDecimal(value.toString());
    }

    private Integer toInteger(Object value) {
        if (value == null) {
            return 0;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return Integer.valueOf(value.toString());
    }
}

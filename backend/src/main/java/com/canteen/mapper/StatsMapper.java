package com.canteen.mapper;

import com.canteen.vo.DailyStatVO;
import com.canteen.vo.DishStatVO;
import com.canteen.vo.StallStatVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 统计聚合 Mapper（口径：不含已取消订单；金额用明细快照聚合）
 */
@Mapper
public interface StatsMapper {

    /** 商家端：营收/订单量按日趋势（days 向前推几天；本档口集合） */
    List<DailyStatVO> selectTrendByStalls(@Param("stallIds") List<Long> stallIds, @Param("days") Integer days);

    /** 管理端：全局按日趋势 */
    List<DailyStatVO> selectTrendAll(@Param("days") Integer days);

    /** 商家端：本档口集合菜品销量排行 */
    List<DishStatVO> selectTopDishesByStalls(@Param("stallIds") List<Long> stallIds,
                                             @Param("days") Integer days, @Param("limit") Integer limit);

    /** 管理端：全局GMV/订单量（status!=2 不含已取消） */
    Map<String, Object> selectOverview();

    /** 管理端：今日 GMV/订单量 */
    Map<String, Object> selectTodayOverview();

    /** 管理端：档口营收排行 */
    List<StallStatVO> selectStallRanking();

    /** 管理端：用户总数 */
    int countUsers();

    /** 管理端：营业中档口数 */
    int countOpenStalls();
}

package com.canteen.mapper;

import com.canteen.entity.Dish;
import com.canteen.vo.DishVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 菜品表 Mapper：daily_stock 防超卖条件更新是核心
 */
@Mapper
public interface DishMapper {

    /** 通用分页查询（商家端/学生端共用：onlyOnSale 控制是否过滤下架与未审核档口） */
    List<DishVO> selectPage(@Param("canteenId") Long canteenId,
                            @Param("stallId") Long stallId,
                            @Param("categoryId") Long categoryId,
                            @Param("keyword") String keyword,
                            @Param("merchantUserId") Long merchantUserId,
                            @Param("onlyOnSale") boolean onlyOnSale);

    /** 按ID集合批量查（下单校验/快照用） */
    List<DishVO> selectByIds(@Param("ids") List<Long> ids);

    Dish selectById(@Param("id") Long id);

    Dish selectForUpdate(@Param("id") Long id);

    /** 新增商家菜品 */
    int insert(Dish dish);

    int updateSelective(Dish dish);

    /** 逻辑下架即物理删除（演示口径：直接删除记录） */
    int delete(@Param("id") Long id);

    /**
     * ★防超卖整数：条件更新扣库存（影响行数=0 即失败）
     */
    int decreaseStockConditional(@Param("id") Long id, @Param("quantity") Integer quantity);

    /** 成功下单后销量累加 */
    int increaseSaleCount(@Param("id") Long id, @Param("quantity") Integer quantity);

    /** 查当前每日限量（Redis 降级/初始化用） */
    Integer selectDailyStock(@Param("id") Long id);

    /** 热销榜数据库降级：按累计销量取 TopN */
    List<Dish> selectTopSale(@Param("limit") Integer limit);
}

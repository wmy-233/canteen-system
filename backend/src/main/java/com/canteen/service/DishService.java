package com.canteen.service;

import com.canteen.common.PageResult;
import com.canteen.dto.DishSaveDTO;
import com.canteen.vo.DishVO;
import com.canteen.vo.SaleRankVO;

import java.util.List;

/**
 * 菜品业务：学生端浏览（FR-STU-01）、商家端菜品管理（FR-MER-02）、热销榜（FR-STU-10）
 */
public interface DishService {

    /** 学生端分页浏览（仅上架菜品 + 营业/待审核档口） */
    PageResult<DishVO> pageForStudent(Long canteenId, Long stallId, Long categoryId,
                                      String keyword, Integer pageNum, Integer pageSize);

    /** 商家端分页（仅本档口） */
    PageResult<DishVO> pageForMerchant(Long merchantUserId, Long stallId, String keyword,
                                       Integer pageNum, Integer pageSize);

    DishVO detail(Long id);

    /** 商家新增/修改菜品（限本档口） */
    void save(Long merchantUserId, DishSaveDTO dto);

    /** 商家删除菜品（限本档口） */
    void delete(Long merchantUserId, Long id);

    /** 商家上下架（限本档口）；修改限时菜余量会同步 Redis 余量（避免脏缓存） */
    void changeSaleStatus(Long merchantUserId, Long id, Integer status);

    /** 热销榜 TopN：Redis ZSet 可用即实时榜单，不可用降级为数据库 sale_count 排序（O-3 口径：累计销量） */
    List<SaleRankVO> hotRank(int limit);
}

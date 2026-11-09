package com.canteen.service;

import com.canteen.entity.Canteen;
import com.canteen.entity.DishCategory;
import com.canteen.entity.Stall;

import java.util.List;

/**
 * 基础数据链（A1）：食堂/分类/档口查询
 */
public interface BasicDataService {

    List<Canteen> listCanteens();

    List<DishCategory> listCategories();

    /** 学生端：营业中档口（可按食堂过滤） */
    List<Stall> listOpenStalls(Long canteenId);
}

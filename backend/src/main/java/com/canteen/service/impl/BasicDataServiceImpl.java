package com.canteen.service.impl;

import com.canteen.entity.Canteen;
import com.canteen.entity.DishCategory;
import com.canteen.entity.Stall;
import com.canteen.mapper.CanteenMapper;
import com.canteen.mapper.DishCategoryMapper;
import com.canteen.mapper.StallMapper;
import com.canteen.service.BasicDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 基础数据链实现（A1）
 */
@Service
@RequiredArgsConstructor
public class BasicDataServiceImpl implements BasicDataService {

    private final CanteenMapper canteenMapper;
    private final DishCategoryMapper dishCategoryMapper;
    private final StallMapper stallMapper;

    @Override
    public List<Canteen> listCanteens() {
        return canteenMapper.selectAll();
    }

    @Override
    public List<DishCategory> listCategories() {
        return dishCategoryMapper.selectAll();
    }

    @Override
    public List<Stall> listOpenStalls(Long canteenId) {
        // 学生端默认过滤未审核/停业档口
        return stallMapper.selectOpen(canteenId);
    }
}

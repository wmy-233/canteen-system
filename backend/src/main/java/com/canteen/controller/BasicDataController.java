package com.canteen.controller;

import com.canteen.entity.Canteen;
import com.canteen.entity.DishCategory;
import com.canteen.entity.Stall;
import com.canteen.service.BasicDataService;
import com.canteen.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 基础数据链（A1）：食堂/分类/档口浏览
 */
@Tag(name = "基础数据")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class BasicDataController {

    private final BasicDataService basicDataService;

    @Operation(summary = "食堂列表")
    @GetMapping("/canteen/list")
    public Result<List<Canteen>> canteens() {
        return Result.success(basicDataService.listCanteens());
    }

    @Operation(summary = "菜品分类列表")
    @GetMapping("/category/list")
    public Result<List<DishCategory>> categories() {
        return Result.success(basicDataService.listCategories());
    }

    @Operation(summary = "营业中档口（可按食堂过滤；学生端浏览用）")
    @GetMapping("/stall/list")
    public Result<List<Stall>> openStalls(@RequestParam(required = false) Long canteenId) {
        return Result.success(basicDataService.listOpenStalls(canteenId));
    }
}

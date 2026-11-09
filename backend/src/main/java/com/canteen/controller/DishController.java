package com.canteen.controller;

import com.canteen.common.PageResult;
import com.canteen.common.Result;
import com.canteen.dto.DishSaveDTO;
import com.canteen.service.DishService;
import com.canteen.vo.DishVO;
import com.canteen.vo.SaleRankVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 学生端：菜品浏览（FR-STU-01）与热销榜（FR-STU-10）
 */
@Tag(name = "学生端-菜品")
@RestController
@RequestMapping("/api/dish")
@RequiredArgsConstructor
public class DishController {

    private final DishService dishService;

    @Operation(summary = "菜品分页（食堂/档口/分类/搜索/销量余量展示）")
    @GetMapping("/page")
    public Result<PageResult<DishVO>> page(
            @RequestParam(required = false) Long canteenId,
            @RequestParam(required = false) Long stallId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(required = false, defaultValue = "10") Integer pageSize) {
        return Result.success(dishService.pageForStudent(
                canteenId, stallId, categoryId, keyword, pageNum, pageSize));
    }

    @Operation(summary = "菜品详情（含评分聚合）")
    @GetMapping("/{id}")
    public Result<DishVO> detail(@PathVariable Long id) {
        return Result.success(dishService.detail(id));
    }

    @Operation(summary = "实时热销榜（Redis ZSet；Redis 不可用降级数据库销量排序）")
    @GetMapping("/hot")
    public Result<List<SaleRankVO>> hot(@RequestParam(required = false, defaultValue = "10") Integer limit) {
        return Result.success(dishService.hotRank(limit));
    }
}

package com.canteen.controller;

import com.canteen.common.PageResult;
import com.canteen.common.Result;
import com.canteen.common.SecurityUtil;
import com.canteen.dto.DishSaveDTO;
import com.canteen.service.DishService;
import com.canteen.vo.DishVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商家端：菜品管理（FR-MER-02；数据权限"仅本档口"在 Service 校验）
 */
@Tag(name = "商家端-菜品管理")
@RestController
@RequestMapping("/api/merchant/dish")
@RequiredArgsConstructor
public class MerchantDishController {

    private final DishService dishService;

    @Operation(summary = "本档口菜品分页")
    @GetMapping("/page")
    public Result<PageResult<DishVO>> page(
            @RequestParam(required = false) Long stallId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(required = false, defaultValue = "10") Integer pageSize) {
        return Result.success(dishService.pageForMerchant(
                SecurityUtil.getUserId(), stallId, keyword, pageNum, pageSize));
    }

    @Operation(summary = "新增/修改菜品（修改传 id；仅本档口）")
    @PostMapping("/save")
    public Result<Void> save(@Validated @RequestBody DishSaveDTO dto) {
        dishService.save(SecurityUtil.getUserId(), dto);
        return Result.success();
    }

    @Operation(summary = "上下架（status 0下架 1上架）")
    @PutMapping("/{id}/status/{status}")
    public Result<Void> changeStatus(@PathVariable Long id, @PathVariable Integer status) {
        dishService.changeSaleStatus(SecurityUtil.getUserId(), id, status);
        return Result.success();
    }

    @Operation(summary = "删除菜品（仅本档口）")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        dishService.delete(SecurityUtil.getUserId(), id);
        return Result.success();
    }
}

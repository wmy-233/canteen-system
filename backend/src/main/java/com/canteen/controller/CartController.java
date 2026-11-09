package com.canteen.controller;

import com.canteen.common.Result;
import com.canteen.common.SecurityUtil;
import com.canteen.dto.CartAddDTO;
import com.canteen.dto.CartUpdateDTO;
import com.canteen.service.CartService;
import com.canteen.vo.CartVO;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 购物车（FR-STU-03）：登录即可（学生端）
 */
@Tag(name = "购物车")
@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @Operation(summary = "我的购物车（带菜品/档口信息）")
    @GetMapping
    public Result<List<CartVO>> list() {
        return Result.success(cartService.listMyCart());
    }

    @Operation(summary = "加购（同菜品重复加购累加数量；限量菜超余量被拒）")
    @PostMapping
    public Result<Void> add(@Validated @RequestBody CartAddDTO dto) {
        cartService.add(dto);
        return Result.success();
    }

    @Operation(summary = "改量（按 dishId）")
    @PutMapping("/{dishId}")
    public Result<Void> update(@PathVariable Long dishId, @Validated @RequestBody CartUpdateDTO dto) {
        cartService.updateQuantity(dishId, dto.getQuantity());
        return Result.success();
    }

    @Operation(summary = "删除一条")
    @DeleteMapping("/{itemId}")
    public Result<Void> delete(@PathVariable Long itemId) {
        cartService.delete(itemId);
        return Result.success();
    }

    @Operation(summary = "清空购物车")
    @DeleteMapping
    public Result<Void> clear() {
        cartService.clear();
        return Result.success();
    }
}

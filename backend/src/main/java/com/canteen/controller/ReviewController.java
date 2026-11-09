package com.canteen.controller;

import com.canteen.aspect.OpLog;
import com.canteen.common.Result;
import com.canteen.dto.ReviewCreateDTO;
import com.canteen.entity.Review;
import com.canteen.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 评价（FR-STU-09）：仅本人已完成订单
 */
@Tag(name = "评价")
@RestController
@RequestMapping("/api/review")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @Operation(summary = "提交评价（1-5 分 + 文字；同一订单同一菜品一次）")
    @OpLog(module = "评价", action = "提交评价")
    @PostMapping
    public Result<Void> create(@Validated @RequestBody ReviewCreateDTO dto) {
        reviewService.create(dto);
        return Result.success();
    }

    @Operation(summary = "订单的评价列表")
    @GetMapping("/order/{orderId}")
    public Result<List<Review>> list(@PathVariable Long orderId) {
        return Result.success(reviewService.listByOrder(orderId));
    }
}

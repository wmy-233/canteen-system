package com.canteen.controller;

import com.canteen.aspect.OpLog;
import com.canteen.common.PageResult;
import com.canteen.common.Result;
import com.canteen.dto.OrderCreateDTO;
import com.canteen.service.OrderService;
import com.canteen.vo.OrderCreateVO;
import com.canteen.vo.OrderVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 学生端订单（FR-STU-04 下单 / 我的订单 / 取消）
 */
@Tag(name = "学生端-订单")
@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "下单（同一档口单一事务；未传券自动匹配最高可用，配送单可选 deliveryType=1）")
    @OpLog(module = "订单", action = "下单")
    @PostMapping
    public Result<OrderCreateVO> create(@Validated @RequestBody OrderCreateDTO dto) {
        return Result.success(orderService.create(dto));
    }

    @Operation(summary = "我的订单列表（status 0待处理 1已完成 2已取消，null 全部）")
    @GetMapping("/page")
    public Result<PageResult<OrderVO>> page(
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(required = false, defaultValue = "10") Integer pageSize) {
        return Result.success(orderService.myOrders(status, pageNum, pageSize));
    }

    @Operation(summary = "订单详情（本人）")
    @GetMapping("/{id}")
    public Result<OrderVO> detail(@PathVariable Long id) {
        return Result.success(orderService.detail(id));
    }

    @Operation(summary = "取消订单（退回钱包+退回用券）")
    @OpLog(module = "订单", action = "取消")
    @PostMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id) {
        orderService.cancel(id);
        return Result.success();
    }
}

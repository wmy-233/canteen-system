package com.canteen.controller;

import com.canteen.aspect.OpLog;
import com.canteen.common.PageResult;
import com.canteen.common.Result;
import com.canteen.common.SecurityUtil;
import com.canteen.service.OrderService;
import com.canteen.vo.OrderVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商家端：出餐看板（FR-MER-03）：本档口队列 + 取餐码核销
 */
@Tag(name = "商家端-出餐看板")
@RestController
@RequestMapping("/api/merchant/order")
@RequiredArgsConstructor
public class MerchantOrderController {

    private final OrderService orderService;

    @Operation(summary = "本档口订单（status null=全部，0待处理 1已完成 2已取消）")
    @GetMapping("/page")
    public Result<PageResult<OrderVO>> page(
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(required = false, defaultValue = "10") Integer pageSize) {
        return Result.success(orderService.ordersForMerchant(
                SecurityUtil.getUserId(), status, pageNum, pageSize));
    }

    @Operation(summary = "核销出餐（按取餐码对应订单；仅自取单）")
    @OpLog(module = "商家", action = "核销出餐")
    @PostMapping("/{orderId}/verify")
    public Result<Void> verify(@PathVariable Long orderId) {
        orderService.verify(SecurityUtil.getUserId(), orderId);
        return Result.success();
    }
}

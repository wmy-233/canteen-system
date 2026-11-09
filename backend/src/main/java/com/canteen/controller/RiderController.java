package com.canteen.controller;

import com.canteen.aspect.OpLog;
import com.canteen.common.PageResult;
import com.canteen.common.Result;
import com.canteen.dto.RiderAssignDTO;
import com.canteen.dto.RiderSaveDTO;
import com.canteen.dto.RiderUpdateDTO;
import com.canteen.service.RiderService;
import com.canteen.vo.OrderVO;
import com.canteen.vo.RiderVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 骑手管理（A11 专项，管理员"负责骑手"的功能）：全部挂 /api/admin/rider/**（仅 ADMIN）
 * 状态机：0未分配 -> 1配送中 -> 2已送达；指派必须在岗骑手
 */
@Tag(name = "管理端-骑手管理")
@RestController
@RequestMapping("/api/admin/rider")
@RequiredArgsConstructor
public class RiderController {

    private final RiderService riderService;

    @Operation(summary = "骑手列表（keyword 模糊搜姓名/手机号/账号）")
    @GetMapping("/list")
    public Result<PageResult<RiderVO>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(required = false, defaultValue = "10") Integer pageSize) {
        return Result.success(riderService.page(keyword, pageNum, pageSize));
    }

    @Operation(summary = "新增骑手（创建登录账号并绑定 RIDER 角色，默认在岗）")
    @OpLog(module = "骑手", action = "新增")
    @PostMapping("/create")
    public Result<Void> create(@Validated @RequestBody RiderSaveDTO dto) {
        riderService.create(dto);
        return Result.success();
    }

    @Operation(summary = "修改骑手（姓名/手机号/0停用 1在岗；停用即禁登录）")
    @OpLog(module = "骑手", action = "修改")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Validated @RequestBody RiderUpdateDTO dto) {
        riderService.update(id, dto);
        return Result.success();
    }

    @Operation(summary = "待分配配送单列表（delivery_status=0）")
    @GetMapping("/pending-orders")
    public Result<PageResult<OrderVO>> pendingOrders(
            @RequestParam(required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(required = false, defaultValue = "10") Integer pageSize) {
        return Result.success(riderService.pendingOrders(pageNum, pageSize));
    }

    @Operation(summary = "指派骑手（校验在岗；0未分配 -> 1配送中）")
    @OpLog(module = "骑手", action = "指派配送单")
    @PostMapping("/assign")
    public Result<Void> assign(@Validated @RequestBody RiderAssignDTO dto) {
        riderService.assign(dto);
        return Result.success();
    }

    @Operation(summary = "标记送达（1配送中 -> 2已送达，订单同步已完成）")
    @OpLog(module = "骑手", action = "标记送达")
    @PostMapping("/{orderId}/deliver")
    public Result<Void> deliver(@PathVariable Long orderId) {
        riderService.deliver(orderId);
        return Result.success();
    }
}

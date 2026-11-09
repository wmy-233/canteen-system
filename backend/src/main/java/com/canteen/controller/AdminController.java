package com.canteen.controller;

import com.canteen.aspect.OpLog;
import com.canteen.common.PageResult;
import com.canteen.common.Result;
import com.canteen.dto.StallAuditDTO;
import com.canteen.dto.UserRoleDTO;
import com.canteen.dto.UserStatusDTO;
import com.canteen.service.AdminUserService;
import com.canteen.service.StallService;
import com.canteen.vo.StallVO;
import com.canteen.vo.UserAdminVO;
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

import java.util.List;

/**
 * 管理端：用户管理（FR-ADM-01）与档口审核（FR-ADM-02）
 */
@Tag(name = "管理端-用户与档口")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminUserService adminUserService;
    private final StallService stallService;

    // ---------------- 用户管理 ----------------

    @Operation(summary = "用户列表（keyword 模糊搜账号/昵称/手机号）")
    @GetMapping("/user/page")
    public Result<PageResult<UserAdminVO>> users(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(required = false, defaultValue = "10") Integer pageSize) {
        return Result.success(adminUserService.page(keyword, pageNum, pageSize));
    }

    @Operation(summary = "禁用/启用账号（禁用后登录被拒，旧 token 因角色实时查库同样失效）")
    @OpLog(module = "管理", action = "用户状态变更")
    @PostMapping("/user/{id}/status")
    public Result<Void> userStatus(@PathVariable Long id, @Validated @RequestBody UserStatusDTO dto) {
        adminUserService.updateStatus(id, dto);
        return Result.success();
    }

    @Operation(summary = "重置用户角色（STUDENT/MERCHANT/RIDER）")
    @OpLog(module = "管理", action = "重置角色")
    @PostMapping("/user/{id}/role")
    public Result<Void> userRole(@PathVariable Long id, @Validated @RequestBody UserRoleDTO dto) {
        adminUserService.resetRole(id, dto.getRoleCode());
        return Result.success();
    }

    // ---------------- 档口审核 ----------------

    @Operation(summary = "档口列表（status 0停业 1营业 2待审核，null 全部）")
    @GetMapping("/stall/list")
    public Result<List<StallVO>> stalls(@RequestParam(required = false) Integer status) {
        return Result.success(stallService.listForAdmin(status));
    }

    @Operation(summary = "档口审核（pass=true 营业 / false 驳回停业）")
    @OpLog(module = "管理", action = "档口审核")
    @PostMapping("/stall/{stallId}/audit")
    public Result<Void> audit(@PathVariable Long stallId, @Validated @RequestBody StallAuditDTO dto) {
        stallService.audit(stallId, dto);
        return Result.success();
    }
}

package com.canteen.controller;

import com.canteen.common.Result;
import com.canteen.entity.SysOperationLog;
import com.canteen.mapper.SysOperationLogMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 管理端：AOP 操作日志查询（/api/admin/** → ROLE_ADMIN） */
@Tag(name = "管理端-操作日志")
@RestController
@RequestMapping("/api/admin/logs")
@RequiredArgsConstructor
public class AdminLogController {

    private final SysOperationLogMapper operationLogMapper;

    @Operation(summary = "操作日志列表（最近 300 条，支持模块/关键字/成功失败过滤）")
    @GetMapping
    public Result<List<SysOperationLog>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) Integer success) {
        return Result.success(operationLogMapper.selectPage(keyword, module, success));
    }

    @Operation(summary = "清空操作日志")
    @DeleteMapping
    public Result<Void> clear() {
        operationLogMapper.deleteAll();
        return Result.success();
    }
}

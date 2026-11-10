package com.canteen.controller;

import com.canteen.common.Result;
import com.canteen.common.SecurityUtil;
import com.canteen.dto.ComplaintCreateDTO;
import com.canteen.dto.ComplaintReplyDTO;
import com.canteen.service.ComplaintService;
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
import java.util.Map;

/**
 * 投诉工单（学生端公开前缀 /api/complaint，
 * 商家/管理端分别挂在 /api/merchant /api/admin 下走角色 URL 规则）
 */
@Tag(name = "投诉工单")
@RestController
@RequestMapping("/api/complaint")
@RequiredArgsConstructor
public class ComplaintController {

    private final ComplaintService complaintService;

    @Operation(summary = "学生：发起投诉 { orderId, type(1菜品质量2漏发缺件3配送4服务态度), content }")
    @PostMapping
    public Result<Void> create(@Validated @RequestBody ComplaintCreateDTO dto) {
        complaintService.create(dto.getOrderId(), dto.getType(), dto.getContent());
        return Result.success();
    }

    @Operation(summary = "学生：我的投诉列表")
    @GetMapping("/mine")
    public Result<List<Map<String, Object>>> mine() {
        return Result.success(complaintService.listMine(SecurityUtil.getUserId()));
    }
}

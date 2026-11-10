package com.canteen.controller;

import com.canteen.common.Result;
import com.canteen.dto.ComplaintResolveDTO;
import com.canteen.entity.Complaint;
import com.canteen.mapper.ComplaintMapper;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 管理端：投诉裁定（URL 规则 /api/admin/** → ROLE_ADMIN）
 */
@Tag(name = "管理端投诉裁定")
@RestController
@RequestMapping("/api/admin/complaint")
@RequiredArgsConstructor
public class AdminComplaintController {

    private final ComplaintService complaintService;
    private final ComplaintMapper complaintMapper;

    @Operation(summary = "全部投诉列表（status: 0待处理 1已回应 2已裁定）")
    @GetMapping("/list")
    public Result<List<Map<String, Object>>> list(@RequestParam(required = false) Integer status) {
        return Result.success(complaintMapper.selectAll(status));
    }

    @Operation(summary = "裁定投诉 { pass, content, refund(true=全额退款) }")
    @PostMapping("/{id}/resolve")
    public Result<Void> resolve(@PathVariable Long id, @Validated @RequestBody ComplaintResolveDTO dto) {
        complaintService.resolve(id, Boolean.TRUE.equals(dto.getPass()), dto.getContent(), Boolean.TRUE.equals(dto.getRefund()));
        return Result.success();
    }
}

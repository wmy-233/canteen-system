package com.canteen.controller;

import com.canteen.common.Result;
import com.canteen.dto.ComplaintReplyDTO;
import com.canteen.entity.Stall;
import com.canteen.mapper.StallMapper;
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
import java.util.stream.Collectors;

/**
 * 商家端：投诉处理（URL 规则 /api/merchant/** → ROLE_MERCHANT）
 */
@Tag(name = "商家投诉处理")
@RestController
@RequestMapping("/api/merchant/complaint")
@RequiredArgsConstructor
public class MerchantComplaintController {

    private final ComplaintService complaintService;
    private final StallMapper stallMapper;

    @Operation(summary = "本店投诉列表（status: 0/1/2 可过滤）")
    @GetMapping("/list")
    public Result<List<Map<String, Object>>> list(@RequestParam(required = false) Integer status) {
        Long uid = com.canteen.common.SecurityUtil.getUserId();
        List<Long> stallIds = stallMapper.selectByMerchantUserId(uid)
                .stream().map(Stall::getId).collect(Collectors.toList());
        return Result.success(complaintService.listForMerchant(stallIds, status));
    }

    @Operation(summary = "回应投诉 { content }")
    @PostMapping("/{id}/reply")
    public Result<Void> reply(@PathVariable Long id, @Validated @RequestBody ComplaintReplyDTO dto) {
        complaintService.reply(id, dto.getContent());
        return Result.success();
    }
}

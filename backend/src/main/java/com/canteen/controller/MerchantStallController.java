package com.canteen.controller;

import com.canteen.common.Result;
import com.canteen.common.SecurityUtil;
import com.canteen.dto.StallSaveDTO;
import com.canteen.service.StallService;
import com.canteen.vo.StallVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 商家端：档口信息维护（FR-MER-01）
 */
@Tag(name = "商家端-档口")
@RestController
@RequestMapping("/api/merchant/stall")
@RequiredArgsConstructor
public class MerchantStallController {

    private final StallService stallService;

    @Operation(summary = "我的档口")
    @GetMapping("/mine")
    public Result<List<StallVO>> mine() {
        return Result.success(stallService.listMine(SecurityUtil.getUserId()));
    }

    @Operation(summary = "修改档口名称/营业状态（仅本档口；审核中不可改）")
    @PutMapping("/{stallId}")
    public Result<Void> update(@PathVariable Long stallId, @Validated @RequestBody StallSaveDTO dto) {
        stallService.updateMine(SecurityUtil.getUserId(), stallId, dto);
        return Result.success();
    }
}

package com.canteen.controller;

import com.canteen.aspect.OpLog;
import com.canteen.common.Result;
import com.canteen.dto.WalletTopUpDTO;
import com.canteen.service.WalletService;
import com.canteen.vo.WalletVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 钱包（FR-STU-06）：余额+充值
 */
@Tag(name = "钱包")
@RestController
@RequestMapping("/api/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @Operation(summary = "我的钱包（余额+流水分页）")
    @GetMapping
    public Result<WalletVO> my(@RequestParam(required = false, defaultValue = "1") Integer pageNum,
                               @RequestParam(required = false, defaultValue = "10") Integer pageSize) {
        return Result.success(walletService.myWallet(pageNum, pageSize));
    }

    @Operation(summary = "充值（模拟钱包）")
    @OpLog(module = "钱包", action = "充值")
    @PostMapping("/recharge")
    public Result<Void> recharge(@Validated @RequestBody WalletTopUpDTO dto) {
        walletService.recharge(dto);
        return Result.success();
    }
}

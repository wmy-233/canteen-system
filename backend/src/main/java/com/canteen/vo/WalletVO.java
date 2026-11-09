package com.canteen.vo;

import com.canteen.common.PageResult;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 钱包 VO：余额 + 流水列表（流水按时间倒序分页）
 */
@Data
public class WalletVO {

    /** user.balance 当前余额 */
    private BigDecimal balance;

    /** 流水（分页） */
    private PageResult<WalletRecordVO> records;
}

package com.canteen.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 钱包流水 VO
 */
@Data
public class WalletRecordVO {

    private Long id;

    /** 1充值 2消费 3退款 */
    private Integer type;

    private BigDecimal amount;

    private String refOrderNo;

    private LocalDateTime createTime;
}

package com.canteen.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 钱包流水实体：余额 = 流水累计
 */
@Data
public class WalletRecord {

    private Long id;

    private Long userId;

    /** 1充值 2消费 3退款 */
    private Integer type;

    private BigDecimal amount;

    /** 退款/退款对应订单号 */
    private String refOrderNo;

    private LocalDateTime createTime;
}

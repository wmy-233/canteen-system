package com.canteen.service;

import com.canteen.dto.WalletTopUpDTO;
import com.canteen.vo.WalletVO;

/**
 * 钱包业务（FR-STU-06：充值/余额/扣款/退款流水）
 */
public interface WalletService {

    /** 余额 + 流水（分页，按时间倒序） */
    WalletVO myWallet(Integer pageNum, Integer pageSize);

    void recharge(WalletTopUpDTO dto);
}

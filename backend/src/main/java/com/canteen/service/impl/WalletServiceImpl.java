package com.canteen.service.impl;

import com.canteen.common.BizException;
import com.canteen.common.PageResult;
import com.canteen.common.SecurityUtil;
import com.canteen.dto.WalletTopUpDTO;
import com.canteen.entity.User;
import com.canteen.entity.WalletRecord;
import com.canteen.mapper.UserMapper;
import com.canteen.mapper.WalletRecordMapper;
import com.canteen.service.WalletService;
import com.canteen.vo.WalletRecordVO;
import com.canteen.vo.WalletVO;
import com.github.pagehelper.PageHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 模拟钱包实现（FR-STU-06）：余额=流水累计；扣款/退款与订单同事务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {

    private final UserMapper userMapper;
    private final WalletRecordMapper walletRecordMapper;

    @Override
    public WalletVO myWallet(Integer pageNum, Integer pageSize) {
        Long userId = SecurityUtil.getUserId();
        pageNum = pageNum == null ? 1 : pageNum;
        pageSize = pageSize == null ? 10 : pageSize;

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException("用户不存在");
        }
        PageHelper.startPage(pageNum, pageSize);
        java.util.List<WalletRecordVO> records = walletRecordMapper.selectPageByUserId(userId);

        WalletVO vo = new WalletVO();
        vo.setBalance(user.getBalance());
        vo.setRecords(PageResult.build(records, pageNum, pageSize));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recharge(WalletTopUpDTO dto) {
        Long userId = SecurityUtil.getUserId();
        WalletRecord record = new WalletRecord();
        record.setUserId(userId);
        record.setType(1);
        record.setAmount(dto.getAmount());
        walletRecordMapper.insert(record);
        userMapper.addBalance(userId, dto.getAmount());
        log.info("钱包充值: userId={}, amount={}", userId, dto.getAmount());
    }
}

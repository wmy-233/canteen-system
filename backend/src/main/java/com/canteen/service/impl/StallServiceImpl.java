package com.canteen.service.impl;

import com.canteen.common.BizException;
import com.canteen.dto.StallAuditDTO;
import com.canteen.dto.StallSaveDTO;
import com.canteen.entity.Stall;
import com.canteen.mapper.StallMapper;
import com.canteen.service.StallService;
import com.canteen.vo.StallVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 档口实现（FR-MER-01 / FR-ADM-02）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StallServiceImpl implements StallService {

    private final StallMapper stallMapper;
    private final com.canteen.common.MessageSender messageSender;

    @Override
    public List<StallVO> listMine(Long merchantUserId) {
        List<Stall> stalls = stallMapper.selectByMerchantUserId(merchantUserId);
        return stalls.stream().map(this::toVo).collect(java.util.stream.Collectors.toList());
    }

    @Override
    public void updateMine(Long merchantUserId, Long stallId, StallSaveDTO dto) {
        Stall stall = mustOwn(merchantUserId, stallId);
        Stall update = new Stall();
        update.setId(stall.getId());
        // 商家不可以自行改成"待审核"，只允许 0停业/1营业；待审核档口禁止营业操作
        update.setName(dto.getName());
        update.setStatus(dto.getStatus());
        if (stall.getStatus() == 2) {
            throw new BizException("档口正在审核中，暂不能修改营业状态");
        }
        stallMapper.updateSelective(update);
        log.info("档口信息更新: stallId={}, operator={}", stallId, merchantUserId);
    }

    @Override
    public List<StallVO> listForAdmin(Integer status) {
        return stallMapper.selectAll(status).stream()
                .map(this::toVo).collect(java.util.stream.Collectors.toList());
    }

    @Override
    public void audit(Long stallId, StallAuditDTO dto) {
        Stall stall = stallMapper.selectById(stallId);
        if (stall == null) {
            throw new BizException("档口不存在");
        }
        if (stall.getStatus() != 2) {
            throw new BizException("该档口不在待审核状态");
        }
        // 通过=1 营业；驳回=0 停业
        int newStatus = Boolean.TRUE.equals(dto.getPass()) ? 1 : 0;
        stallMapper.updateStatus(stallId, newStatus);
        // 站内消息通知商家审核结果
        messageSender.send(stall.getMerchantUserId(),
                Boolean.TRUE.equals(dto.getPass()) ? "档口审核通过 🎉" : "档口审核未通过",
                Boolean.TRUE.equals(dto.getPass())
                        ? "你的档口『" + stall.getName() + "』已通过审核，正式进入营业状态，可上架菜品接单啦！"
                        : "你申请的档口『" + stall.getName() + "』未通过审核，若有疑问请联系管理员复核。",
                com.canteen.common.MessageSender.TYPE_AUDIT);
        log.info("档口审核: stallId={}, 结果={}", stallId, Boolean.TRUE.equals(dto.getPass()) ? "通过" : "驳回");
    }

    /** 数据权限：商家只能操作自己的档口 */
    private Stall mustOwn(Long merchantUserId, Long stallId) {
        Stall stall = stallMapper.selectById(stallId);
        if (stall == null) {
            throw new BizException("档口不存在");
        }
        if (!stall.getMerchantUserId().equals(merchantUserId)) {
            log.warn("档口越权操作被拦截: stallId={}, operator={}", stallId, merchantUserId);
            throw new BizException("只能操作本档口");
        }
        return stall;
    }

    private StallVO toVo(Stall stall) {
        StallVO vo = new StallVO();
        vo.setId(stall.getId());
        vo.setCanteenId(stall.getCanteenId());
        vo.setMerchantUserId(stall.getMerchantUserId());
        vo.setName(stall.getName());
        vo.setStatus(stall.getStatus());
        vo.setCreateTime(stall.getCreateTime());
        return vo;
    }
}

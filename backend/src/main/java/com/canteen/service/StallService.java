package com.canteen.service;

import com.canteen.dto.StallSaveDTO;
import com.canteen.dto.StallAuditDTO;
import com.canteen.vo.StallVO;

import java.util.List;

/**
 * 档口业务：商家信息维护（FR-MER-01）、管理员审核（FR-ADM-02）
 */
public interface StallService {

    /** 商家本人档口（一般一个，可能多个） */
    List<StallVO> listMine(Long merchantUserId);

    void updateMine(Long merchantUserId, Long stallId, StallSaveDTO dto);

    /** 管理端：按状态查（null 全部） */
    List<StallVO> listForAdmin(Integer status);

    /** 管理端审核：通过=1 营业 / 驳回=0 停业 */
    void audit(Long stallId, StallAuditDTO dto);
}

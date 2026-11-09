package com.canteen.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 管理员档口审核 DTO（FR-ADM-02：通过=1 营业 / 驳回=0 停业）
 */
@Data
public class StallAuditDTO {

    @NotNull(message = "审核结论不能为空")
    private Boolean pass;
}

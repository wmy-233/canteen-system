package com.canteen.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 商家档口信息维护 DTO（FR-MER-01：名称 + 今日营业状态）
 */
@Data
public class StallSaveDTO {

    @Size(max = 50, message = "档口名称过长")
    private String name;

    /** 0停业 1营业（不允许商家自改为"待审核"，Service 全量接管） */
    @Min(value = 0, message = "状态非法")
    @Max(value = 1, message = "商家仅可设置营业/停业")
    private Integer status;
}

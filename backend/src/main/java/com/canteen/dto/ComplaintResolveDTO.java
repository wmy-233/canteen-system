package com.canteen.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 管理员裁定投诉请求
 */
@Data
@Schema(description = "投诉裁定请求")
public class ComplaintResolveDTO {

    @Schema(description = "是否成立（true=支持学生）", example = "true")
    @NotNull(message = "请选择裁定结果")
    private Boolean pass;

    @Schema(description = "裁定说明", example = "经核实情况属实")
    @Size(max = 300, message = "裁定说明最长300字")
    private String content;

    @Schema(description = "成立时是否全额退款", example = "true")
    private Boolean refund;
}

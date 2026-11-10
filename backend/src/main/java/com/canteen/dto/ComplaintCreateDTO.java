package com.canteen.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 学生投诉发起请求
 */
@Data
@Schema(description = "投诉发起请求")
public class ComplaintCreateDTO {

    @Schema(description = "关联订单ID", example = "1")
    @NotNull(message = "订单不能为空")
    private Long orderId;

    @Schema(description = "投诉类型 1菜品质量 2漏发缺件 3配送问题 4服务态度", example = "1")
    @NotNull(message = "请选择投诉类型")
    @Min(value = 1, message = "投诉类型不合法")
    @Max(value = 4, message = "投诉类型不合法")
    private Integer type;

    @Schema(description = "投诉内容", example = "菜里有异物")
    @NotNull(message = "请填写投诉内容")
    @Size(min = 5, max = 300, message = "投诉内容5-300字")
    private String content;
}

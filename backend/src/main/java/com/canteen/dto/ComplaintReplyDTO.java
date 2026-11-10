package com.canteen.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 商家投诉回应请求
 */
@Data
@Schema(description = "投诉回应请求")
public class ComplaintReplyDTO {

    @Schema(description = "回应说明", example = "已与顾客电话沟通致歉并补发")
    @NotNull(message = "请填写回应说明")
    @Size(min = 5, max = 300, message = "回应说明5-300字")
    private String content;
}

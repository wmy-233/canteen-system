package com.canteen.dto;

import lombok.Data;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * 商家菜品新增/修改 DTO（FR-MER-02）
 */
@Data
public class DishSaveDTO {

    /** 修改时必传，新增为空 */
    private Long id;

    /** 档口ID（商家仅能操作本档口，Service 校验） */
    @NotNull(message = "档口不能为空")
    private Long stallId;

    @NotNull(message = "分类不能为空")
    private Long categoryId;

    @NotNull(message = "菜品名称不能为空")
    @Size(max = 50, message = "菜品名称过长")
    private String name;

    @Size(max = 255, message = "描述过长")
    private String description;

    @NotNull(message = "价格不能为空")
    @DecimalMin(value = "0.01", message = "价格必须大于 0")
    private BigDecimal price;

    private String image;

    /** 每日限量（0 不限量；>0 为限量菜） */
    @Min(value = 0, message = "限量数不能为负")
    @Max(value = 9999, message = "限量数过大")
    private Integer dailyStock;

    /** 0下架 1上架 */
    @Min(value = 0, message = "状态非法")
    @Max(value = 1, message = "状态非法")
    private Integer status;
}

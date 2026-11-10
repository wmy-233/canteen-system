package com.canteen.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.ToString;

import javax.validation.constraints.Max;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * 商家入驻注册请求：账号 + 档口申请信息
 * 严谨逻辑：创建 MERCHANT 账号的同时必须提交开店申请，
 * 生成的档口为「待审核(status=2)」状态，须管理员在档口审核页通过后才可营业。
 */
@Data
@Schema(description = "商家入驻注册请求（公开接口，注册即申请档口，待管理员审核）")
public class RegisterMerchantRequest {

    @Schema(description = "登录账号", example = "merchant3")
    @NotBlank(message = "账号不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9_]{3,20}$", message = "账号为3-20位字母数字下划线")
    private String username;

    @Schema(description = "密码", example = "123456")
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度6-20位")
    @ToString.Exclude  // 防止密码随 toString 进入日志（LogAspect 会记录参数）
    private String password;

    @Schema(description = "商家昵称/店铺联系人", example = "王老板")
    @Size(max = 50, message = "昵称最长50字符")
    private String nickname;

    @Schema(description = "联系电话（便于审核沟通与配送联系）", example = "13900001111")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @Schema(description = "申请入驻的档口名称", example = "香满楼卤味档")
    @NotBlank(message = "档口名称不能为空")
    @Size(min = 2, max = 30, message = "档口名2-30字")
    private String stallName;

    @Schema(description = "所属食堂ID（必选一个真实存在的食堂）", example = "1")
    @NotNull(message = "请选择所属食堂")
    @Max(value = 999999, message = "食堂ID不合法")
    private Long canteenId;
}

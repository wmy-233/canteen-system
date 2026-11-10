package com.canteen.service;

import com.canteen.dto.LoginRequest;
import com.canteen.dto.RegisterRequest;
import com.canteen.vo.LoginResponse;

/**
 * 用户业务接口
 */
public interface UserService {

    /** 注册（默认 STUDENT 角色），成功后直接返回登录态 */
    LoginResponse register(RegisterRequest request);

    /**
     * 商家入驻注册：创建 MERCHANT 账号 + 待审核档口申请
     * （档口 status=2，需管理员在档口审核中通过后才能营业）
     */
    LoginResponse registerMerchant(com.canteen.dto.RegisterMerchantRequest request);

    /** 登录校验并返回 token 与角色 */
    LoginResponse login(LoginRequest request);
}

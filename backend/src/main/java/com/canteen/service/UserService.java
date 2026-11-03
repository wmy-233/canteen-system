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

    /** 登录校验并返回 token 与角色 */
    LoginResponse login(LoginRequest request);
}

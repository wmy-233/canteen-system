import request from './request'

/**
 * 用户相关接口
 * 后端：POST /api/user/login → Result<LoginResponse{ token, userId, username, nickname, role }>
 */
export function login(data) {
  return request.post('/user/login', data)
}

/** 学生注册（公开，注册即 STUDENT 角色并自动登录） */
export function register(data) {
  return request.post('/user/register', data)
}

/**
 * 商家入驻注册（公开）：注册即申请档口
 * data: { username, password, nickname?, phone, stallName, canteenId }
 * 生成档口 status=2 待审核，管理员在「档口审核」通过后才能营业
 */
export function registerMerchant(data) {
  return request.post('/user/register-merchant', data)
}

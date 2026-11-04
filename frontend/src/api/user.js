import request from './request'

/**
 * 用户相关接口
 * 后端：POST /api/user/login → Result<LoginResponse{ token, userId, username, nickname, role }>
 */
export function login(data) {
  return request.post('/user/login', data)
}

export function register(data) {
  return request.post('/user/register', data)
}

import { defineStore } from 'pinia'

/**
 * 用户状态管理：登录信息、token、角色
 */
export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    userInfo: null,   // { id, username, nickname, role }
    role: localStorage.getItem('role') || ''
  }),
  actions: {
    setLogin({ token, userInfo }) {
      this.token = token
      this.userInfo = userInfo
      this.role = userInfo?.role || ''
      localStorage.setItem('token', token)
      localStorage.setItem('role', this.role)
    },
    logout() {
      this.token = ''
      this.userInfo = null
      this.role = ''
      localStorage.removeItem('token')
      localStorage.removeItem('role')
    }
  }
})

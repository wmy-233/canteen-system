import { defineStore } from 'pinia'

/**
 * 用户状态管理：登录信息、token、角色
 */
export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    userInfo: null,   // { id, username, nickname, role }
    role: localStorage.getItem('role') || '',
    // 登录页选择的门户（仅用于 UI 主题与体验，不参与鉴权）
    portal: sessionStorage.getItem('portal') || ''
  }),
  actions: {
    setLogin({ token, userInfo }) {
      this.token = token
      this.userInfo = userInfo
      this.role = userInfo?.role || ''
      localStorage.setItem('token', token)
      localStorage.setItem('role', this.role)
    },
    setPortal(portal) {
      this.portal = portal
      sessionStorage.setItem('portal', portal)
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

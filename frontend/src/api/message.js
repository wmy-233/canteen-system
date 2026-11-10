import request from './request'

// 站内消息中心（三端共用）
export const messagePage = (params) => request.get('/message/page', { params })
export const messageUnreadCount = () => request.get('/message/unread-count')
export const messageRead = (id) => request.post(`/message/${id}/read`)
export const messageReadAll = () => request.post('/message/read-all')

export const MESSAGE_TYPE = {
  0: '系统',
  1: '订单',
  2: '入驻',
  3: '投诉',
  4: '配送'
}
export const MESSAGE_ICON = {
  0: '📢',
  1: '🍱',
  2: '🏪',
  3: '📮',
  4: '🛵'
}

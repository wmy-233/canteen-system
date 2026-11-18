import request from './request'

// 骑手端（/api/rider/** = ROLE_RIDER）
export const riderMe = () => request.get('/rider/me')
export const riderPending = () => request.get('/rider/orders/pending')
export const riderMine = () => request.get('/rider/orders/mine')
export const riderGrab = (orderId) => request.post(`/rider/orders/${orderId}/grab`)
export const riderDeliver = (orderId) => request.post(`/rider/orders/${orderId}/deliver`)

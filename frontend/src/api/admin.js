import request from './request'

// ---- 管理端（/api/admin/** = ROLE_ADMIN）----
// 用户管理
export const userPage = (params) => request.get('/admin/user/page', { params })           // { keyword?, pageNum, pageSize }
export const userChangeStatus = (id, data) => request.post(`/admin/user/${id}/status`, data) // { status: 0禁用|1启用 }
export const userChangeRole = (id, data) => request.post(`/admin/user/${id}/role`, data)     // { roleCode }

// 档口审核
export const stallList = (params) => request.get('/admin/stall/list', { params })            // { status? }
export const stallAudit = (stallId, data) => request.post(`/admin/stall/${stallId}/audit`, data) // { pass: Boolean }

// 优惠券
export const couponGrant = (data) => request.post('/admin/coupon', data) // { name, threshold, amount, stock, expireTime }
export const couponList = () => request.get('/admin/coupon/list')

// 骑手调度
export const riderPage = (params) => request.get('/admin/rider/list', { params })            // { keyword?, pageNum, pageSize }
export const riderCreate = (data) => request.post('/admin/rider/create', data)                // { username, password, name, phone }
export const riderUpdate = (id, data) => request.put(`/admin/rider/${id}`, data)              // { name?, phone?, status }
export const riderPendingOrders = (params) => request.get('/admin/rider/pending-orders', { params })
export const riderAssign = (data) => request.post('/admin/rider/assign', data)                // { orderId, riderId }
export const riderDeliver = (orderId) => request.post(`/admin/rider/${orderId}/deliver`)

// 全局统计
export const statsOverview = () => request.get('/admin/stats/overview')
export const statsStallRanking = () => request.get('/admin/stats/stall-ranking')
export const statsTrend = (params) => request.get('/admin/stats/trend', { params }) // { days }

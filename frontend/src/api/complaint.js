import request from './request'

export const COMPLAINT_TYPE = { 1: '菜品质量', 2: '漏发缺件', 3: '配送问题', 4: '服务态度' }
export const COMPLAINT_STATUS = { 0: '待处理', 1: '商家已回应', 2: '已裁定' }

// ---- 学生端 ----
export const complaintCreate = (data) => request.post('/complaint', data) // { orderId, type, content }
export const complaintMine = () => request.get('/complaint/mine')

// ---- 商家端（/api/merchant/**）----
export const complaintStallList = (params) => request.get('/merchant/complaint/list', { params })
export const complaintReply = (id, data) => request.post(`/merchant/complaint/${id}/reply`, data) // { content }

// ---- 管理端（/api/admin/**）----
export const complaintAdminList = (params) => request.get('/admin/complaint/list', { params })
export const complaintResolve = (id, data) => request.post(`/admin/complaint/${id}/resolve`, data) // { pass, content, refund }

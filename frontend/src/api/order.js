import request from './request'

// 订单（下单即钱包扣款，无独立支付接口）
// data: { deliveryType: 0自取|1配送, couponId?: 用户领券记录ID, items: [{dishId, quantity}] }
export const orderCreate = (data) => request.post('/order', data)
export const orderPage = (params) => request.get('/order/page', { params }) // { status?: 0/1/2, pageNum, pageSize }
export const orderDetail = (id) => request.get(`/order/${id}`)
export const orderCancel = (id) => request.post(`/order/${id}/cancel`)

// 订单状态
export const ORDER_STATUS = { 0: '待处理', 1: '已完成', 2: '已取消' }
export const DELIVERY_TYPE = { 0: '自取', 1: '配送' }
export const DELIVERY_STATUS = { 0: '待分配', 1: '配送中', 2: '已送达' }

// 评价
export const reviewCreate = (data) => request.post('/review', data) // { orderId, dishId, score(1-5), content? }
export const reviewByOrder = (orderId) => request.get(`/review/order/${orderId}`)

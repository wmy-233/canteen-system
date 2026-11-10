import request from './request'

// ---- 商家端（/api/merchant/** = ROLE_MERCHANT）----
export const stallMine = () => request.get('/merchant/stall/mine')
export const stallSave = (stallId, data) => request.put(`/merchant/stall/${stallId}`, data) // { name?, status: 0停业|1营业 }

export const dishPage = (params) => request.get('/merchant/dish/page', { params })
// data: { id?, stallId, categoryId, name, description?, price, image?, dailyStock, status }
export const dishSave = (data) => request.post('/merchant/dish/save', data)
export const dishChangeStatus = (id, status) => request.put(`/merchant/dish/${id}/status/${status}`)
export const dishDelete = (id) => request.delete(`/merchant/dish/${id}`)

export const orderPage = (params) => request.get('/merchant/order/page', { params }) // { status?, pageNum, pageSize }
export const orderVerify = (orderId) => request.post(`/merchant/order/${orderId}/verify`) // 自取单核销出餐

export const statsTrend = (params) => request.get('/merchant/stats/trend', { params })   // { days }
export const statsTopDishes = (params) => request.get('/merchant/stats/top-dishes', { params }) // { days, limit }

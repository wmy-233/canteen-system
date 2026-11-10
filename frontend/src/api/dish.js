import request from './request'

// 菜品浏览
export const dishPage = (params) => request.get('/dish/page', { params })
export const dishDetail = (id) => request.get(`/dish/${id}`)
export const dishHot = (params) => request.get('/dish/hot', { params })

// 购物车
export const cartList = () => request.get('/cart')
export const cartAdd = (data) => request.post('/cart', data)                      // { dishId, quantity }
export const cartUpdate = (dishId, data) => request.put(`/cart/${dishId}`, data)  // { quantity }
export const cartRemove = (itemId) => request.delete(`/cart/${itemId}`)
export const cartClear = () => request.delete('/cart')

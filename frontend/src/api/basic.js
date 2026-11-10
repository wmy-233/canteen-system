import request from './request'

// 基础数据：食堂 / 分类 / 档口（登录即可访问）
export const canteenList = () => request.get('/canteen/list')
export const categoryList = () => request.get('/category/list')
export const stallList = (params) => request.get('/stall/list', { params })

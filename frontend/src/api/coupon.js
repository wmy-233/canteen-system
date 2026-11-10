import request from './request'

// 学生侧
export const couponIssuing = () => request.get('/coupon/issuing')
export const couponMine = () => request.get('/coupon/mine')
export const couponClaim = (couponId) => request.post(`/coupon/${couponId}/claim`)

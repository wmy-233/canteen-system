import request from './request'

// 钱包：GET /wallet 返回 { balance, records: PageResult } —— 余额和流水一次拿到
export const walletInfo = (params) => request.get('/wallet', { params })
export const walletRecharge = (data) => request.post('/wallet/recharge', data) // { amount: ≥0.01 }

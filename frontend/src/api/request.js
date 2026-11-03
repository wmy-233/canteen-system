import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'
import { useUserStore } from '../stores/user'

// axios 全局封装：统一 baseURL / token 携带 / 统一错误提示
const request = axios.create({
  baseURL: '/api',
  timeout: 10000
})

// 请求拦截器：自动携带 token
request.interceptors.request.use(config => {
  const userStore = useUserStore()
  if (userStore.token) {
    config.headers['Authorization'] = 'Bearer ' + userStore.token
  }
  return config
})

// 响应拦截器：统一处理错误
request.interceptors.response.use(
  response => {
    const res = response.data
    // 统一返回体 { code, message, data }
    if (res.code && res.code !== 200) {
      ElMessage.error(res.message || '请求失败')
      return Promise.reject(res)
    }
    return res
  },
  error => {
    if (error.response && error.response.status === 401) {
      // token 失效：清空状态并跳回登录页
      const userStore = useUserStore()
      userStore.logout()
      router.push('/login')
    }
    ElMessage.error(error.response?.data?.message || '网络异常，请稍后重试')
    return Promise.reject(error)
  }
)

export default request

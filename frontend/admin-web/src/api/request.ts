// axios 实例：基础 baseURL、token 注入、统一错误处理
import axios, { type AxiosInstance } from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import router from '@/router'

// ponytail: 动态 baseURL — 跟随当前页面所在 host,避免 NAT 回环
// 浏览器从 sandbox 用 42.193.183.187 访问自己,POST 永远 pending
// 用 window.location.host 同源拼接,本机测试走 127.0.0.1:8081,
// 用户从外网走 42.193.183.187:8081(实际就是他们访问的 IP)
const apiBase = `http://${window.location.hostname}:8081/admin/api`
const instance: AxiosInstance = axios.create({
  baseURL: apiBase,
  timeout: 30000
})

// 请求拦截：自动注入 Bearer token
instance.interceptors.request.use((config) => {
  const store = useUserStore()
  if (store.token) config.headers.Authorization = `Bearer ${store.token}`
  return config
})

// 响应拦截：解包 data、统一 401/403/5xx 处理
instance.interceptors.response.use(
  (resp) => resp.data,
  (err) => {
    if (err.response?.status === 401) {
      ElMessage.error('Token 已过期，请重新登录')
      useUserStore().logout()
      router.push('/login')
    } else if (err.response?.status === 403) {
      ElMessage.error('需要管理员权限')
    } else if (err.response?.status >= 500) {
      ElMessage.error('服务异常')
    }
    return Promise.reject(err)
  }
)

export default instance
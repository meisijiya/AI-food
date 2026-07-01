// axios 实例：基础 baseURL、token 注入、统一错误处理
import axios, { type AxiosInstance } from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import router from '@/router'

// ponytail: 用相对路径 + 当前页面的 pathname
// - 用户从 cloud (119.29.52.111/admin/) 访问 → axios 用 /admin/api → nginx 代理 → admin-server
//   CORS 同源,无需 OPTIONS
// - 本地 sandbox (127.0.0.1:5174/admin/) 访问 → axios 用 /admin/api → 5174 自身 → vite preview 返回 SPA HTML → 失败
//   这种情况下需要 vite dev proxy(本地开发用)或用户必须走 cloud
const instance: AxiosInstance = axios.create({
  baseURL: (window.location.pathname.startsWith('/admin/') ? '/admin/api' : `http://${window.location.hostname}:8081/admin/api`),
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
import axios from 'axios'
import type { AxiosInstance, AxiosResponse } from 'axios'
import { useAuthStore } from '@/stores/auth'
import router from '@/router'

// re-entrance guard:防止并发 401 同时触发多次 logout 流程
let isHandlingAuthError = false

// ponytail: 裸 axios 实例 — 仅用于 logout / login 这种"无论 token 状态如何
// 都应自然处理"的端点。无 interceptor,意味着:
//   (1) 这个实例发出的请求失败,不会触发 isHandlingAuthError 路径;
//   (2) 即便有人未来改了 logout 路径名(比如 /api/user/logout),它也不会再
//       走到主 api 的拦截器,URL guard 也就没有"失效路径";
//   (3) 跟主 api 共享 withCredentials/timeout,只是不要 interceptor。
const bareApi: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '/api',
  timeout: 10000,
  withCredentials: true,
  headers: { 'Content-Type': 'application/json' }
})

// ponytail: 裸 axios 实例 — 用于 auth 类端点。无 401 interceptor,只保留响应解包,
// 这样:logout 即使收到 401/403 也不会触发死循环;login/register/send-code 拿到的
// 仍是 unwrapped data,跟原来 request() 行为一致
bareApi.interceptors.response.use(
  (response) => {
    const body = response.data
    if (body && typeof body === 'object' && 'code' in body && 'data' in body) {
      if (body.code !== 200) {
        return Promise.reject(new Error(body.message || '请求失败'))
      }
      return body.data
    }
    return body
  },
  (error) => Promise.reject(error)  // 不做任何 401 处理 — 让调用方决定
)

const api: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '/api',
  timeout: 30000,
  // 安全修复（M2）：让浏览器自动带上 HttpOnly cookie（auth_token），
  // ——这样 XSS 拿不到 token，浏览器请求仍然携带认证凭据
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器
api.interceptors.request.use(
  (config) => {
    const authStore = useAuthStore()
    if (authStore.token) {
      config.headers.Authorization = `Bearer ${authStore.token}`
    }
    // FormData 请求不设置 Content-Type，让浏览器自动添加 boundary
    if (config.data instanceof FormData) {
      delete config.headers['Content-Type']
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截器 - 自动解包 ApiResponse
api.interceptors.response.use(
  (response: AxiosResponse) => {
    const body = response.data
    if (body === undefined || body === null) return body

    // 解包 { code, message, data } → data
    if (typeof body === 'object' && 'code' in body && 'data' in body) {
      if (body.code !== 200) {
        return Promise.reject(new Error(body.message || '请求失败'))
      }
      return body.data
    }

    return body
  },
  (error) => {
    const status = error.response?.status
    const url: string = error.config?.url || ''
    // 401/403 on auth endpoints (login / logout / register / send-code) must NOT
    // trigger the logout flow — that would call /auth/logout, which itself returns
    // 401, looping the interceptor until the browser/service exhausts. Login is
    // wrong-creds (not expired), logout is expected-on-stale-token.
    const isAuthEndpoint = url.startsWith('/auth/')
    // ponytail: re-entrance guard — 并发 5 个 401 进来也只触发 1 次 logout + 1 次跳转,
    // 防止"一个 401 触发 logout → logout 期间其他 401 又触发 logout" 的 cascade
    if ((status === 401 || status === 403) && !isAuthEndpoint && !isHandlingAuthError) {
      isHandlingAuthError = true
      try {
        const authStore = useAuthStore()
        authStore.logout()
        if (router.currentRoute.value.path !== '/login') {
          router.push('/login')
        }
      } finally {
        // 下一轮微任务再复位 — 同步 finally 太早会让本批次其他 401 误闯
        Promise.resolve().then(() => { isHandlingAuthError = false })
      }
    }
    const msg = error.response?.data?.message || error.message || '请求失败'
    console.error('API Error:', status, msg)
    return Promise.reject(error)
  }
)

// 类型安全的请求封装 — 拦截器解包后返回的是 data，不是 AxiosResponse
function request<T = any>(method: string, url: string, data?: any, config?: any): Promise<T> {
  switch (method) {
    case 'get': return api.get(url, config) as any
    case 'post': return api.post(url, data, config) as any
    case 'put': return api.put(url, data, config) as any
    case 'delete': return api.delete(url, { ...(config || {}), data }) as any
    default: throw new Error(`Unknown method: ${method}`)
  }
}

export { api, bareApi, request }

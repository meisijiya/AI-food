import axios from 'axios'
import type { AxiosInstance, AxiosResponse } from 'axios'
import { useAuthStore } from '@/stores/auth'
import router from '@/router'

const api: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器 - 自动添加 Authorization 头
api.interceptors.request.use(
  (config) => {
    const authStore = useAuthStore()
    if (authStore.token) {
      config.headers.Authorization = `Bearer ${authStore.token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截器
api.interceptors.response.use(
  (response: AxiosResponse) => {
    // 检查 x-new-token 头，自动续期
    const newToken = response.headers['x-new-token']
    if (newToken) {
      const authStore = useAuthStore()
      authStore.setToken(newToken)
    }

    const body = response.data

    // 安全检查：body 可能为空
    if (body === undefined || body === null) {
      return body
    }

    // 自动解包 ApiResponse: { code, message, data } → data
    if (typeof body === 'object' && 'code' in body && 'data' in body) {
      if (body.code !== 200) {
        return Promise.reject(new Error(body.message || '请求失败'))
      }
      return body.data
    }

    // 非 ApiResponse 格式（如 ConversationController 直接返回的对象）
    return body
  },
  (error) => {
    const status = error.response?.status
    // 401/403 → 清除 token → 跳转登录页
    if (status === 401 || status === 403) {
      const authStore = useAuthStore()
      authStore.logout()
      if (router.currentRoute.value.path !== '/login') {
        router.push('/login')
      }
    }
    const msg = error.response?.data?.message || error.message || '请求失败'
    console.error('API Error:', status, msg)
    return Promise.reject(error)
  }
)

// 认证相关接口
export const authApi = {
  sendCode: (email: string) => api.post('/auth/send-code', { email }),
  register: (data: { username: string; password: string; nickname: string; email: string; code: string }) =>
    api.post('/auth/register', data),
  login: (data: { username: string; password: string }) => api.post('/auth/login', data)
}

// 用户相关接口
export const userApi = {
  getUserInfo: () => api.get('/user/info'),
  signIn: () => api.post('/user/sign'),
  getSignStatus: () => api.get('/user/sign-status'),
  updateNickname: (nickname: string) => api.put('/user/nickname', { nickname }),
  uploadAvatar: (file: File) => {
    const formData = new FormData()
    formData.append('file', file)
    return api.post('/user/avatar', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  }
}

// 记录相关接口
export const recordApi = {
  getRecordList: (params?: { page?: number; size?: number }) => api.get('/record/list', { params }),
  getRecordDetail: (sessionId: string) => api.get(`/record/detail/${sessionId}`)
}

// 对话相关接口
export const conversationApi = {
  start: () => api.post('/conversation/start'),
  getStatus: (sessionId: string) => api.get(`/conversation/status/${sessionId}`),
  complete: (sessionId: string) => api.post(`/conversation/complete/${sessionId}`),
  getHistory: (sessionId: string) => api.get(`/conversation/history/${sessionId}`)
}

export default api

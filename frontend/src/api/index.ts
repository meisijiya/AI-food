import axios from 'axios'
import type { AxiosInstance, AxiosResponse } from 'axios'
import { useAuthStore } from '@/stores/auth'
import router from '@/router'

const api: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '/api',
  timeout: 30000,
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
    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截器 - 自动解包 ApiResponse
api.interceptors.response.use(
  (response: AxiosResponse) => {
    const newToken = response.headers['x-new-token']
    if (newToken) {
      const authStore = useAuthStore()
      authStore.setToken(newToken)
    }

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

// 类型安全的请求封装 — 拦截器解包后返回的是 data，不是 AxiosResponse
function request<T = any>(method: string, url: string, data?: any, config?: any): Promise<T> {
  switch (method) {
    case 'get': return api.get(url, config) as any
    case 'post': return api.post(url, data, config) as any
    case 'put': return api.put(url, data, config) as any
    case 'delete': return api.delete(url, config) as any
    default: throw new Error(`Unknown method: ${method}`)
  }
}

// 认证相关接口
export const authApi = {
  sendCode: (email: string) => request('post', '/auth/send-code', { email }),
  register: (data: { username: string; password: string; nickname: string; email: string; code: string }) =>
    request('post', '/auth/register', data),
  login: (data: { username: string; password: string }) => request('post', '/auth/login', data)
}

// 用户相关接口
export const userApi = {
  getUserInfo: () => request('get', '/user/info'),
  signIn: () => request('post', '/user/sign'),
  getSignStatus: () => request('get', '/user/sign-status'),
  updateNickname: (nickname: string) => request('put', '/user/nickname', { nickname }),
  uploadAvatar: (file: File) => {
    const formData = new FormData()
    formData.append('file', file)
    return request('post', '/user/avatar', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  }
}

// 记录相关接口
export const recordApi = {
  getRecordList: (params?: { page?: number; size?: number; sort?: string }) => request('get', '/record/list', undefined, { params }),
  getRecordDetail: (sessionId: string) => request('get', `/record/detail/${sessionId}`),
  deleteRecord: (sessionId: string) => request('delete', `/record/delete/${sessionId}`),
  batchDeleteRecords: (sessionIds: string[]) => request('delete', '/record/batch-delete', { sessionIds }),
  updatePhoto: (sessionId: string, photoUrl: string) => request('put', `/record/photo/${sessionId}`, { photoUrl }),
  deletePhoto: (sessionId: string) => request('delete', `/record/photo/${sessionId}`),
  getPendingRecommendation: () => request('get', '/record/pending')
}

// 上传相关接口
export const uploadApi = {
  uploadPhoto: (file: File, sessionId?: string) => {
    const formData = new FormData()
    formData.append('file', file)
    if (sessionId) formData.append('sessionId', sessionId)
    return request<any>('post', '/upload/photo', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  }
}

// 对话相关接口
export const conversationApi = {
  start: () => request('post', '/conversation/start'),
  getStatus: (sessionId: string) => request('get', `/conversation/status/${sessionId}`),
  complete: (sessionId: string) => request('post', `/conversation/complete/${sessionId}`),
  cancel: (sessionId: string) => request('delete', `/conversation/cancel/${sessionId}`),
  getHistory: (sessionId: string) => request('get', `/conversation/history/${sessionId}`)
}

export default api

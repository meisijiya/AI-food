import axios from 'axios'
import type { AxiosInstance, AxiosResponse } from 'axios'

const api: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器
api.interceptors.request.use(
  (config) => {
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 响应拦截器
api.interceptors.response.use(
  (response: AxiosResponse) => {
    return response.data
  },
  (error) => {
    console.error('API Error:', error)
    return Promise.reject(error)
  }
)

// 对话相关接口
export const conversationApi = {
  // 启动新的对话会话
  start: () => api.post('/conversation/start'),
  
  // 获取会话状态
  getStatus: (sessionId: string) => api.get(`/conversation/status/${sessionId}`),
  
  // 手动结束收集，直接进入推荐
  complete: (sessionId: string) => api.post(`/conversation/complete/${sessionId}`),
  
  // 获取对话历史
  getHistory: (sessionId: string) => api.get(`/conversation/history/${sessionId}`)
}

export default api
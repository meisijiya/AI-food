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
    case 'delete': return api.delete(url, { ...(config || {}), data }) as any
    default: throw new Error(`Unknown method: ${method}`)
  }
}

// 认证相关接口
export const authApi = {
  sendCode: (email: string) => request('post', '/auth/send-code', { email }),
  register: (data: { password: string; email: string; code: string }) =>
    request('post', '/auth/register', data),
  login: (data: { email: string; password: string }) => request('post', '/auth/login', data),
  logout: () => request('post', '/auth/logout')
}

// 游客相关接口
export const guestApi = {
  getGuestInfo: () => request('get', '/guest/info'),
  getGuestStats: () => request('get', '/guest/stats')
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
  },
  searchUsers: (params: { keyword: string; page?: number; size?: number }) =>
    request('get', '/user/search', undefined, { params }),
  updatePassword: (data: { oldPassword: string; newPassword: string }) =>
    request('put', '/user/password', data)
}

// 记录相关接口
export const recordApi = {
  getRecordList: (params?: { page?: number; size?: number; sort?: string }) => request('get', '/record/list', undefined, { params }),
  getRecordDetail: (sessionId: string) => request('get', `/record/detail/${sessionId}`),
  deleteRecord: (sessionId: string) => request('delete', `/record/delete/${sessionId}`),
  batchDeleteRecords: (sessionIds: string[]) => request('delete', '/record/batch-delete', { sessionIds }),
  updatePhoto: (sessionId: string, photoUrl: string) => request('put', `/record/photo/${sessionId}`, { photoUrl }),
  deletePhoto: (sessionId: string) => request('delete', `/record/photo/${sessionId}`),
  updateComment: (sessionId: string, comment: string) => request('put', `/record/comment/${sessionId}`, { comment }),
  getPendingRecommendation: () => request('get', '/record/pending')
}

// 上传相关接口
export const uploadApi = {
  uploadPhoto: (file: File, sessionId?: string, oldPhotoUrl?: string) => {
    const formData = new FormData()
    formData.append('file', file)
    if (sessionId) formData.append('sessionId', sessionId)
    if (oldPhotoUrl) formData.append('oldPhotoUrl', oldPhotoUrl)
    return request<any>('post', '/upload/photo', formData)
  },
  uploadChatPhoto: (file: File) => {
    const formData = new FormData()
    formData.append('file', file)
    return request<any>('post', '/upload/chat-photo', formData)
  },
  uploadChatFile: (file: File) => {
    const formData = new FormData()
    formData.append('file', file)
    return request<any>('post', '/upload/chat-file', formData)
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

// 大厅相关接口
export const feedApi = {
  publish: (data: { sessionId: string; commentPreview?: string; visibility?: string }) => 
    request('post', '/feed/publish', data),
  getList: (params?: { page?: number; size?: number; foodName?: string; paramName?: string; paramValue?: string }) =>
    request('get', '/feed/list', undefined, { params }),
  getDetail: (postId: number) => request('get', `/feed/detail/${postId}`),
  toggleLike: (postId: number) => request('post', `/feed/like/${postId}`),
  addComment: (postId: number, content: string, imageUrl?: string) => request('post', `/feed/comment/${postId}`, { content, imageUrl }),
  deleteComment: (commentId: number) => request('delete', `/feed/comment/${commentId}`),
  getComments: (postId: number, params?: { page?: number; size?: number }) =>
    request('get', `/feed/comments/${postId}`, undefined, { params }),
  checkPublished: (sessionId: string) => request('get', `/feed/check/${sessionId}`),
  unpublish: (sessionId: string) => request('delete', `/feed/unpublish/${sessionId}`),
  getHotRank: () => request('get', '/feed/hot-rank'),
  getFriendFeed: (params?: { page?: number; size?: number }) =>
    request('get', '/feed/friend-feed', undefined, { params })
}

// 通知相关接口
export const notificationApi = {
  getList: (params?: { page?: number; size?: number }) =>
    request('get', '/notification/list', undefined, { params }),
  getUnread: () => request('get', '/notification/unread'),
  deleteOne: (notificationId: string) => request('delete', `/notification/${notificationId}`),
  clearAll: () => request('delete', '/notification/clear-all')
}

// 关注相关接口
export const followApi = {
  toggleFollow: (userId: number) => request('post', `/follow/${userId}`),
  getFollowingList: (params?: { page?: number; size?: number }) =>
    request('get', '/follow/following', undefined, { params }),
  getFollowersList: (params?: { page?: number; size?: number }) =>
    request('get', '/follow/followers', undefined, { params }),
  checkFollow: (userId: number) => request('get', `/follow/check/${userId}`),
  getFollowStats: (userId: number) => request('get', `/follow/stats/${userId}`),
  getMyFollowStats: () => request('get', '/follow/stats'),
  getMutualFriends: (params?: { page?: number; size?: number }) =>
    request('get', '/follow/mutual', undefined, { params }),
  checkMutualFollow: (userId: number) => request('get', `/follow/mutual/check/${userId}`)
}

// 聊天相关接口
export const chatApi = {
  getConversations: () => request('get', '/chat/conversations'),
  getMessages: (conversationId: number, params?: { page?: number; size?: number }) =>
    request('get', `/chat/messages/${conversationId}`, undefined, { params }),
  markRead: (conversationId: number) => request('post', `/chat/read/${conversationId}`),
  getUnread: () => request('get', '/chat/unread'),
  getContacts: () => request('get', '/chat/contacts'),
  sendMessage: (data: { receiverId: number; content: string; messageType?: string }) =>
    request('post', '/chat/send', data),
  checkPermission: (receiverId: number) => request('get', `/chat/permission/${receiverId}`),
  clearConversation: (conversationId: number) => request('delete', `/chat/conversation/${conversationId}`),
  getOrCreateConversationWith: (otherUserId: number) =>
    request('get', `/chat/conversation/with/${otherUserId}`),
  deleteChatFile: (fileId: number) => request('delete', `/chat/file/${fileId}`),
  deleteChatPhoto: (photoId: number) => request('delete', `/chat/photo/${photoId}`),
  deleteMessage: (messageId: number) => request('delete', `/chat/message/${messageId}`)
}

// 分享相关接口
export const shareApi = {
  createShare: (sessionId: string) => request('post', '/share/create', { sessionId }),
  getShareDetail: (shareToken: string) => request('get', `/share/detail/${shareToken}`),
  checkShare: (sessionId: string) => request('get', `/share/check/${sessionId}`)
}

// 匹配相关接口
export const matchApi = {
  getRandomMatch: (excludeIds?: number[]) =>
    request('get', '/bloom/random-match', undefined, {
      params: excludeIds?.length ? { excludeIds } : {}
    })
}

export default api

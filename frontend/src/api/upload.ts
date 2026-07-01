// 上传相关接口
import { request } from './http'

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

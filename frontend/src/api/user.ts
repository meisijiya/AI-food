// 用户相关接口
import { request } from './http'

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

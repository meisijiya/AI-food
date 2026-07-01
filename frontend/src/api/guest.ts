// 游客相关接口
import { request } from './http'

export const guestApi = {
  getGuestInfo: () => request('get', '/guest/info'),
  getGuestStats: () => request('get', '/guest/stats')
}

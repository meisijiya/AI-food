// 通知相关接口
import { request } from './http'

export const notificationApi = {
  getList: (params?: { page?: number; size?: number }) =>
    request('get', '/notification/list', undefined, { params }),
  getUnread: () => request('get', '/notification/unread'),
  deleteOne: (notificationId: string) => request('delete', `/notification/${notificationId}`),
  clearAll: () => request('delete', '/notification/clear-all')
}

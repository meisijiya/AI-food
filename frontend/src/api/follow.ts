// 关注相关接口
import { request } from './http'

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

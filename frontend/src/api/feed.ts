// 大厅相关接口
import { request } from './http'

export const feedApi = {
  publish: (data: { sessionId: string; commentPreview?: string; visibility?: string }) =>
    request('post', '/feed/publish', data),
  getList: (params?: { page?: number; size?: number; foodName?: string; paramName?: string; paramValue?: string }) =>
    request('get', '/feed/list', undefined, { params }),
  getDetail: (postId: number) => request('get', `/feed/detail/${postId}`),
  toggleLike: (postId: number) => request('post', `/feed/like/${postId}`),
  addComment: (postId: number, content: string, imageUrl?: string) =>
    request('post', `/feed/comment/${postId}`, { content, imageUrl }),
  deleteComment: (commentId: number) => request('delete', `/feed/comment/${commentId}`),
  getComments: (postId: number, params?: { page?: number; size?: number }) =>
    request('get', `/feed/comments/${postId}`, undefined, { params }),
  checkPublished: (sessionId: string) => request('get', `/feed/check/${sessionId}`),
  unpublish: (sessionId: string) => request('delete', `/feed/unpublish/${sessionId}`),
  getHotRank: () => request('get', '/feed/hot-rank'),
  getFriendFeed: (params?: { page?: number; size?: number }) =>
    request('get', '/feed/friend-feed', undefined, { params })
}

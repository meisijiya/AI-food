// 记录相关接口
import { request } from './http'

export const recordApi = {
  getRecordList: (params?: { page?: number; size?: number; sort?: string }) =>
    request('get', '/record/list', undefined, { params }),
  getRecordDetail: (sessionId: string) => request('get', `/record/detail/${sessionId}`),
  deleteRecord: (sessionId: string) => request('delete', `/record/delete/${sessionId}`),
  batchDeleteRecords: (sessionIds: string[]) => request('delete', '/record/batch-delete', { sessionIds }),
  updatePhoto: (sessionId: string, photoUrl: string) =>
    request('put', `/record/photo/${sessionId}`, { photoUrl }),
  deletePhoto: (sessionId: string) => request('delete', `/record/photo/${sessionId}`),
  updateComment: (sessionId: string, comment: string) =>
    request('put', `/record/comment/${sessionId}`, { comment }),
  getPendingRecommendation: () => request('get', '/record/pending')
}

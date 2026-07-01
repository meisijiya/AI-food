// 分享相关接口
import { request } from './http'

export const shareApi = {
  createShare: (sessionId: string) => request('post', '/share/create', { sessionId }),
  getShareDetail: (shareToken: string) => request('get', `/share/detail/${shareToken}`),
  checkShare: (sessionId: string) => request('get', `/share/check/${sessionId}`)
}

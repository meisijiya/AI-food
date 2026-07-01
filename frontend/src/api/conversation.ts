// 对话相关接口
import { request } from './http'

export const conversationApi = {
  start: () => request('post', '/conversation/start'),
  getStatus: (sessionId: string) => request('get', `/conversation/status/${sessionId}`),
  complete: (sessionId: string) => request('post', `/conversation/complete/${sessionId}`),
  cancel: (sessionId: string) => request('delete', `/conversation/cancel/${sessionId}`),
  getHistory: (sessionId: string) => request('get', `/conversation/history/${sessionId}`)
}

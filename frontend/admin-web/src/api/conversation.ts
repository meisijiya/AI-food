import request from './request'

export function listConversations(params: any) {
  return request.get('/conversations', { params })
}

export function getConversationDetail(id: number) {
  return request.get(`/conversations/${id}`)
}

export function getConversationMessages(id: number, params: any) {
  return request.get(`/conversations/${id}/messages`, { params })
}

export function deleteConversation(id: number) {
  return request.delete(`/conversations/${id}`)
}
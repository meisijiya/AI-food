// 聊天相关接口
import { request } from './http'

export const chatApi = {
  getConversations: () => request('get', '/chat/conversations'),
  getMessages: (conversationId: number, params?: { page?: number; size?: number }) =>
    request('get', `/chat/messages/${conversationId}`, undefined, { params }),
  markRead: (conversationId: number) => request('post', `/chat/read/${conversationId}`),
  getUnread: () => request('get', '/chat/unread'),
  getContacts: () => request('get', '/chat/contacts'),
  sendMessage: (data: { receiverId: number; content: string; messageType?: string }) =>
    request('post', '/chat/send', data),
  checkPermission: (receiverId: number) => request('get', `/chat/permission/${receiverId}`),
  clearConversation: (conversationId: number) => request('delete', `/chat/conversation/${conversationId}`),
  getOrCreateConversationWith: (otherUserId: number) =>
    request('get', `/chat/conversation/with/${otherUserId}`),
  deleteChatFile: (fileId: number) => request('delete', `/chat/file/${fileId}`),
  deleteChatPhoto: (photoId: number) => request('delete', `/chat/photo/${photoId}`),
  deleteMessage: (messageId: number) => request('delete', `/chat/message/${messageId}`)
}

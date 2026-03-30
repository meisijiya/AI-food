// 聊天消息类型
export interface ChatMessage {
  id: number
  conversationId: number
  senderId: number
  receiverId: number
  content: string
  messageType: 'text' | 'image' | 'file'
  photoId?: number
  fileId?: number
  isRead: boolean
  createdAt: string
}

// 图片消息内容
export interface ImageContent {
  thumbnailUrl: string
  originalUrl: string
  fileName: string
}

// 文件消息内容
export interface FileContent {
  fileUrl: string
  fileName: string
  fileSize: string | number
}

// 消息缓存结构
export interface CachedMessages {
  data: ChatMessage[]
  timestamp: number
}

// 发送消息请求
export interface SendMessageRequest {
  receiverId: number
  content: string
  messageType?: string
}

// 权限检查响应
export interface PermissionCheckResponse {
  permission: 'ok' | 'max_reached' | 'not_allowed'
  remaining: number
}

// 会话列表项
export interface ConversationItem {
  conversationId: number
  userId: number
  nickname: string
  avatar?: string
  lastMessage?: string
  lastMessageAt?: string
  unreadCount: number
}

// 创建会话响应
export interface CreateConversationResponse {
  conversationId: number
  userId: number
  nickname?: string
  avatar?: string
}

// 消息分页响应
export interface MessagesPageResponse {
  items: ChatMessage[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

// WebSocket 消息类型
export interface WebSocketMessage {
  type: string
  [key: string]: any
}

// re-export hub — 保持向后兼容,所有现存调用方
// (import { xxxApi } from '@/api') 无需改动
export { authApi } from './auth'
export { guestApi } from './guest'
export { userApi } from './user'
export { recordApi } from './record'
export { uploadApi } from './upload'
export { conversationApi } from './conversation'
export { feedApi } from './feed'
export { notificationApi } from './notification'
export { followApi } from './follow'
export { chatApi } from './chat'
export { shareApi } from './share'
export { matchApi } from './match'

// 暴露 http 层基础设施 — 供少数高级用例使用
export { api, bareApi, request } from './http'

import { api } from './http'
export default api

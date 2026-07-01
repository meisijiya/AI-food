// Feed / 大厅 相关类型定义
// 字段命名与后端 FeedController 返回的 JSON 一致,见 backend/ai-food-app FeedController

/** 大厅 Tab 单个帖子(post-card 渲染) */
export interface FeedPost {
  id: number
  thumbnailUrl?: string
  foodName: string
  commentPreview?: string
  avatar?: string
  nickname?: string
  likeCount?: number
  visibility?: 'public' | 'friends'
}

/** 热榜 Tab 单个条目 */
export interface HotRankItem {
  id: number
  thumbnailUrl?: string
  foodName: string
  visibility?: 'public' | 'friends'
  nickname?: string
  hotScore: number
}

/** 好友动态 Tab 单个条目 */
export interface FriendFeedItem {
  postId: number
  avatar?: string
  nickname?: string
  publishedAt: string
  foodName: string
  visibility?: 'public' | 'friends'
  thumbnailUrl?: string
}

/** 筛选表单(食物名 + 参数名 + 参数值) */
export interface FilterForm {
  foodName: string
  paramName: string
  paramValue: string
}

// Result.vue 相关类型定义

/** 上传的照片(缩略图 + 原始图) */
export interface Photo {
  thumbnailUrl: string
  originalUrl: string
}

/** 食物卡片数据 */
export interface FoodCardData {
  foodName: string
  reason: string | null
}

/** 收集到的参数(展示用 chip) */
export interface CollectedParam {
  name: string
  label: string
  value: string
}

/** recordApi.getPendingRecommendation() 的返回结构(P1 nice-to-have) */
export interface PendingRecommendation {
  hasPending?: boolean
  sessionId?: string
  content?: unknown
  /** 收集到的参数(7 参数:time/location/weather/mood/companion/budget/taste) */
  paramValues?: Record<string, string>
}
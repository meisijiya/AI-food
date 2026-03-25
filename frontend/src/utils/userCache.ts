/**
 * 用户信息缓存（头像、昵称等）
 * 缓存有效期：30分钟 + 随机 0~5分钟 抖动
 */

const BASE_EXPIRE_MS = 30 * 60 * 1000
const JITTER_MAX_MS = 5 * 60 * 1000

interface CachedUserInfo {
  userId: number
  nickname: string
  avatar: string | null
  expiry: number
}

function getCacheKey(userId: number) {
  return `userCache:${userId}`
}

function randomExpiry(): number {
  return Date.now() + BASE_EXPIRE_MS + Math.floor(Math.random() * JITTER_MAX_MS)
}

export function getCachedUser(userId: number): { nickname: string; avatar: string | null } | null {
  try {
    const raw = localStorage.getItem(getCacheKey(userId))
    if (!raw) return null
    const data: CachedUserInfo = JSON.parse(raw)
    if (Date.now() > data.expiry) {
      localStorage.removeItem(getCacheKey(userId))
      return null
    }
    return { nickname: data.nickname, avatar: data.avatar }
  } catch {
    return null
  }
}

export function setCachedUser(userId: number, nickname: string, avatar: string | null) {
  try {
    const data: CachedUserInfo = { userId, nickname, avatar, expiry: randomExpiry() }
    localStorage.setItem(getCacheKey(userId), JSON.stringify(data))
  } catch { /* quota exceeded */ }
}

/**
 * 从对话列表中批量缓存用户信息
 */
export function cacheUsersFromList(conversations: Array<{ userId: number; nickname: string; avatar: string | null }>) {
  for (const conv of conversations) {
    if (conv.userId && conv.nickname) {
      setCachedUser(conv.userId, conv.nickname, conv.avatar)
    }
  }
}

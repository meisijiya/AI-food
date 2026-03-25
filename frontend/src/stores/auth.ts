import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export interface UserInfo {
  userId: number
  username: string
  nickname: string
  email: string
  avatar: string | null
}

// 安全读取 localStorage，防止 "undefined" / "null" 字符串污染
function safeGetItem(key: string): string | null {
  const val = localStorage.getItem(key)
  if (!val || val === 'undefined' || val === 'null') return null
  return val
}

function safeParseItem<T>(key: string): T | null {
  const raw = safeGetItem(key)
  if (!raw) return null
  try {
    return JSON.parse(raw)
  } catch {
    localStorage.removeItem(key)
    return null
  }
}

const CACHE_KEY = 'userInfo'
const EXPIRY_KEY = 'userInfoExpiry'
const BASE_EXPIRE_MS = 30 * 60 * 1000 // 30 分钟
const JITTER_MAX_MS = 5 * 60 * 1000   // 随机 0~5 分钟

function isCacheExpired(): boolean {
  const expiry = safeGetItem(EXPIRY_KEY)
  if (!expiry) return true
  return Date.now() > Number(expiry)
}

function setCacheExpiry() {
  const jitter = Math.floor(Math.random() * JITTER_MAX_MS)
  const expiry = Date.now() + BASE_EXPIRE_MS + jitter
  localStorage.setItem(EXPIRY_KEY, String(expiry))
}

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string>(safeGetItem('token') || '')
  const userInfo = ref<UserInfo | null>(
    isCacheExpired() ? null : safeParseItem<UserInfo>(CACHE_KEY)
  )

  const isLoggedIn = computed(() => !!token.value)
  const isCacheValid = computed(() => !isCacheExpired() && !!userInfo.value)

  function setToken(newToken: string) {
    token.value = newToken
    localStorage.setItem('token', newToken)
  }

  function setUserInfo(info: UserInfo) {
    userInfo.value = info
    localStorage.setItem(CACHE_KEY, JSON.stringify(info))
    setCacheExpiry()
  }

  async function logout() {
    // 先通知后端删除 Redis token
    try {
      const { authApi } = await import('@/api')
      await authApi.logout()
    } catch {
      // 忽略网络错误
    }
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('token')
    localStorage.removeItem(CACHE_KEY)
    localStorage.removeItem(EXPIRY_KEY)
  }

  // 清除所有可能的脏数据
  function clearStale() {
    const t = safeGetItem('token')
    if (!t) {
      token.value = ''
      localStorage.removeItem('token')
    }
    if (isCacheExpired()) {
      userInfo.value = null
      localStorage.removeItem(CACHE_KEY)
      localStorage.removeItem(EXPIRY_KEY)
    }
  }

  return { token, userInfo, isLoggedIn, isCacheValid, setToken, setUserInfo, logout, clearStale }
})

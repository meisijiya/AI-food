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

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string>(safeGetItem('token') || '')
  const userInfo = ref<UserInfo | null>(safeParseItem<UserInfo>(CACHE_KEY))

  const isLoggedIn = computed(() => !!token.value)
  const isGuest = computed(() => !token.value)

  function setToken(newToken: string) {
    token.value = newToken
    localStorage.setItem('token', newToken)
    document.cookie = `auth_token=${newToken}; path=/; max-age=604800; SameSite=Strict`
  }

  function setUserInfo(info: UserInfo) {
    userInfo.value = info
    localStorage.setItem(CACHE_KEY, JSON.stringify(info))
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
    document.cookie = 'auth_token=; path=/; max-age=0'
  }

  // 清除过期的 token
  function clearStale() {
    const t = safeGetItem('token')
    if (!t) {
      token.value = ''
      localStorage.removeItem('token')
      userInfo.value = null
      localStorage.removeItem(CACHE_KEY)
    }
  }

  return { token, userInfo, isLoggedIn, isGuest, setToken, setUserInfo, logout, clearStale }
})

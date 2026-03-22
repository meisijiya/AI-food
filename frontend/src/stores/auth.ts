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

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string>(safeGetItem('token') || '')
  const userInfo = ref<UserInfo | null>(safeParseItem<UserInfo>('userInfo'))

  const isLoggedIn = computed(() => !!token.value)

  function setToken(newToken: string) {
    token.value = newToken
    localStorage.setItem('token', newToken)
  }

  function setUserInfo(info: UserInfo) {
    userInfo.value = info
    localStorage.setItem('userInfo', JSON.stringify(info))
  }

  function logout() {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
  }

  // 清除所有可能的脏数据
  function clearStale() {
    const t = safeGetItem('token')
    const u = safeGetItem('userInfo')
    if (!t) {
      token.value = ''
      localStorage.removeItem('token')
    }
    if (!u) {
      userInfo.value = null
      localStorage.removeItem('userInfo')
    }
  }

  return { token, userInfo, isLoggedIn, setToken, setUserInfo, logout, clearStale }
})

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
    // 安全修复（M2）：不再写入 localStorage / document.cookie，避免 XSS 一把抓走 token
    // token 仅保留在内存中（Pinia state），用于本次会话内的 Authorization header 和 WebSocket 子协议
    // 持久化由后端 HttpOnly cookie 承担（前端 JS 无法读取，但浏览器自动随请求带上）
    token.value = newToken
  }

  function setUserInfo(info: UserInfo) {
    userInfo.value = info
    localStorage.setItem(CACHE_KEY, JSON.stringify(info))
  }

  async function logout() {
    // 清本地状态优先,再 best-effort 通知后端 — 这样 /auth/logout 不会带着
    // 失效的 Authorization 头出发;即便被 interceptor 抓到 401,新的 URL guard
    // 也不会再触发 logout,死循环被打断(详情见 api/index.ts 拦截器注释)
    token.value = ''
    userInfo.value = null
    // 安全修复（M2）：不再清理 localStorage['token'] / document.cookie（写入已被禁用）
    localStorage.removeItem(CACHE_KEY)
    try {
      const { authApi } = await import('@/api')
      await authApi.logout()
    } catch {
      // 忽略网络错误 — 本地状态已干净
    }
  }

  // 清除过期的 token
  function clearStale() {
    // 安全修复（M2）：不再从 localStorage 读取 token；内存中的 token 由 setToken / logout 管理
    if (!token.value) {
      userInfo.value = null
      localStorage.removeItem(CACHE_KEY)
    }
  }

  return { token, userInfo, isLoggedIn, isGuest, setToken, setUserInfo, logout, clearStale }
})

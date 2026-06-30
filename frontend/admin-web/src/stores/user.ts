// 管理员用户 store：token + 用户信息，持久化到 localStorage
import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useUserStore = defineStore('user', () => {
  // ponytail: any 类型占位 — 后端 admin-user 模型待 Task 21+ 定义
  const token = ref<string>(localStorage.getItem('admin_token') || '')
  const adminUser = ref<any>(JSON.parse(localStorage.getItem('admin_user') || 'null'))

  // 登录：写入 token + 用户信息并同步 localStorage
  function setLogin(t: string, user: any) {
    token.value = t
    adminUser.value = user
    localStorage.setItem('admin_token', t)
    localStorage.setItem('admin_user', JSON.stringify(user))
  }

  // 登出：清空 store + localStorage
  function logout() {
    token.value = ''
    adminUser.value = null
    localStorage.removeItem('admin_token')
    localStorage.removeItem('admin_user')
  }

  return { token, adminUser, setLogin, logout }
})
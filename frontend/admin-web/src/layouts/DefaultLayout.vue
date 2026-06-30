<script setup lang="ts">
// ponytail: 占位 Layout — 后续 Task 21+ 接入侧边栏 + 头部 + 菜单
import { useUserStore } from '@/stores/user'
import { useRouter } from 'vue-router'

const user = useUserStore()
const router = useRouter()

// 退出登录：清 store 并跳登录页
function handleLogout() {
  user.logout()
  router.push('/login')
}
</script>

<template>
  <div class="layout">
    <header class="layout-header">
      <span class="brand">AI-Food Admin</span>
      <div class="user-area">
        <span>{{ user.adminUser?.username || 'admin' }}</span>
        <el-button link type="primary" @click="handleLogout">退出</el-button>
      </div>
    </header>
    <main class="layout-main">
      <router-view />
    </main>
  </div>
</template>

<style scoped>
.layout { display: flex; flex-direction: column; height: 100vh; }
.layout-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 0 16px; height: 48px; background: #001529; color: #fff;
}
.layout-header .brand { font-weight: 600; }
.layout-header .user-area { display: flex; align-items: center; gap: 12px; }
.layout-main { flex: 1; padding: 16px; overflow: auto; background: #f5f5f5; }
</style>
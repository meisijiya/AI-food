<script setup lang="ts">
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const menus = [
  { path: '/dashboard', title: 'Dashboard', icon: 'DataLine' },
  { path: '/user', title: '用户管理', icon: 'User' },
  { path: '/conversation', title: 'AI 对话', icon: 'ChatDotRound' },
  { path: '/token-usage', title: 'Token 用量', icon: 'Coin' },
  { path: '/model', title: '模型管理', icon: 'Cpu' },
  { path: '/recommendation', title: '推荐记录', icon: 'List' },
  { path: '/monitor', title: '系统监控', icon: 'Monitor' },
  { path: '/audit-log', title: '操作日志', icon: 'Document' }
]

function onLogout() {
  userStore.logout()
  router.push('/login')
}
</script>

<template>
  <el-container class="layout">
    <el-aside width="220px" class="sidebar">
      <div class="logo">🍜 AI-Food Admin</div>
      <el-menu :default-active="route.path" router>
        <el-menu-item v-for="m in menus" :key="m.path" :index="m.path">
          <el-icon><component :is="m.icon" /></el-icon>
          <span>{{ m.title }}</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <div class="header-title">{{ route.meta.title || 'Admin' }}</div>
        <el-dropdown>
          <span class="user-info">
            <el-avatar :size="32" :src="userStore.adminUser?.avatar" />
            {{ userStore.adminUser?.nickname || userStore.adminUser?.username }}
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="onLogout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>
      <el-main><router-view /></el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.layout { height: 100vh; }
.sidebar { background: #001529; }
.logo {
  height: 60px;
  line-height: 60px;
  text-align: center;
  font-size: 18px;
  font-weight: bold;
  color: white;
  background: #002140;
}
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: white;
  border-bottom: 1px solid #e6e6e6;
  padding: 0 20px;
}
.user-info {
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 8px;
}
</style>
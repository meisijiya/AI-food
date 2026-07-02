<script setup lang="ts">
/**
 * AI-Food Admin — 默认布局
 * 220px 浅色侧栏 + 56px 白色顶栏(breadcrumb + user)+ 灰色主区
 * 保留:e2e 菜单 text 选择器("用户管理"/"AI 对话"/...)与路由 path
 */
import { useRouter, useRoute } from 'vue-router'
import { ArrowDown } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

// ponytail: 菜单元数据保持内联 — 8 项不会增长,提一层 indirection 不值
const menus = [
  { path: '/dashboard',     title: 'Dashboard',  icon: 'DataLine' },
  { path: '/user',          title: '用户管理',   icon: 'User' },
  { path: '/conversation',  title: 'AI 对话',    icon: 'ChatDotRound' },
  { path: '/token-usage',   title: 'Token 用量', icon: 'Coin' },
  { path: '/model',         title: '模型管理',   icon: 'Cpu' },
  { path: '/recommendation',title: '推荐记录',   icon: 'List' },
  { path: '/monitor',       title: '系统监控',   icon: 'Monitor' },
  { path: '/audit-log',     title: '操作日志',   icon: 'Document' }
]

/** 退出登录并跳到 /login */
function onLogout(): void {
  userStore.logout()
  router.push('/login')
}
</script>

<template>
  <el-container class="layout">
    <el-aside width="220px" class="sidebar">
      <div class="logo">
        <span class="logo-emoji">🍜</span>
        <span class="logo-text">AI-Food Admin</span>
      </div>
      <el-menu
        class="sidebar-menu"
        :default-active="route.path" router
        background-color="transparent"
        text-color="#4b5563"
        active-text-color="var(--color-primary)">
        <el-menu-item v-for="m in menus" :key="m.path" :index="m.path">
          <el-icon><component :is="m.icon" /></el-icon>
          <span>{{ m.title }}</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <el-breadcrumb separator="/" class="breadcrumb">
          <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
          <el-breadcrumb-item>{{ route.meta.title || 'Admin' }}</el-breadcrumb-item>
        </el-breadcrumb>
        <el-dropdown>
          <span class="user-info">
            <el-avatar :size="32" :src="userStore.adminUser?.avatar" />
            <span class="user-name">{{ userStore.adminUser?.nickname || userStore.adminUser?.username }}</span>
            <el-icon class="user-caret"><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="onLogout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>
      <el-main class="main"><router-view /></el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.layout { height: 100vh; }

/* === Sidebar === */
.sidebar {
  background: var(--color-bg-subtle);
  border-right: 1px solid var(--color-border);
}
.logo {
  height: 60px; display: flex; align-items: center; justify-content: center;
  gap: 8px; background: var(--color-bg-surface);
  border-bottom: 1px solid var(--color-border);
  font-size: 16px; font-weight: 600; color: var(--color-text-primary);
}
.logo-emoji { font-size: 22px; }
.sidebar-menu { border-right: none; padding-top: var(--space-sm); }
.sidebar-menu :deep(.el-menu-item) {
  height: 44px; margin: 2px 12px; border-radius: var(--radius-md);
}
.sidebar-menu :deep(.el-menu-item.is-active) {
  background: var(--color-primary-soft);
  color: var(--color-primary) !important; font-weight: 600;
}

/* === Header === */
.header {
  display: flex; justify-content: space-between; align-items: center;
  background: var(--color-bg-surface);
  border-bottom: 1px solid var(--color-border);
  padding: 0 var(--space-lg); height: 56px;
}
.breadcrumb { font-size: var(--font-base); }
.breadcrumb :deep(.el-breadcrumb__inner) {
  color: var(--color-text-secondary); font-weight: normal;
}
.breadcrumb :deep(.el-breadcrumb__item:last-child .el-breadcrumb__inner) {
  color: var(--color-text-primary); font-weight: 600;
}
.user-info {
  cursor: pointer; display: flex; align-items: center; gap: 8px;
  padding: 4px 8px; border-radius: var(--radius-md);
  transition: background 0.18s ease;
}
.user-info:hover { background: var(--color-bg-subtle); }
.user-name { font-size: var(--font-base); color: var(--color-text-primary); }
.user-caret { color: var(--color-text-tertiary); font-size: 12px; }

/* === Main === */
.main { background: var(--color-bg-app); padding: 0; }
</style>
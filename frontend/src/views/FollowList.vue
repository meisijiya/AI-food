<template>
  <div class="follow-container">
    <div class="bg-glow bg-glow-1"></div>

    <!-- Header -->
    <div class="page-header animate-fade-up">
      <button class="back-btn" @click="router.back()">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m15 18-6-6 6-6"/></svg>
      </button>
      <h1 class="page-title">{{ activeTab === 'following' ? '关注' : '粉丝' }}</h1>
      <div class="header-placeholder"></div>
    </div>

    <!-- Tab Bar -->
    <div class="tab-bar animate-fade-up delay-100">
      <button 
        class="tab-item" 
        :class="{ active: activeTab === 'following' }"
        @click="switchTab('following')"
      >
        关注 {{ followStats.followingCount }}
      </button>
      <button 
        class="tab-item" 
        :class="{ active: activeTab === 'followers' }"
        @click="switchTab('followers')"
      >
        粉丝 {{ followStats.followerCount }}
      </button>
    </div>

    <!-- User List -->
    <div class="user-list">
      <div 
        v-for="(user, index) in userList" 
        :key="user.userId" 
        class="user-item animate-fade-up"
        :style="{ animationDelay: (index % 10) * 0.05 + 's' }"
      >
        <div class="user-avatar" @click="viewUserPosts(user.userId)">
          <img v-if="user.avatar" :src="user.avatar" alt="" />
          <span v-else>{{ user.nickname?.charAt(0) || '?' }}</span>
        </div>
        <div class="user-info" @click="viewUserPosts(user.userId)">
          <div class="user-name">{{ user.nickname }}</div>
        </div>
        <button 
          v-if="user.userId !== currentUserId"
          class="follow-btn"
          :class="{ following: user.isFollowing }"
          @click="handleToggleFollow(user)"
        >
          {{ user.isFollowing ? '已关注' : '关注' }}
        </button>
      </div>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="loading-more">
      <div class="spinner"></div>
    </div>

    <!-- Empty -->
    <div v-if="!loading && userList.length === 0" class="empty-state">
      <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" class="empty-icon"><path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M22 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>
      <div class="empty-text">{{ activeTab === 'following' ? '暂无关注' : '暂无粉丝' }}</div>
      <div class="empty-hint">{{ activeTab === 'following' ? '去发现有趣的人吧' : '发布内容吸引更多粉丝' }}</div>
    </div>

    <div class="nav-spacer"></div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { followApi } from '@/api'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const currentUserId = computed(() => authStore.userInfo?.userId)
const activeTab = ref<string>((route.query.type as string) || 'following')
const loading = ref(false)
const userList = ref<any[]>([])
const followStats = reactive({
  followingCount: 0,
  followerCount: 0
})

async function fetchStats() {
  try {
    const res = await followApi.getMyFollowStats()
    if (res) {
      followStats.followingCount = res.followingCount || 0
      followStats.followerCount = res.followerCount || 0
    }
  } catch { /* ignore */ }
}

async function fetchList() {
  loading.value = true
  userList.value = []
  try {
    const res = activeTab.value === 'following'
      ? await followApi.getFollowingList({ page: 0, size: 100 })
      : await followApi.getFollowersList({ page: 0, size: 100 })
    userList.value = res?.items || []
  } catch { /* ignore */ }
  finally { loading.value = false }
}

function switchTab(tab: string) {
  activeTab.value = tab
  fetchList()
}

async function handleToggleFollow(user: any) {
  try {
    const res = await followApi.toggleFollow(user.userId)
    if (res) {
      user.isFollowing = res.isFollowing
      // Refresh stats
      await fetchStats()
    }
  } catch { /* ignore */ }
}

function viewUserPosts(_userId: number) {
  // Could navigate to user's posts page
  // For now, just go back to feed
}

onMounted(() => {
  fetchStats()
  fetchList()
})
</script>

<style lang="scss" scoped>
.follow-container {
  min-height: 100vh;
  background: var(--color-surface);
  padding: 40px 12px 100px;
  position: relative;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
}

.bg-glow {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  pointer-events: none;
}

.bg-glow-1 {
  top: -60px;
  right: -40px;
  width: 240px;
  height: 240px;
  background: var(--color-primary-container);
  opacity: 0.1;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  z-index: 1;
  position: relative;
}

.back-btn {
  width: 36px;
  height: 36px;
  border: none;
  border-radius: 50%;
  background: none;
  color: var(--color-on-surface);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  &:active { background: var(--color-surface-container-low); }
}

.page-title {
  font-family: var(--font-serif);
  font-style: italic;
  font-size: 24px;
  font-weight: 400;
  color: var(--color-on-surface);
  em { font-style: italic; color: var(--color-primary); }
}

.header-placeholder {
  width: 36px;
}

.tab-bar {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
  z-index: 1;
  position: relative;
}

.tab-item {
  flex: 1;
  padding: 10px 16px;
  border: 1.5px solid var(--color-surface-container-low);
  border-radius: 100px;
  background: none;
  color: var(--color-on-surface-variant);
  font-family: var(--font-sans);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  
  &.active {
    background: linear-gradient(135deg, var(--color-primary-container), var(--color-primary));
    border-color: transparent;
    color: white;
  }
  
  &:not(.active):active {
    background: var(--color-surface-container-low);
  }
}

.user-list {
  z-index: 1;
  position: relative;
}

.user-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: var(--color-surface-container-lowest);
  border-radius: 1.25rem;
  border: 1px solid rgba(255, 255, 255, 0.8);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  margin-bottom: 8px;
}

.user-avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--color-primary-container), var(--color-primary));
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-family: var(--font-serif);
  font-style: italic;
  font-size: 18px;
  font-weight: 400;
  flex-shrink: 0;
  overflow: hidden;
  cursor: pointer;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}

.user-info {
  flex: 1;
  min-width: 0;
  cursor: pointer;
}

.user-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-on-surface);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.follow-btn {
  padding: 6px 16px;
  border: 1.5px solid var(--color-primary);
  border-radius: 100px;
  background: none;
  color: var(--color-primary);
  font-family: var(--font-sans);
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  flex-shrink: 0;

  &:active {
    transform: scale(0.95);
  }

  &.following {
    background: var(--color-surface-container-low);
    border-color: var(--color-surface-container-low);
    color: var(--color-on-surface-variant);
  }
}

.loading-more {
  display: flex;
  justify-content: center;
  padding: 20px 0;
  z-index: 1;
  position: relative;
}

.spinner {
  width: 24px;
  height: 24px;
  border: 3px solid var(--color-surface-container-low);
  border-top-color: var(--color-primary);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin { to { transform: rotate(360deg); } }

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 60px 0;
  z-index: 1;
  position: relative;
}

.empty-icon { color: var(--color-on-surface-variant); opacity: 0.3; margin-bottom: 16px; }
.empty-text { font-size: 15px; font-weight: 600; color: var(--color-on-surface-variant); margin-bottom: 6px; }
.empty-hint { font-size: 12px; color: var(--color-on-surface-variant); opacity: 0.6; }

.nav-spacer { height: 80px; }
</style>

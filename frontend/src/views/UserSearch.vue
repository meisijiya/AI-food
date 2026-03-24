<template>
  <div class="search-container" ref="scrollContainer" @scroll="onScroll">
    <div class="bg-glow bg-glow-1"></div>

    <!-- Header -->
    <div class="page-header animate-fade-up">
      <button class="back-btn" @click="router.back()">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m15 18-6-6 6-6"/></svg>
      </button>
      <h1 class="page-title"><em>搜索用户</em></h1>
      <div class="header-placeholder"></div>
    </div>

    <!-- Search Input -->
    <div class="search-bar animate-fade-up delay-100">
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="search-icon"><circle cx="11" cy="11" r="8"/><path d="m21 21-4.3-4.3"/></svg>
      <input
        v-model="keyword"
        class="search-input"
        placeholder="搜索昵称或用户名..."
        @input="onSearchInput"
      />
      <button v-if="keyword" class="search-clear" @click="keyword = ''; onSearchInput()">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><path d="m15 9-6 6"/><path d="m9 9 6 6"/></svg>
      </button>
    </div>

    <!-- User List -->
    <div class="user-list">
      <div
        v-for="(user, index) in users"
        :key="user.userId"
        class="user-item animate-fade-up"
        :style="{ animationDelay: (index % 10) * 0.05 + 's' }"
      >
        <div class="user-avatar">
          <img v-if="user.avatar" :src="user.avatar" alt="" />
          <span v-else>{{ user.nickname?.charAt(0) || '?' }}</span>
        </div>
        <div class="user-info">
          <div class="user-name">{{ user.nickname }}</div>
          <div class="sign-badge">
            <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M12 2v20M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"/></svg>
            连续签到 <span class="sign-days">{{ user.continuousDays }}</span> 天
          </div>
        </div>
        <button class="user-chat-btn" @click="openChatWith(user)">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>
        </button>
        <button
          class="follow-btn"
          :class="{ following: followMap[user.userId] }"
          @click="handleToggleFollow(user)"
        >
          {{ followMap[user.userId] ? '已关注' : '关注' }}
        </button>
      </div>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="loading-more">
      <div class="spinner"></div>
    </div>

    <!-- Empty -->
    <div v-if="!loading && searched && users.length === 0" class="empty-state">
      <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" class="empty-icon"><circle cx="11" cy="11" r="8"/><path d="m21 21-4.3-4.3"/></svg>
      <div class="empty-text">未找到相关用户</div>
      <div class="empty-hint">换个关键词试试</div>
    </div>

    <!-- Init state -->
    <div v-if="!loading && !searched && users.length === 0" class="empty-state">
      <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" class="empty-icon"><circle cx="11" cy="11" r="8"/><path d="m21 21-4.3-4.3"/></svg>
      <div class="empty-text">搜索用户</div>
      <div class="empty-hint">输入昵称或用户名开始搜索</div>
    </div>

    <div class="nav-spacer"></div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { userApi, followApi } from '@/api'

const router = useRouter()
const scrollContainer = ref<HTMLElement>()

const keyword = ref('')
const users = ref<any[]>([])
const page = ref(0)
const pageSize = 10
const loading = ref(false)
const finished = ref(false)
const searched = ref(false)
const followMap = ref<Record<number, boolean>>({})

let searchTimer: ReturnType<typeof setTimeout> | null = null

function onSearchInput() {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    fetchUsers(true)
  }, 300)
}

function onScroll() {
  const el = scrollContainer.value
  if (!el || loading.value || finished.value || !keyword.value.trim()) return
  if (el.scrollTop + el.clientHeight >= el.scrollHeight - 100) {
    fetchUsers(false)
  }
}

async function fetchUsers(reset = false) {
  const kw = keyword.value.trim()
  if (!kw) {
    users.value = []
    searched.value = false
    return
  }
  if (loading.value) return
  loading.value = true
  if (reset) { page.value = 0; finished.value = false }

  try {
    const res = await userApi.searchUsers({ keyword: kw, page: page.value, size: pageSize })
    const list = res?.items || []
    searched.value = true

    if (reset) {
      users.value = list
    } else {
      users.value.push(...list)
    }

    // Check follow status for each user
    for (const user of list) {
      if (followMap.value[user.userId] === undefined) {
        try {
          const check = await followApi.checkFollow(user.userId)
          followMap.value[user.userId] = check?.isFollowing || false
        } catch {
          followMap.value[user.userId] = false
        }
      }
    }

    if (list.length < pageSize) {
      finished.value = true
    } else {
      page.value++
    }
  } catch {
    // ignore
  } finally {
    loading.value = false
  }
}

function openChatWith(user: any) {
  localStorage.setItem('lastChatPartner', JSON.stringify({
    userId: user.userId,
    conversationId: user.conversationId,
    nickname: user.nickname,
    avatar: user.avatar
  }))
  router.push({
    path: '/chat-room',
    query: { userId: user.userId, nickname: user.nickname }
  })
}

async function handleToggleFollow(user: any) {
  try {
    const res = await followApi.toggleFollow(user.userId)
    if (res) {
      followMap.value[user.userId] = res.isFollowing
    }
  } catch {
    // ignore
  }
}

onMounted(() => {
  // Focus search input
})
</script>

<style lang="scss" scoped>
.search-container {
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

/* Search Bar */
.search-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  background: var(--color-surface-container-lowest);
  border-radius: 100px;
  border: 1.5px solid var(--color-surface-container-low);
  margin-bottom: 16px;
  z-index: 1;
  position: relative;
  transition: border-color 0.2s;
  &:focus-within { border-color: var(--color-primary); }
}

.search-icon {
  color: var(--color-on-surface-variant);
  opacity: 0.5;
  flex-shrink: 0;
}

.search-input {
  flex: 1;
  border: none;
  background: none;
  font-family: var(--font-sans);
  font-size: 14px;
  color: var(--color-on-surface);
  outline: none;
  &::placeholder { color: var(--color-on-surface-variant); opacity: 0.5; }
}

.search-clear {
  width: 24px;
  height: 24px;
  border: none;
  border-radius: 50%;
  background: var(--color-surface-container-low);
  color: var(--color-on-surface-variant);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  flex-shrink: 0;
}

/* User List */
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

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}

.user-info {
  flex: 1;
  min-width: 0;
}

.user-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-on-surface);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-bottom: 2px;
}

.sign-badge {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  font-size: 11px;
  color: var(--color-on-surface-variant);

  svg {
    color: var(--color-primary);
    opacity: 0.6;
  }
}

.sign-days {
  font-weight: 700;
  color: var(--color-primary);
}

.user-chat-btn {
  width: 34px;
  height: 34px;
  border: 1.5px solid var(--color-surface-container-low);
  border-radius: 50%;
  background: none;
  color: var(--color-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  flex-shrink: 0;
  &:active { transform: scale(0.95); background: var(--color-surface-container-low); }
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

  &:active { transform: scale(0.95); }

  &.following {
    background: var(--color-surface-container-low);
    border-color: var(--color-surface-container-low);
    color: var(--color-on-surface-variant);
  }
}

/* Loading / Empty */
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

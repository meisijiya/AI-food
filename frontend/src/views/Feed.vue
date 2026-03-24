<template>
  <div class="feed-container" ref="scrollContainer" @scroll="onScroll">
    <div class="bg-glow bg-glow-1"></div>

    <!-- Header -->
    <div class="feed-header animate-fade-up">
      <div class="header-left">
        <h1 class="page-title" @click="resetToHall"><em>大厅</em></h1>
        <button class="friends-entry" @click="router.push('/friends')">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M22 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>
          好友
        </button>
      </div>
      <div class="header-actions">
        <button v-if="activeTab === 'feed'" class="filter-btn" @click="showFilter = true">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polygon points="22 3 2 3 10 12.46 10 19 14 21 14 12.46 22 3"/></svg>
          筛选
        </button>
        <button class="notify-btn" @click="router.push('/feed')">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9"/><path d="M10.3 21a1.94 1.94 0 0 0 3.4 0"/></svg>
          <span v-if="unreadCount > 0" class="notify-badge">{{ unreadCount > 99 ? '99+' : unreadCount }}</span>
        </button>
      </div>
    </div>

    <!-- Sub Navigation -->
    <div class="sub-nav animate-fade-up delay-100">
      <button 
        class="sub-nav-item" 
        :class="{ active: activeTab === 'hot' }"
        @click="switchTab('hot')"
      >
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M8.5 14.5A2.5 2.5 0 0 0 11 12c0-1.38-.5-2-1-3-1.072-2.143-.224-4.054 2-6 .5 2.5 2 4.9 4 6.5 2 1.6 3 3.5 3 5.5a7 7 0 1 1-14 0c0-1.153.433-2.294 1-3a2.5 2.5 0 0 0 2.5 2.5z"/></svg>
        热榜
      </button>
      <button 
        class="sub-nav-item" 
        :class="{ active: activeTab === 'friend' }"
        @click="switchTab('friend')"
      >
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M22 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>
        友榜
      </button>
    </div>

    <!-- Feed Tab Content -->
    <div v-if="activeTab === 'feed'" class="waterfall">
      <div
        v-for="(post, index) in posts"
        :key="post.id"
        class="feed-card animate-scale-in"
        :style="{ animationDelay: (index % 6) * 0.05 + 's' }"
        @click="router.push('/feed/' + post.id)"
      >
        <div v-if="post.thumbnailUrl" class="card-photo">
          <CachedImage :src="post.thumbnailUrl" :alt="post.foodName" :lazy="true" />
        </div>
        <div class="card-body">
          <div class="card-food">{{ post.foodName }}</div>
          <div v-if="post.commentPreview" class="card-preview">{{ post.commentPreview }}</div>
        </div>
        <div class="card-footer">
          <div class="card-user">
            <img v-if="post.avatar" :src="post.avatar" class="card-avatar" alt="" />
            <div v-else class="card-avatar-placeholder">
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M19 21v-2a4 4 0 0 0-4-4H9a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
            </div>
            <span class="card-nickname">{{ post.nickname || '匿名' }}</span>
          </div>
          <div class="card-likes">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M19 14c1.49-1.46 3-3.21 3-5.5A5.5 5.5 0 0 0 16.5 3c-1.76 0-3 .5-4.5 2-1.5-1.5-2.74-2-4.5-2A5.5 5.5 0 0 0 2 8.5c0 2.3 1.5 4.05 3 5.5l7 7Z"/></svg>
            {{ post.likeCount || 0 }}
          </div>
        </div>
      </div>
    </div>

    <!-- Hot Rank Tab Content -->
    <div v-else-if="activeTab === 'hot'" class="hot-rank-list">
      <div 
        v-for="(item, index) in hotRankList" 
        :key="item.id" 
        class="hot-rank-item animate-fade-up"
        :style="{ animationDelay: (index % 10) * 0.05 + 's' }"
        @click="router.push('/feed/' + item.id)"
      >
        <span class="rank-num" :class="{ 'top-3': index < 3 }">{{ index + 1 }}</span>
        <div v-if="item.thumbnailUrl" class="rank-photo">
          <CachedImage :src="item.thumbnailUrl" :alt="item.foodName" :lazy="true" />
        </div>
        <div class="rank-content">
          <div class="rank-food">{{ item.foodName }}</div>
          <div class="rank-meta">
            <span class="rank-user">{{ item.nickname || '匿名' }}</span>
            <span class="rank-score">热度 {{ item.hotScore }}</span>
          </div>
        </div>
      </div>
      <div v-if="hotRankList.length === 0" class="empty-state">
        <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" class="empty-icon"><path d="M8.5 14.5A2.5 2.5 0 0 0 11 12c0-1.38-.5-2-1-3-1.072-2.143-.224-4.054 2-6 .5 2.5 2 4.9 4 6.5 2 1.6 3 3.5 3 5.5a7 7 0 1 1-14 0c0-1.153.433-2.294 1-3a2.5 2.5 0 0 0 2.5 2.5z"/></svg>
        <div class="empty-text">暂无热榜数据</div>
        <div class="empty-hint">浏览、点赞、评论可增加热度</div>
      </div>
    </div>

    <!-- Friend Feed Tab Content -->
    <div v-else-if="activeTab === 'friend'" class="friend-feed-list">
      <div 
        v-for="(item, index) in friendFeedList" 
        :key="item.postId" 
        class="friend-feed-item animate-fade-up"
        :style="{ animationDelay: (index % 10) * 0.05 + 's' }"
        @click="router.push('/feed/' + item.postId)"
      >
        <div class="friend-avatar">
          <img v-if="item.avatar" :src="item.avatar" alt="" />
          <span v-else>{{ item.nickname?.charAt(0) || '?' }}</span>
        </div>
        <div class="friend-content">
          <div class="friend-header">
            <span class="friend-name">{{ item.nickname || '匿名' }}</span>
            <span class="friend-time">{{ formatTime(item.publishedAt) }}</span>
          </div>
          <div class="friend-food">{{ item.foodName }}</div>
        </div>
        <div v-if="item.thumbnailUrl" class="friend-photo">
          <CachedImage :src="item.thumbnailUrl" :alt="item.foodName" :lazy="true" />
        </div>
      </div>
      <div v-if="friendFeedList.length === 0" class="empty-state">
        <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" class="empty-icon"><path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M22 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>
        <div class="empty-text">暂无好友动态</div>
        <div class="empty-hint">关注更多人，看看他们在吃什么</div>
      </div>
    </div>

    <!-- Loading -->
    <div v-if="loading && posts.length > 0" class="loading-more">
      <div class="spinner"></div>
    </div>

    <!-- Empty for feed -->
    <div v-if="activeTab === 'feed' && !loading && posts.length === 0" class="empty-state">
      <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" class="empty-icon"><rect width="7" height="7" x="3" y="3" rx="1"/><rect width="7" height="7" x="14" y="3" rx="1"/><rect width="7" height="7" x="14" y="14" rx="1"/><rect width="7" height="7" x="3" y="14" rx="1"/></svg>
      <div class="empty-text">暂无发布内容</div>
      <div class="empty-hint">去推荐记录发布你的美食吧</div>
    </div>

    <div class="nav-spacer"></div>

    <!-- Filter modal -->
    <Transition name="overlay-fade">
      <div v-if="showFilter" class="filter-overlay" @click="showFilter = false">
        <div class="filter-panel" @click.stop>
          <div class="filter-title">筛选</div>
          <div class="filter-group">
            <label class="filter-label">食物名称</label>
            <input v-model="filterFoodName" class="filter-input" placeholder="搜索食物名称..." />
          </div>
          <div class="filter-group">
            <label class="filter-label">参数筛选</label>
            <select v-model="filterParamName" class="filter-select">
              <option value="">不限</option>
              <option value="time">用餐时间</option>
              <option value="location">用餐地点</option>
              <option value="mood">当前心情</option>
              <option value="taste">口味偏好</option>
              <option value="budget">预算范围</option>
              <option value="companion">同行人员</option>
              <option value="weather">天气情况</option>
            </select>
            <input v-if="filterParamName" v-model="filterParamValue" class="filter-input" placeholder="输入筛选值..." />
          </div>
          <div class="filter-actions">
            <button class="filter-reset" @click="resetFilter">重置</button>
            <button class="filter-apply" @click="applyFilter">确定</button>
          </div>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { feedApi } from '@/api'
import CachedImage from '@/components/CachedImage.vue'

const router = useRouter()
const scrollContainer = ref<HTMLElement>()

const activeTab = ref('feed')
const posts = ref<any[]>([])
const hotRankList = ref<any[]>([])
const friendFeedList = ref<any[]>([])
const page = ref(0)
const pageSize = 10
const loading = ref(false)
const finished = ref(false)
const unreadCount = ref(0)

const showFilter = ref(false)
const filterFoodName = ref('')
const filterParamName = ref('')
const filterParamValue = ref('')

function switchTab(tab: string) {
  activeTab.value = tab
  if (tab === 'feed') {
    fetchPosts(true)
  } else if (tab === 'hot') {
    fetchHotRank()
  } else if (tab === 'friend') {
    fetchFriendFeed()
  }
}

function resetToHall() {
  activeTab.value = 'feed'
  if (scrollContainer.value) {
    scrollContainer.value.scrollTop = 0
  }
  fetchPosts(true)
}

function onScroll() {
  const el = scrollContainer.value
  if (!el || loading.value || finished.value || activeTab.value !== 'feed') return
  if (el.scrollTop + el.clientHeight >= el.scrollHeight - 100) {
    fetchPosts()
  }
}

async function fetchPosts(reset = false) {
  if (loading.value) return
  if (!reset && finished.value) return
  loading.value = true
  if (reset) { page.value = 0; finished.value = false }

  try {
    const params: any = { page: page.value, size: pageSize }
    if (filterFoodName.value) params.foodName = filterFoodName.value
    if (filterParamName.value && filterParamValue.value) {
      params.paramName = filterParamName.value
      params.paramValue = filterParamValue.value
    }

    const res = await feedApi.getList(params)
    const list = res?.items || []
    if (reset) {
      posts.value = list
    } else {
      posts.value.push(...list)
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

async function fetchHotRank() {
  try {
    const res = await feedApi.getHotRank()
    hotRankList.value = res?.items || []
  } catch { /* ignore */ }
}

async function fetchFriendFeed() {
  try {
    const res = await feedApi.getFriendFeed({ page: 0, size: 20 })
    friendFeedList.value = res?.items || []
  } catch { /* ignore */ }
}

async function fetchNotifications() {
  try {
    const res = await feedApi.getNotifications()
    unreadCount.value = (res?.unreadLikes || 0) + (res?.unreadComments || 0)
  } catch { /* ignore */ }
}

function resetFilter() {
  filterFoodName.value = ''
  filterParamName.value = ''
  filterParamValue.value = ''
}

function applyFilter() {
  showFilter.value = false
  fetchPosts(true)
}

function formatTime(dateStr: string): string {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)
  const days = Math.floor(diff / 86400000)
  
  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  if (hours < 24) return `${hours}小时前`
  if (days < 7) return `${days}天前`
  return date.toLocaleDateString()
}

onMounted(() => {
  fetchPosts(true)
  fetchNotifications()
})
</script>

<style lang="scss" scoped>
.feed-container {
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
  background: var(--color-secondary-fixed);
  opacity: 0.1;
}

/* Header */
.feed-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  z-index: 1;
  position: relative;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.page-title {
  font-family: var(--font-serif);
  font-style: italic;
  font-size: 32px;
  font-weight: 400;
  color: var(--color-on-surface);
  cursor: pointer;
  transition: opacity 0.2s;
  &:active { opacity: 0.7; }
  em { font-style: italic; color: var(--color-primary); }
}

.friends-entry {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 14px;
  border: 1.5px solid var(--color-surface-container-low);
  border-radius: 100px;
  background: var(--color-surface-container-lowest);
  color: var(--color-on-surface-variant);
  font-family: var(--font-sans);
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  transition: all 0.2s;
  &:active { background: var(--color-surface-container-low); transform: scale(0.95); }
}

.header-actions {
  display: flex;
  gap: 8px;
}

.filter-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 14px;
  border: 1.5px solid var(--color-surface-container-low);
  border-radius: 100px;
  background: none;
  color: var(--color-on-surface-variant);
  font-family: var(--font-sans);
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  &:active { background: var(--color-surface-container-low); }
}

.notify-btn {
  position: relative;
  width: 36px;
  height: 36px;
  border: none;
  border-radius: 50%;
  background: none;
  color: var(--color-on-surface-variant);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

.notify-badge {
  position: absolute;
  top: 2px;
  right: 0;
  min-width: 16px;
  height: 16px;
  padding: 0 4px;
  border-radius: 8px;
  background: #ef4444;
  color: white;
  font-size: 10px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  line-height: 1;
}

/* Sub Navigation */
.sub-nav {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
  z-index: 1;
  position: relative;
}

.sub-nav-item {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 8px 16px;
  border: 1.5px solid var(--color-surface-container-low);
  border-radius: 100px;
  background: none;
  color: var(--color-on-surface-variant);
  font-family: var(--font-sans);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  
  svg {
    opacity: 0.7;
  }
  
  &.active {
    background: linear-gradient(135deg, var(--color-primary-container), var(--color-primary));
    border-color: transparent;
    color: white;
    svg { opacity: 1; stroke: white; }
  }
  
  &:not(.active):active {
    background: var(--color-surface-container-low);
  }
}

/* Waterfall */
.waterfall {
  column-count: 2;
  column-gap: 10px;
  z-index: 1;
  position: relative;
}

.feed-card {
  break-inside: avoid;
  margin-bottom: 10px;
  background: var(--color-surface-container-lowest);
  border-radius: 1.25rem;
  border: 1px solid rgba(255, 255, 255, 0.8);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
  &:active { transform: scale(0.98); }
}

.card-photo {
  img {
    width: 100%;
    display: block;
    border-radius: 1.25rem 1.25rem 0 0;
  }
}

.card-body {
  padding: 10px 12px;
}

.card-food {
  font-family: var(--font-serif);
  font-style: italic;
  font-size: 15px;
  font-weight: 500;
  color: var(--color-on-surface);
  margin-bottom: 4px;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-preview {
  font-size: 12px;
  line-height: 1.5;
  color: var(--color-on-surface-variant);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px 10px;
}

.card-user {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
}

.card-avatar {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
}

.card-avatar-placeholder {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: var(--color-surface-container-low);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-on-surface-variant);
  flex-shrink: 0;
}

.card-nickname {
  font-size: 11px;
  color: var(--color-on-surface-variant);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-likes {
  display: flex;
  align-items: center;
  gap: 3px;
  font-size: 11px;
  color: var(--color-on-surface-variant);
  flex-shrink: 0;
  svg { color: #ef4444; }
}

/* Hot Rank */
.hot-rank-list {
  z-index: 1;
  position: relative;
}

.hot-rank-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: var(--color-surface-container-lowest);
  border-radius: 1.25rem;
  border: 1px solid rgba(255, 255, 255, 0.8);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  margin-bottom: 8px;
  cursor: pointer;
  transition: transform 0.2s;
  &:active { transform: scale(0.98); }
}

.rank-num {
  width: 24px;
  height: 24px;
  border-radius: 6px;
  background: var(--color-surface-container-low);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
  color: var(--color-on-surface-variant);
  flex-shrink: 0;
  
  &.top-3 {
    background: linear-gradient(135deg, #f59e0b, #ef4444);
    color: white;
  }
}

.rank-photo {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  overflow: hidden;
  flex-shrink: 0;
  img { width: 100%; height: 100%; object-fit: cover; }
}

.rank-content {
  flex: 1;
  min-width: 0;
}

.rank-food {
  font-family: var(--font-serif);
  font-style: italic;
  font-size: 15px;
  font-weight: 500;
  color: var(--color-on-surface);
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.rank-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: var(--color-on-surface-variant);
}

.rank-user {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.rank-score {
  color: var(--color-primary);
  font-weight: 600;
}

/* Friend Feed */
.friend-feed-list {
  z-index: 1;
  position: relative;
}

.friend-feed-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: var(--color-surface-container-lowest);
  border-radius: 1.25rem;
  border: 1px solid rgba(255, 255, 255, 0.8);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  margin-bottom: 8px;
  cursor: pointer;
  transition: transform 0.2s;
  &:active { transform: scale(0.98); }
}

.friend-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--color-primary-container), var(--color-primary));
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-family: var(--font-serif);
  font-style: italic;
  font-size: 16px;
  font-weight: 400;
  flex-shrink: 0;
  overflow: hidden;
  img { width: 100%; height: 100%; object-fit: cover; }
}

.friend-content {
  flex: 1;
  min-width: 0;
}

.friend-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.friend-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-on-surface);
}

.friend-time {
  font-size: 11px;
  color: var(--color-on-surface-variant);
}

.friend-food {
  font-family: var(--font-serif);
  font-style: italic;
  font-size: 14px;
  color: var(--color-on-surface);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.friend-photo {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  overflow: hidden;
  flex-shrink: 0;
  img { width: 100%; height: 100%; object-fit: cover; }
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

/* Filter */
.filter-overlay {
  position: fixed;
  inset: 0;
  z-index: 200;
  background: rgba(11, 15, 16, 0.4);
  backdrop-filter: blur(8px);
  display: flex;
  align-items: center;
  justify-content: center;
}

.filter-panel {
  width: calc(100% - 48px);
  max-width: 400px;
  background: var(--color-surface-container-lowest);
  border-radius: 2rem;
  padding: 28px 24px;
}

.filter-title {
  font-family: var(--font-serif);
  font-style: italic;
  font-size: 22px;
  font-weight: 500;
  color: var(--color-on-surface);
  margin-bottom: 20px;
}

.filter-group {
  margin-bottom: 16px;
}

.filter-label {
  display: block;
  font-size: 12px;
  font-weight: 700;
  color: var(--color-on-surface-variant);
  text-transform: uppercase;
  letter-spacing: 0.1em;
  margin-bottom: 8px;
}

.filter-input, .filter-select {
  width: 100%;
  padding: 12px 16px;
  border: 1.5px solid var(--color-surface-container-low);
  border-radius: 1rem;
  background: var(--color-surface);
  font-family: var(--font-sans);
  font-size: 14px;
  color: var(--color-on-surface);
  outline: none;
  &:focus { border-color: var(--color-primary); }
}

.filter-select {
  appearance: none;
  margin-bottom: 8px;
}

.filter-actions {
  display: flex;
  gap: 10px;
  margin-top: 20px;
}

.filter-reset {
  flex: 1;
  padding: 12px;
  border: 1.5px solid var(--color-surface-container-low);
  border-radius: 1rem;
  background: none;
  color: var(--color-on-surface-variant);
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
}

.filter-apply {
  flex: 1;
  padding: 12px;
  border: none;
  border-radius: 1rem;
  background: linear-gradient(135deg, var(--color-primary-container), var(--color-primary));
  color: white;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
}

.overlay-fade-enter-active { transition: opacity 0.25s ease; }
.overlay-fade-leave-active { transition: opacity 0.2s ease; }
.overlay-fade-enter-from, .overlay-fade-leave-to { opacity: 0; }

@media (min-width: 640px) {
  .feed-container { padding: 40px 20px 100px; }
  .waterfall { column-gap: 14px; }
  .feed-card { margin-bottom: 14px; }
}
</style>

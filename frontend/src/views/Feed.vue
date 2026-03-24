<template>
  <div class="feed-container" ref="scrollContainer" @scroll="onScroll">
    <div class="bg-glow bg-glow-1"></div>

    <!-- Header -->
    <div class="feed-header animate-fade-up">
      <h1 class="page-title"><em>大厅</em></h1>
      <div class="header-actions">
        <button class="filter-btn" @click="showFilter = true">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polygon points="22 3 2 3 10 12.46 10 19 14 21 14 12.46 22 3"/></svg>
          筛选
        </button>
        <button class="notify-btn" @click="router.push('/feed')">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9"/><path d="M10.3 21a1.94 1.94 0 0 0 3.4 0"/></svg>
          <span v-if="unreadCount > 0" class="notify-badge">{{ unreadCount > 99 ? '99+' : unreadCount }}</span>
        </button>
      </div>
    </div>

    <!-- Waterfall grid -->
    <div class="waterfall">
      <div
        v-for="(post, index) in posts"
        :key="post.id"
        class="feed-card animate-scale-in"
        :style="{ animationDelay: (index % 6) * 0.05 + 's' }"
        @click="router.push('/feed/' + post.id)"
      >
        <!-- Photo -->
        <div v-if="post.thumbnailUrl" class="card-photo">
          <img :src="post.thumbnailUrl" :alt="post.foodName" loading="lazy" />
        </div>

        <!-- Content -->
        <div class="card-body">
          <div class="card-food">{{ post.foodName }}</div>
          <div v-if="post.commentPreview" class="card-preview">{{ post.commentPreview }}</div>
        </div>

        <!-- Footer -->
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

    <!-- Loading -->
    <div v-if="loading && posts.length > 0" class="loading-more">
      <div class="spinner"></div>
    </div>

    <!-- Empty -->
    <div v-if="!loading && posts.length === 0" class="empty-state">
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

const router = useRouter()
const scrollContainer = ref<HTMLElement>()

const posts = ref<any[]>([])
const page = ref(0)
const pageSize = 10
const loading = ref(false)
const finished = ref(false)
const unreadCount = ref(0)

const showFilter = ref(false)
const filterFoodName = ref('')
const filterParamName = ref('')
const filterParamValue = ref('')

function onScroll() {
  const el = scrollContainer.value
  if (!el || loading.value || finished.value) return
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
  margin-bottom: 16px;
  z-index: 1;
  position: relative;
}

.page-title {
  font-family: var(--font-serif);
  font-style: italic;
  font-size: 32px;
  font-weight: 400;
  color: var(--color-on-surface);
  em { font-style: italic; color: var(--color-primary); }
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

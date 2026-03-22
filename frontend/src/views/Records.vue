<template>
  <div class="records-container" ref="scrollContainer" @scroll="onScroll">
    <div class="bg-glow bg-glow-1"></div>

    <!-- Header -->
    <h1 class="page-title animate-fade-up">
      <em>推荐记录</em>
    </h1>

    <!-- Pull-down indicator -->
    <div v-if="refreshing" class="refresh-indicator">
      <div class="refresh-spinner"></div>
      <span>刷新中...</span>
    </div>

    <!-- Record list -->
    <div class="record-list">
      <div
        v-for="(record, index) in records"
        :key="record.sessionId"
        class="record-card"
        :class="['animate-fade-up', `delay-${Math.min(index + 1, 4)}00`, 'animate-start-hidden']"
        @click="goDetail(record.sessionId)"
      >
        <div class="record-food">{{ displayFoodName(record) }}</div>
        <div class="record-reason">{{ displayReason(record) }}</div>
        <div class="record-date">{{ formatDate(record.createdAt) }}</div>
      </div>
    </div>

    <!-- Loading more -->
    <div v-if="loading && records.length > 0" class="loading-more">
      <div class="refresh-spinner"></div>
      <span>加载更多...</span>
    </div>

    <!-- No more -->
    <div v-if="finished && records.length > 0" class="no-more">没有更多了</div>

    <!-- Empty state -->
    <div v-if="!loading && records.length === 0" class="empty-state">
      <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" class="empty-icon">
        <path d="M12 8v4l3 3"/>
        <circle cx="12" cy="12" r="10"/>
      </svg>
      <div class="empty-text">暂无推荐记录</div>
      <div class="empty-hint">去首页开始你的美食之旅吧</div>
    </div>

    <div class="nav-spacer"></div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { recordApi } from '@/api'

interface RecordItem {
  sessionId: string
  foodName: string
  reason: string
  createdAt: string
  status?: string
  mode?: string
}

const router = useRouter()
const scrollContainer = ref<HTMLElement>()

const records = ref<RecordItem[]>([])
const page = ref(1)
const pageSize = 10
const loading = ref(false)
const finished = ref(false)
const refreshing = ref(false)

async function fetchRecords(reset = false) {
  if (loading.value) return
  if (!reset && finished.value) return

  loading.value = true
  if (reset) {
    page.value = 1
    finished.value = false
  }

  try {
    // 后端返回: { list, total, pages, current }
    const res = await recordApi.getRecordList({ page: page.value - 1, size: pageSize })
    const list = res?.list || []
    if (reset) {
      records.value = list
    } else {
      records.value.push(...list)
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
    refreshing.value = false
  }
}

function onScroll() {
  const el = scrollContainer.value
  if (!el || loading.value || finished.value) return
  if (el.scrollTop + el.clientHeight >= el.scrollHeight - 100) {
    fetchRecords()
  }
}

async function onPullDown() {
  refreshing.value = true
  await fetchRecords(true)
}

function goDetail(sessionId: string) {
  router.push('/records/' + sessionId)
}

function formatDate(dateStr: string) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

function displayFoodName(record: RecordItem) {
  return record.foodName || '暂无推荐结果'
}

function displayReason(record: RecordItem) {
  return record.reason || `会话状态：${record.status || 'completed'} · 模式：${record.mode || 'inertia'}`
}

onMounted(() => {
  fetchRecords(true)

  const el = scrollContainer.value
  if (!el) return
  let startY = 0
  el.addEventListener('touchstart', (e) => {
    if (el.scrollTop <= 0) {
      startY = e.touches[0].clientY
    }
  }, { passive: true })
  el.addEventListener('touchend', (e) => {
    const diff = e.changedTouches[0].clientY - startY
    if (diff > 60 && el.scrollTop <= 0) {
      onPullDown()
    }
  }, { passive: true })
})
</script>

<style lang="scss" scoped>
.records-container {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: var(--color-surface);
  padding: 40px 20px 100px;
  position: relative;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
}

.bg-glow {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  pointer-events: none;
  animation: sanctuary-glow-pulse 6s ease-in-out infinite;
}

.bg-glow-1 {
  top: -60px;
  left: -40px;
  width: 240px;
  height: 240px;
  background: var(--color-primary-container);
  opacity: 0.12;
}

.page-title {
  font-family: var(--font-serif);
  font-style: italic;
  font-size: 32px;
  font-weight: 400;
  color: var(--color-on-surface);
  margin-bottom: 24px;
  z-index: 1;

  em {
    font-style: italic;
    color: var(--color-primary);
  }
}

.refresh-indicator {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 12px 0;
  font-size: 12px;
  color: var(--color-on-surface-variant);
  z-index: 1;
}

.refresh-spinner {
  width: 16px;
  height: 16px;
  border: 2px solid var(--color-surface-container-low);
  border-top-color: var(--color-primary);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.record-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  z-index: 1;
}

.record-card {
  padding: 20px 24px;
  background: var(--color-surface-container-lowest);
  border-radius: 2rem;
  border: 1px solid rgba(255, 255, 255, 0.8);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  cursor: pointer;
  transition: transform 0.3s cubic-bezier(0.22, 1, 0.36, 1), box-shadow 0.3s ease;

  &:active {
    transform: scale(0.98);
  }
}

.record-food {
  font-family: var(--font-serif);
  font-style: italic;
  font-size: 18px;
  font-weight: 400;
  color: var(--color-on-surface);
  margin-bottom: 8px;
}

.record-reason {
  font-size: 13px;
  line-height: 1.6;
  color: var(--color-on-surface-variant);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  margin-bottom: 10px;
}

.record-date {
  font-size: 11px;
  color: var(--color-on-surface-variant);
  opacity: 0.6;
}

.loading-more {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 20px 0;
  font-size: 12px;
  color: var(--color-on-surface-variant);
  z-index: 1;
}

.no-more {
  text-align: center;
  padding: 20px 0;
  font-size: 12px;
  color: var(--color-on-surface-variant);
  opacity: 0.5;
  z-index: 1;
}

.empty-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 0;
  z-index: 1;
}

.empty-icon {
  color: var(--color-on-surface-variant);
  opacity: 0.3;
  margin-bottom: 16px;
}

.empty-text {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-on-surface-variant);
  margin-bottom: 6px;
}

.empty-hint {
  font-size: 12px;
  color: var(--color-on-surface-variant);
  opacity: 0.6;
}

.nav-spacer {
  height: 80px;
}
</style>

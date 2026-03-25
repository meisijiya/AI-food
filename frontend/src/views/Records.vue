<template>
  <div class="records-container" ref="scrollContainer" @scroll="onScroll">
    <div class="bg-glow bg-glow-1"></div>

    <!-- Header row -->
    <div class="header-row animate-fade-up">
      <h1 class="page-title"><em>推荐记录</em></h1>
      <div class="header-actions">
        <!-- Sort toggle -->
        <button class="sort-btn" @click="toggleSort">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path v-if="sortOrder === 'desc'" d="m3 8 4-4 4 4"/><path v-if="sortOrder === 'desc'" d="M7 4v16"/>
            <path v-if="sortOrder === 'asc'" d="m3 16 4 4 4-4"/><path v-if="sortOrder === 'asc'" d="M7 20V4"/>
          </svg>
          {{ sortOrder === 'desc' ? '降序' : '升序' }}
        </button>
        <!-- Batch delete toggle -->
        <button class="edit-btn" :class="{ active: batchMode }" @click="toggleBatchMode">
          {{ batchMode ? '取消' : '编辑' }}
        </button>
      </div>
    </div>

    <!-- Batch action bar -->
    <div v-if="batchMode" class="batch-bar animate-fade-up">
      <label class="select-all">
        <input type="checkbox" :checked="allSelected" @change="toggleSelectAll" />
        <span>全选 ({{ selectedIds.length }})</span>
      </label>
      <button class="batch-delete-btn" :disabled="selectedIds.length === 0" @click="batchDelete">
        删除选中
      </button>
    </div>

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
        :class="[
          'animate-fade-up',
          `delay-${Math.min(index + 1, 4)}00`,
          'animate-start-hidden',
          { selected: selectedIds.includes(record.sessionId) }
        ]"
        @click="onCardClick(record.sessionId)"
      >
        <!-- Batch select checkbox -->
        <label v-if="batchMode" class="card-checkbox" @click.stop>
          <input
            type="checkbox"
            :checked="selectedIds.includes(record.sessionId)"
            @change="toggleSelect(record.sessionId)"
          />
        </label>

        <div class="card-body">
          <div class="record-food">{{ displayFoodName(record) }}</div>
          <div class="record-reason">{{ displayReason(record) }}</div>
          <div class="record-date">{{ formatDate(record.createdAt) }}</div>
        </div>

        <!-- Single delete button -->
        <button v-if="!batchMode" class="card-delete" @click.stop="deleteRecord(record.sessionId)" title="删除">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M3 6h18"/><path d="M19 6v14c0 1-1 2-2 2H7c-1 0-2-1-2-2V6"/><path d="M8 6V4c0-1 1-2 2-2h4c1 0 2 1 2 2v2"/>
          </svg>
        </button>
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
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { recordApi } from '@/api'
import { showSuccess, showError } from '@/utils/toast'

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

// Sort
const sortOrder = ref<'desc' | 'asc'>('desc')

// Batch mode
const batchMode = ref(false)
const selectedIds = ref<string[]>([])

const allSelected = computed(() =>
  records.value.length > 0 && selectedIds.value.length === records.value.length
)

function toggleSort() {
  sortOrder.value = sortOrder.value === 'desc' ? 'asc' : 'desc'
  fetchRecords(true)
}

function toggleBatchMode() {
  batchMode.value = !batchMode.value
  selectedIds.value = []
}

function toggleSelect(sessionId: string) {
  const idx = selectedIds.value.indexOf(sessionId)
  if (idx >= 0) {
    selectedIds.value.splice(idx, 1)
  } else {
    selectedIds.value.push(sessionId)
  }
}

function toggleSelectAll() {
  if (allSelected.value) {
    selectedIds.value = []
  } else {
    selectedIds.value = records.value.map(r => r.sessionId)
  }
}

function onCardClick(sessionId: string) {
  if (batchMode.value) {
    toggleSelect(sessionId)
  } else {
    router.push('/records/' + sessionId)
  }
}

async function fetchRecords(reset = false) {
  if (loading.value) return
  if (!reset && finished.value) return

  loading.value = true
  if (reset) {
    page.value = 1
    finished.value = false
  }

  try {
    const res = await recordApi.getRecordList({
      page: page.value - 1,
      size: pageSize,
      sort: sortOrder.value
    })
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

async function deleteRecord(sessionId: string) {
  try {
    await recordApi.deleteRecord(sessionId)
    records.value = records.value.filter(r => r.sessionId !== sessionId)
    showSuccess('删除成功')
  } catch {
    showError('删除失败')
  }
}

async function batchDelete() {
  if (selectedIds.value.length === 0) return
  try {
    await recordApi.batchDeleteRecords(selectedIds.value)
    records.value = records.value.filter(r => !selectedIds.value.includes(r.sessionId))
    selectedIds.value = []
    batchMode.value = false
    showSuccess('批量删除成功')
  } catch {
    showError('批量删除失败')
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

/* Header row */
.header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  z-index: 1;
}

.page-title {
  font-family: var(--font-serif);
  font-style: italic;
  font-size: 32px;
  font-weight: 400;
  color: var(--color-on-surface);

  em {
    font-style: italic;
    color: var(--color-primary);
  }
}

.header-actions {
  display: flex;
  gap: 8px;
}

.sort-btn {
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
  transition: all 0.2s;

  &:active {
    background: var(--color-surface-container-low);
  }
}

.edit-btn {
  padding: 6px 14px;
  border: 1.5px solid var(--color-surface-container-low);
  border-radius: 100px;
  background: none;
  color: var(--color-on-surface-variant);
  font-family: var(--font-sans);
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;

  &.active {
    border-color: var(--color-primary);
    color: var(--color-primary);
  }
}

/* Batch bar */
.batch-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 16px;
  background: var(--color-surface-container-low);
  border-radius: 1rem;
  margin-bottom: 12px;
  z-index: 1;
}

.select-all {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: var(--color-on-surface);
  cursor: pointer;

  input[type="checkbox"] {
    width: 16px;
    height: 16px;
    accent-color: var(--color-primary);
  }
}

.batch-delete-btn {
  padding: 6px 16px;
  border: none;
  border-radius: 100px;
  background: #ef4444;
  color: white;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;

  &:disabled {
    opacity: 0.4;
    cursor: not-allowed;
  }
}

/* Pull-down */
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

/* Record list */
.record-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  z-index: 1;
}

.record-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 20px 24px;
  background: var(--color-surface-container-lowest);
  border-radius: 2rem;
  border: 1px solid rgba(255, 255, 255, 0.8);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  cursor: pointer;
  transition: transform 0.3s cubic-bezier(0.22, 1, 0.36, 1), box-shadow 0.3s ease, border-color 0.2s;

  &:active {
    transform: scale(0.98);
  }

  &.selected {
    border-color: var(--color-primary-container);
    background: rgba(104, 160, 255, 0.05);
  }
}

.card-checkbox {
  flex-shrink: 0;
  cursor: pointer;

  input[type="checkbox"] {
    width: 18px;
    height: 18px;
    accent-color: var(--color-primary);
  }
}

.card-body {
  flex: 1;
  min-width: 0;
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

.card-delete {
  flex-shrink: 0;
  width: 36px;
  height: 36px;
  border: none;
  border-radius: 50%;
  background: rgba(239, 68, 68, 0.08);
  color: #ef4444;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;

  &:active {
    background: rgba(239, 68, 68, 0.15);
    transform: scale(0.9);
  }
}

/* Loading / empty */
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

@media (min-width: 1024px) {
  .records-container {
    max-width: 60%;
    margin: 0 auto;
  }
}
</style>

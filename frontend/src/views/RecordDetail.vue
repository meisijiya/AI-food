<template>
  <div class="detail-container">
    <div class="bg-glow bg-glow-1"></div>

    <!-- Loading -->
    <div v-if="loading" class="loading-state">
      <div class="loading-spinner"></div>
    </div>

    <template v-if="detail">
      <!-- Header -->
      <h1 class="page-title animate-fade-up">
        <em>{{ foodName }}</em>
      </h1>

      <!-- Recommendation card (dark) -->
      <div class="recommend-card animate-fade-up delay-100 animate-start-hidden">
        <div class="recommend-food">{{ foodName }}</div>
        <div class="recommend-reason" v-if="reason">{{ reason }}</div>
      </div>

      <!-- Collected params -->
      <div v-if="paramsList.length" class="params-section animate-fade-up delay-200 animate-start-hidden">
        <div class="section-label">收集信息</div>
        <div class="params-chips">
          <span
            v-for="p in paramsList"
            :key="p.paramName"
            class="param-chip"
          >
            {{ paramLabel(p.paramName) }}: {{ p.paramValue }}
          </span>
        </div>
      </div>

      <!-- Q&A timeline -->
      <div v-if="qaList.length" class="timeline-section animate-fade-up delay-300 animate-start-hidden">
        <div class="section-label">对话记录</div>
        <div class="timeline">
          <template v-for="(qa, index) in qaList" :key="index">
            <!-- AI question -->
            <div class="timeline-item">
              <div class="timeline-dot assistant"></div>
              <div class="timeline-line"></div>
              <div class="timeline-content">
                <div class="timeline-role">AI</div>
                <div class="timeline-text">{{ qa.aiQuestion }}</div>
              </div>
            </div>
            <!-- User answer -->
            <div class="timeline-item">
              <div class="timeline-dot user"></div>
              <div class="timeline-line" v-if="index < qaList.length - 1"></div>
              <div class="timeline-content">
                <div class="timeline-role user-role">你</div>
                <div class="timeline-text">{{ qa.userAnswer }}</div>
              </div>
            </div>
          </template>
        </div>
      </div>

      <!-- Session info -->
      <div v-if="detail.session" class="session-info animate-fade-up delay-400 animate-start-hidden">
        <div class="session-meta">
          <span v-if="detail.session.mode">{{ detail.session.mode === 'inertia' ? '惯性模式' : '随机模式' }}</span>
          <span v-if="detail.session.totalQuestions">{{ detail.session.currentQuestionCount }}/{{ detail.session.totalQuestions }} 轮</span>
          <span v-if="detail.session.createdAt">{{ formatDate(detail.session.createdAt) }}</span>
        </div>
      </div>
    </template>

    <!-- Back button -->
    <button class="back-btn animate-fade-up delay-500 animate-start-hidden" @click="router.back()">
      返回
    </button>

    <div class="nav-spacer"></div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { recordApi } from '@/api'

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const detail = ref<any>(null)

const paramLabels: Record<string, string> = {
  time: '用餐时间',
  location: '用餐地点',
  weather: '天气情况',
  mood: '当前心情',
  companion: '同行人员',
  budget: '预算范围',
  taste: '口味偏好',
  restriction: '饮食禁忌',
  preference: '特殊偏好',
  health: '健康需求'
}

function paramLabel(key: string) {
  return paramLabels[key] || key
}

function formatDate(dateStr: string) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

// 从后端 RecordDetail 结构中提取各字段
const foodName = computed(() => {
  return detail.value?.recommendation?.foodName || '暂无推荐结果'
})

const reason = computed(() => {
  return detail.value?.recommendation?.reason || ''
})

const paramsList = computed(() => {
  return detail.value?.collectedParams || []
})

const qaList = computed(() => {
  return detail.value?.qaRecords || []
})

async function fetchDetail() {
  const sessionId = route.params.sessionId as string
  if (!sessionId) {
    loading.value = false
    return
  }

  try {
    const res = await recordApi.getRecordDetail(sessionId)
    // api 拦截器已解包 ApiResponse → 返回 RecordDetail 的 data 部分
    // data 包含: { session, recommendation, collectedParams, qaRecords }
    detail.value = res
  } catch {
    // ignore
  } finally {
    loading.value = false
  }
}

onMounted(fetchDetail)
</script>

<style lang="scss" scoped>
.detail-container {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: var(--color-surface);
  padding: 40px 20px 100px;
  position: relative;
  overflow-y: auto;
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
  right: -40px;
  width: 240px;
  height: 240px;
  background: var(--color-primary-container);
  opacity: 0.12;
}

.loading-state {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.loading-spinner {
  width: 32px;
  height: 32px;
  border: 3px solid var(--color-surface-container-low);
  border-top-color: var(--color-primary);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
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

/* Recommendation card */
.recommend-card {
  background: var(--color-inverse-surface);
  border-radius: 2rem;
  padding: 28px 24px;
  margin-bottom: 20px;
  z-index: 1;
  position: relative;
  overflow: hidden;

  &::after {
    content: '';
    position: absolute;
    top: -40px;
    right: -40px;
    width: 160px;
    height: 160px;
    background: rgba(140, 225, 243, 0.08);
    border-radius: 50%;
    filter: blur(40px);
  }
}

.recommend-food {
  font-family: var(--font-serif);
  font-style: italic;
  font-size: 24px;
  color: white;
  margin-bottom: 12px;
  position: relative;
  z-index: 1;
}

.recommend-reason {
  font-size: 14px;
  line-height: 1.8;
  color: rgba(255, 255, 255, 0.65);
  position: relative;
  z-index: 1;
}

/* Params */
.params-section {
  margin-bottom: 20px;
  z-index: 1;
}

.section-label {
  font-size: 12px;
  font-weight: 700;
  color: var(--color-on-surface-variant);
  text-transform: uppercase;
  letter-spacing: 0.1em;
  margin-bottom: 12px;
}

.params-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.param-chip {
  padding: 8px 16px;
  background: var(--color-surface-container-lowest);
  border-radius: 100px;
  border: 1px solid var(--color-surface-container-low);
  font-size: 12px;
  color: var(--color-on-surface);
}

/* Timeline */
.timeline-section {
  z-index: 1;
  margin-bottom: 20px;
}

.timeline {
  position: relative;
  padding-left: 20px;
}

.timeline-item {
  position: relative;
  padding-bottom: 16px;
  padding-left: 20px;

  &:last-child {
    padding-bottom: 0;
  }
}

.timeline-dot {
  position: absolute;
  left: -10px;
  top: 4px;
  width: 12px;
  height: 12px;
  border-radius: 50%;
  border: 2px solid var(--color-primary);
  background: var(--color-surface);

  &.assistant {
    border-color: #22d3ee;
  }

  &.user {
    border-color: var(--color-primary);
  }
}

.timeline-line {
  position: absolute;
  left: -5px;
  top: 18px;
  bottom: 0;
  width: 2px;
  background: var(--color-surface-container-low);
}

.timeline-content {
  background: var(--color-surface-container-lowest);
  border-radius: 1.25rem;
  padding: 14px 18px;
  border: 1px solid rgba(255, 255, 255, 0.8);
}

.timeline-role {
  font-size: 10px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  color: #22d3ee;
  margin-bottom: 6px;

  &.user-role {
    color: var(--color-primary);
  }
}

.timeline-text {
  font-size: 13px;
  line-height: 1.7;
  color: var(--color-on-surface);
}

/* Session info */
.session-info {
  margin-bottom: 16px;
  z-index: 1;
}

.session-meta {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;

  span {
    font-size: 11px;
    color: var(--color-on-surface-variant);
    padding: 4px 12px;
    background: var(--color-surface-container-low);
    border-radius: 100px;
  }
}

/* Back button */
.back-btn {
  padding: 14px;
  border: 1.5px solid var(--color-surface-container-low);
  border-radius: 2rem;
  background: none;
  color: var(--color-on-surface-variant);
  font-family: var(--font-sans);
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  z-index: 1;
  margin-top: 8px;

  &:hover {
    border-color: var(--color-primary);
    color: var(--color-primary);
  }

  &:active {
    transform: scale(0.98);
  }
}

.nav-spacer {
  height: 80px;
}
</style>

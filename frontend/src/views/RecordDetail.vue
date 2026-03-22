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
        <em>{{ detail.foodName || '推荐详情' }}</em>
      </h1>

      <!-- Recommendation card -->
      <div class="recommend-card animate-fade-up delay-100 animate-start-hidden">
        <div class="recommend-food">{{ detail.foodName }}</div>
        <div class="recommend-reason">{{ detail.reason }}</div>
      </div>

      <!-- Collected params -->
      <div v-if="detail.params && Object.keys(detail.params).length" class="params-section animate-fade-up delay-200 animate-start-hidden">
        <div class="section-label">收集信息</div>
        <div class="params-chips">
          <span
            v-for="(value, key) in detail.params"
            :key="key"
            class="param-chip"
          >
            {{ paramLabel(key as string) }}: {{ value }}
          </span>
        </div>
      </div>

      <!-- Q&A timeline -->
      <div v-if="detail.history && detail.history.length" class="timeline-section animate-fade-up delay-300 animate-start-hidden">
        <div class="section-label">对话记录</div>
        <div class="timeline">
          <div
            v-for="(item, index) in detail.history"
            :key="index"
            class="timeline-item"
          >
            <div class="timeline-dot" :class="item.role"></div>
            <div class="timeline-line" v-if="index < detail.history.length - 1"></div>
            <div class="timeline-content">
              <div class="timeline-role">{{ item.role === 'assistant' ? 'AI' : '你' }}</div>
              <div class="timeline-text">{{ item.content }}</div>
            </div>
          </div>
        </div>
      </div>
    </template>

    <!-- Back button -->
    <button class="back-btn animate-fade-up delay-400 animate-start-hidden" @click="router.back()">
      返回
    </button>

    <div class="nav-spacer"></div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { recordApi } from '@/api'

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const detail = ref<any>(null)

const paramLabels: Record<string, string> = {
  mood: '心情',
  time: '时间',
  weather: '天气',
  budget: '预算',
  cuisine: '菜系',
  dietary: '饮食偏好',
  location: '位置',
  mealType: '餐次',
  spiceLevel: '辣度',
  occasion: '场合'
}

function paramLabel(key: string) {
  return paramLabels[key] || key
}

async function fetchDetail() {
  const sessionId = route.params.sessionId as string
  if (!sessionId) {
    loading.value = false
    return
  }

  try {
    const res = await recordApi.getRecordDetail(sessionId as string)
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
}

.recommend-food {
  font-family: var(--font-serif);
  font-style: italic;
  font-size: 24px;
  color: white;
  margin-bottom: 12px;
}

.recommend-reason {
  font-size: 14px;
  line-height: 1.8;
  color: rgba(255, 255, 255, 0.65);
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
  padding-bottom: 20px;
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
  color: var(--color-primary);
  margin-bottom: 6px;
}

.timeline-text {
  font-size: 13px;
  line-height: 1.7;
  color: var(--color-on-surface);
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

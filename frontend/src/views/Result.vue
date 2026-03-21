<template>
  <div class="result-page">
    <!-- Decorative glows -->
    <div class="result-glow glow-1"></div>
    <div class="result-glow glow-2"></div>

    <div class="result-content">
      <!-- Header -->
      <div class="result-header animate-fade-up">
        <div class="check-circle animate-scale-in delay-100 animate-start-hidden">
          <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M20 6 9 17l-5-5"/></svg>
        </div>
        <h1 class="result-title animate-fade-up delay-200 animate-start-hidden">
          <em>为你推荐</em>
        </h1>
      </div>

      <!-- Food Card (Dark) -->
      <div class="food-card animate-fade-up delay-300 animate-start-hidden">
        <div class="food-card-glow"></div>
        <div class="food-name">{{ foodName }}</div>
        <div class="food-reason" v-if="reason">{{ reason }}</div>
      </div>

      <!-- Params Card (Light) -->
      <div class="params-card animate-fade-up delay-400 animate-start-hidden" v-if="collectedParams.length > 0">
        <div class="params-glow"></div>
        <h3 class="params-title">根据你的需求</h3>
        <div class="params-grid">
          <div
            v-for="param in collectedParams"
            :key="param.name"
            class="param-chip"
          >
            <span class="param-label">{{ param.label }}</span>
            <span class="param-value">{{ param.value }}</span>
          </div>
        </div>
      </div>

      <!-- Action Buttons -->
      <div class="actions animate-fade-up delay-500 animate-start-hidden">
        <button class="primary-btn" @click="startNewChat">
          再来一次
        </button>
        <button class="ghost-btn" @click="goHome">
          返回首页
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useChatStore } from '@/stores/chat'

const router = useRouter()
const chatStore = useChatStore()

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

const foodName = computed(() => {
  try {
    const result = JSON.parse(chatStore.recommendationResult || '{}')
    return result.foodName || '暂无推荐'
  } catch { return '暂无推荐' }
})

const reason = computed(() => {
  try {
    const result = JSON.parse(chatStore.recommendationResult || '{}')
    return result.reason || ''
  } catch { return '' }
})

const collectedParams = computed(() => {
  const paramValues = chatStore.collectedParamValues
  if (Object.keys(paramValues).length > 0) {
    return Object.entries(paramValues).map(([name, value]) => ({
      name, label: paramLabels[name] || name, value
    }))
  }
  return chatStore.progress.collected.map(name => ({
    name, label: paramLabels[name] || name, value: '已收集'
  }))
})

const startNewChat = () => {
  chatStore.clearChat()
  router.push('/')
}

const goHome = () => {
  router.push('/')
}
</script>

<style lang="scss" scoped>
.result-page {
  min-height: 100vh;
  background: var(--color-surface);
  padding: 40px 20px 60px;
  position: relative;
  overflow: hidden;
}

.result-glow {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  pointer-events: none;
  animation: sanctuary-glow-pulse 6s ease-in-out infinite;
}

.glow-1 {
  top: -40px;
  left: -60px;
  width: 260px;
  height: 260px;
  background: var(--color-secondary-fixed);
  opacity: 0.15;
}

.glow-2 {
  bottom: 80px;
  right: -80px;
  width: 220px;
  height: 220px;
  background: var(--color-primary-container);
  opacity: 0.1;
  animation-delay: 3s;
}

.result-content {
  max-width: 480px;
  margin: 0 auto;
  position: relative;
  z-index: 1;
}

/* Header */
.result-header {
  text-align: center;
  margin-bottom: 32px;
}

.check-circle {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: linear-gradient(135deg, #22c55e, #16a34a);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 20px;
  box-shadow: 0 12px 32px -8px rgba(34, 197, 94, 0.4);
}

.result-title {
  font-family: var(--font-serif);
  font-style: italic;
  font-size: 36px;
  font-weight: 400;
  color: var(--color-on-surface);

  em {
    font-style: italic;
    color: var(--color-primary);
  }
}

/* Food Card (Dark) */
.food-card {
  background: var(--color-inverse-surface);
  color: white;
  border-radius: 2.5rem;
  padding: 40px 32px;
  text-align: center;
  margin-bottom: 20px;
  position: relative;
  overflow: hidden;
}

.food-card-glow {
  position: absolute;
  top: -40px;
  right: -40px;
  width: 160px;
  height: 160px;
  background: rgba(140, 225, 243, 0.08);
  border-radius: 50%;
  filter: blur(40px);
}

.food-name {
  font-family: var(--font-serif);
  font-style: italic;
  font-size: 32px;
  font-weight: 500;
  margin-bottom: 16px;
  position: relative;
  z-index: 1;
}

.food-reason {
  font-size: 14px;
  line-height: 1.8;
  color: rgba(255, 255, 255, 0.65);
  position: relative;
  z-index: 1;
}

/* Params Card (Light) */
.params-card {
  background: var(--color-surface-container-lowest);
  border-radius: 2rem;
  padding: 28px 24px;
  border: 1px solid rgba(255, 255, 255, 0.8);
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  margin-bottom: 32px;
  position: relative;
  overflow: hidden;
}

.params-glow {
  position: absolute;
  bottom: -30px;
  left: -30px;
  width: 120px;
  height: 120px;
  background: var(--color-secondary-fixed);
  opacity: 0.08;
  border-radius: 50%;
  filter: blur(30px);
}

.params-title {
  font-family: var(--font-serif);
  font-size: 18px;
  font-weight: 500;
  color: var(--color-on-surface);
  margin-bottom: 16px;
  position: relative;
  z-index: 1;
}

.params-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  position: relative;
  z-index: 1;
}

.param-chip {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 14px;
  background: var(--color-surface);
  border-radius: 1rem;
  font-size: 12px;
}

.param-label {
  color: var(--color-on-surface-variant);
  font-weight: 600;
}

.param-value {
  color: var(--color-primary);
  font-weight: 700;
}

/* Actions */
.actions {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.primary-btn {
  width: 100%;
  padding: 18px;
  border: none;
  border-radius: 2rem;
  background: linear-gradient(135deg, var(--color-primary-container), var(--color-primary));
  color: white;
  font-family: var(--font-sans);
  font-size: 15px;
  font-weight: 700;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  cursor: pointer;
  box-shadow: 0 12px 32px -8px rgba(0, 89, 182, 0.35);
  transition: all 0.3s cubic-bezier(0.22, 1, 0.36, 1);
  &:hover { transform: translateY(-2px); box-shadow: 0 16px 40px -8px rgba(0, 89, 182, 0.45); }
  &:active { transform: translateY(0); }
}

.ghost-btn {
  width: 100%;
  padding: 16px;
  border: 1.5px solid var(--color-surface-container-low);
  border-radius: 2rem;
  background: transparent;
  color: var(--color-on-surface-variant);
  font-family: var(--font-sans);
  font-size: 14px;
  font-weight: 600;
  letter-spacing: 0.05em;
  cursor: pointer;
  transition: all 0.2s;
  &:hover { background: var(--color-surface-container-low); }
}

@media (min-width: 640px) {
  .result-page {
    padding: 60px 40px 80px;
  }
}
</style>

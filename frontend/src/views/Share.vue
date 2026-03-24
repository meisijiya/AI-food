<template>
  <div class="share-page">
    <!-- Decorative glows -->
    <div class="share-glow glow-1"></div>
    <div class="share-glow glow-2"></div>

    <!-- Loading -->
    <div v-if="loading" class="loading-state">
      <div class="loading-spinner"></div>
    </div>

    <!-- Error -->
    <div v-else-if="error" class="error-state animate-fade-up">
      <div class="error-icon">
        <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><path d="m15 9-6 6"/><path d="m9 9 6 6"/></svg>
      </div>
      <h2 class="error-title"><em>分享不存在</em></h2>
      <p class="error-desc">该分享链接已失效或不存在</p>
    </div>

    <!-- Content -->
    <div v-else-if="share" class="share-content">
      <!-- App entry card — top floating, scrolls with page -->
      <div class="app-card animate-fade-up">
        <div class="app-card-glow"></div>
        <div class="app-card-inner">
          <div class="app-card-text">
            <div class="app-card-title">探索更多美食推荐</div>
            <div class="app-card-desc">让 AI 为你找到最适合的那一道菜</div>
          </div>
          <a :href="appUrl" class="app-cta-btn">打开应用</a>
        </div>
      </div>

      <!-- Sharer info -->
      <div class="sharer-section animate-fade-up delay-100 animate-start-hidden">
        <img v-if="share.sharerAvatar" :src="share.sharerAvatar" class="sharer-avatar" alt="头像" />
        <div v-else class="sharer-avatar-placeholder">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M19 21v-2a4 4 0 0 0-4-4H9a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
        </div>
        <div class="sharer-info">
          <span class="sharer-name">{{ share.sharerNickname || '匿名用户' }}</span>
          <span class="sharer-action">分享了一道美食</span>
        </div>
      </div>

      <!-- Hero photo -->
      <div v-if="share.thumbnailUrl" class="hero-photo-card animate-fade-up delay-150 animate-start-hidden">
        <img :src="share.thumbnailUrl" class="hero-photo" :alt="share.foodName" />
      </div>

      <!-- Food title -->
      <h1 class="food-title animate-fade-up delay-200 animate-start-hidden">
        <em>{{ share.foodName }}</em>
      </h1>

      <!-- Recommendation reason (dark card) -->
      <div class="reason-card animate-fade-up delay-250 animate-start-hidden">
        <div class="reason-food">{{ share.foodName }}</div>
        <div class="reason-text" v-if="share.reason">{{ share.reason }}</div>
      </div>

      <!-- Collected params -->
      <div v-if="paramsList.length" class="params-section animate-fade-up delay-300 animate-start-hidden">
        <div class="section-label">收集信息</div>
        <div class="params-grid">
          <div v-for="p in paramsList" :key="p.name" class="param-item">
            <span class="param-key">{{ paramLabel(p.name) }}</span>
            <span class="param-val">{{ p.value }}</span>
          </div>
        </div>
      </div>

      <!-- User comment -->
      <div v-if="share.comment" class="comment-section animate-fade-up delay-350 animate-start-hidden">
        <div class="section-label">美食评价</div>
        <div class="comment-text">{{ share.comment }}</div>
      </div>

      <!-- Q&A timeline -->
      <div v-if="qaList.length" class="timeline-section animate-fade-up delay-400 animate-start-hidden">
        <div class="section-label">对话记录</div>
        <div class="timeline">
          <template v-for="(qa, index) in qaList" :key="index">
            <div class="timeline-item">
              <div class="timeline-dot assistant"></div>
              <div class="timeline-line"></div>
              <div class="timeline-content">
                <div class="timeline-role">AI</div>
                <div class="timeline-text">{{ qa.question }}</div>
              </div>
            </div>
            <div class="timeline-item">
              <div class="timeline-dot user"></div>
              <div class="timeline-line" v-if="index < qaList.length - 1"></div>
              <div class="timeline-content">
                <div class="timeline-role user-role">{{ share.sharerNickname || '用户' }}</div>
                <div class="timeline-text">{{ qa.answer }}</div>
              </div>
            </div>
          </template>
        </div>
      </div>

      <!-- App entry bottom card -->
      <div class="app-bottom-card animate-fade-up delay-450 animate-start-hidden">
        <div class="app-bottom-inner">
          <div class="app-bottom-icon">
            <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M15 21v-8a1 1 0 0 0-1-1h-4a1 1 0 0 0-1 1v8"/><path d="M3 10a2 2 0 0 1 .709-1.528l7-5.999a2 2 0 0 1 2.582 0l7 5.999A2 2 0 0 1 21 10v9a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/></svg>
          </div>
          <div class="app-bottom-text">
            <div class="app-bottom-name">AI Food</div>
            <div class="app-bottom-desc">智能美食推荐助手</div>
          </div>
          <a :href="appUrl" class="app-bottom-btn">立即体验</a>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { shareApi } from '@/api'

const route = useRoute()
const loading = ref(true)
const error = ref(false)
const share = ref<any>(null)

const appUrl = import.meta.env.VITE_APP_URL || window.location.origin
const shareToken = computed(() => route.params.token as string)

const paramLabels: Record<string, string> = {
  time: '用餐时间', location: '用餐地点', weather: '天气情况',
  mood: '当前心情', companion: '同行人员', budget: '预算范围',
  taste: '口味偏好', restriction: '饮食禁忌', preference: '特殊偏好',
  health: '健康需求'
}

function paramLabel(key: string) {
  return paramLabels[key] || key
}

const paramsList = computed(() => share.value?.collectedParams || [])
const qaList = computed(() => share.value?.qaRecords || [])

async function fetchShare() {
  const token = shareToken.value
  if (!token) { loading.value = false; error.value = true; return }
  try {
    const res = await shareApi.getShareDetail(token)
    share.value = res
  } catch {
    error.value = true
  } finally {
    loading.value = false
  }
}

onMounted(fetchShare)
</script>

<style lang="scss" scoped>
.share-page {
  min-height: 100vh;
  background: var(--color-surface);
  padding: 0 0 80px;
  position: relative;
  overflow-x: hidden;
}

/* Glows */
.share-glow {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  pointer-events: none;
  animation: sanctuary-glow-pulse 6s ease-in-out infinite;
}

.glow-1 {
  top: -60px;
  right: -40px;
  width: 240px;
  height: 240px;
  background: var(--color-secondary-fixed);
  opacity: 0.12;
}

.glow-2 {
  bottom: 200px;
  left: -80px;
  width: 200px;
  height: 200px;
  background: var(--color-primary-container);
  opacity: 0.08;
  animation-delay: 3s;
}

/* Loading */
.loading-state {
  min-height: 60vh;
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

@keyframes spin { to { transform: rotate(360deg); } }

/* Error */
.error-state {
  min-height: 60vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  padding: 40px;
}

.error-icon { color: var(--color-on-surface-variant); opacity: 0.4; margin-bottom: 20px; }
.error-title {
  font-family: var(--font-serif); font-style: italic; font-size: 28px; font-weight: 400;
  color: var(--color-on-surface); margin-bottom: 8px;
  em { font-style: italic; color: var(--color-primary); }
}
.error-desc { font-size: 14px; color: var(--color-on-surface-variant); }

/* Content */
.share-content {
  max-width: 480px;
  margin: 0 auto;
  padding: 20px 20px 40px;
  position: relative;
  z-index: 1;
}

/* App card — top floating */
.app-card {
  background: linear-gradient(135deg, var(--color-inverse-surface), #1a2332);
  border-radius: 2rem;
  padding: 20px 24px;
  margin-bottom: 20px;
  position: relative;
  overflow: hidden;
}

.app-card-glow {
  position: absolute;
  top: -30px;
  right: -30px;
  width: 120px;
  height: 120px;
  background: var(--color-primary-container);
  opacity: 0.15;
  border-radius: 50%;
  filter: blur(30px);
}

.app-card-inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  position: relative;
  z-index: 1;
}

.app-card-text { flex: 1; }

.app-card-title {
  font-family: var(--font-serif); font-style: italic;
  font-size: 16px; font-weight: 500; color: white; margin-bottom: 4px;
}

.app-card-desc {
  font-size: 12px; color: rgba(255, 255, 255, 0.5);
}

.app-cta-btn {
  display: inline-block;
  padding: 10px 24px;
  border-radius: 100px;
  background: linear-gradient(135deg, var(--color-primary-container), var(--color-primary));
  color: white;
  font-family: var(--font-sans);
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.05em;
  text-decoration: none;
  box-shadow: 0 8px 24px -6px rgba(0, 89, 182, 0.4);
  white-space: nowrap;
  transition: all 0.2s;
  &:active { transform: scale(0.96); }
}

/* Sharer */
.sharer-section {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
}

.sharer-avatar {
  width: 40px; height: 40px; border-radius: 50%;
  object-fit: cover; border: 2px solid var(--color-surface-container-low);
}

.sharer-avatar-placeholder {
  width: 40px; height: 40px; border-radius: 50%;
  background: var(--color-surface-container-low);
  display: flex; align-items: center; justify-content: center;
  color: var(--color-on-surface-variant);
}

.sharer-info { display: flex; flex-direction: column; gap: 2px; }
.sharer-name { font-size: 14px; font-weight: 600; color: var(--color-on-surface); }
.sharer-action { font-size: 12px; color: var(--color-on-surface-variant); }

/* Hero photo */
.hero-photo-card { margin-bottom: 24px; }

.hero-photo {
  width: 100%; border-radius: 2rem; display: block;
  max-height: 320px; object-fit: cover;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
}

/* Food title */
.food-title {
  font-family: var(--font-serif); font-style: italic;
  font-size: 32px; font-weight: 400; color: var(--color-on-surface);
  margin-bottom: 24px; text-align: center;
  em { font-style: italic; color: var(--color-primary); }
}

/* Reason card */
.reason-card {
  background: var(--color-inverse-surface);
  border-radius: 2rem; padding: 28px 24px;
  margin-bottom: 20px; position: relative; overflow: hidden;
  &::after {
    content: ''; position: absolute;
    top: -40px; right: -40px; width: 140px; height: 140px;
    background: rgba(140, 225, 243, 0.08); border-radius: 50%; filter: blur(40px);
  }
}

.reason-food {
  font-family: var(--font-serif); font-style: italic;
  font-size: 20px; color: white; margin-bottom: 10px;
  position: relative; z-index: 1;
}

.reason-text {
  font-size: 14px; line-height: 1.8; color: rgba(255, 255, 255, 0.65);
  position: relative; z-index: 1;
}

/* Params — card grid */
.params-section { margin-bottom: 20px; }

.section-label {
  font-size: 10px; font-weight: 700; color: var(--color-on-surface-variant);
  text-transform: uppercase; letter-spacing: 0.15em; margin-bottom: 12px;
}

.params-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}

.param-item {
  display: flex; flex-direction: column; gap: 2px;
  padding: 12px 16px;
  background: var(--color-surface-container-lowest);
  border-radius: 1.25rem;
  border: 1px solid rgba(255, 255, 255, 0.8);
}

.param-key { font-size: 10px; color: var(--color-on-surface-variant); font-weight: 600; text-transform: uppercase; letter-spacing: 0.08em; }
.param-val { font-size: 14px; color: var(--color-on-surface); font-weight: 500; }

/* Comment */
.comment-section { margin-bottom: 20px; }

.comment-text {
  padding: 16px 20px;
  background: var(--color-surface-container-lowest);
  border-radius: 1.25rem; border: 1px solid rgba(255, 255, 255, 0.8);
  font-size: 14px; line-height: 1.7; color: var(--color-on-surface);
  white-space: pre-wrap;
}

/* Timeline */
.timeline-section { margin-bottom: 24px; }

.timeline { position: relative; padding-left: 20px; }

.timeline-item {
  position: relative; padding-bottom: 14px; padding-left: 20px;
  &:last-child { padding-bottom: 0; }
}

.timeline-dot {
  position: absolute; left: -10px; top: 4px;
  width: 12px; height: 12px; border-radius: 50%;
  border: 2px solid var(--color-primary); background: var(--color-surface);
  &.assistant { border-color: #22d3ee; }
  &.user { border-color: var(--color-primary); }
}

.timeline-line {
  position: absolute; left: -5px; top: 18px; bottom: 0;
  width: 2px; background: var(--color-surface-container-low);
}

.timeline-content {
  background: var(--color-surface-container-lowest);
  border-radius: 1rem; padding: 12px 16px;
  border: 1px solid rgba(255, 255, 255, 0.8);
}

.timeline-role {
  font-size: 10px; font-weight: 700; text-transform: uppercase;
  letter-spacing: 0.1em; color: #22d3ee; margin-bottom: 4px;
  &.user-role { color: var(--color-primary); }
}

.timeline-text {
  font-size: 13px; line-height: 1.7; color: var(--color-on-surface);
}

/* Bottom app card */
.app-bottom-card {
  background: var(--color-surface-container-lowest);
  border-radius: 1.5rem; padding: 20px;
  border: 1px solid var(--color-surface-container-low);
}

.app-bottom-inner {
  display: flex; align-items: center; gap: 14px;
}

.app-bottom-icon {
  width: 48px; height: 48px; border-radius: 1rem;
  background: linear-gradient(135deg, var(--color-primary-container), var(--color-primary));
  color: white; display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}

.app-bottom-text { flex: 1; }
.app-bottom-name {
  font-family: var(--font-serif); font-style: italic;
  font-size: 16px; font-weight: 600; color: var(--color-on-surface);
}
.app-bottom-desc { font-size: 12px; color: var(--color-on-surface-variant); margin-top: 2px; }

.app-bottom-btn {
  padding: 10px 20px; border-radius: 100px;
  background: linear-gradient(135deg, var(--color-primary-container), var(--color-primary));
  color: white; font-family: var(--font-sans); font-size: 13px; font-weight: 700;
  text-decoration: none; white-space: nowrap;
  box-shadow: 0 6px 20px -6px rgba(0, 89, 182, 0.35);
  transition: all 0.2s;
  &:active { transform: scale(0.96); }
}

@media (min-width: 640px) {
  .share-content { padding: 32px 24px 60px; }
  .food-title { font-size: 40px; }
  .params-grid { gap: 12px; }
}
</style>

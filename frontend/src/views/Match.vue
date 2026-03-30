<template>
  <div class="match-page">
    <!-- Decorative glow -->
    <div class="page-glow"></div>

    <!-- Header -->
    <div class="match-header animate-fade-up">
      <button class="back-btn" @click="goBack">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M19 12H5M12 19l-7-7 7-7"/>
        </svg>
      </button>
      <h1 class="page-title"><em>匹配</em></h1>
      <div class="header-spacer"></div>
    </div>

    <!-- Loading State -->
    <div v-if="loading" class="loading-container">
      <div class="loading-card glass-card">
        <div class="pulse-avatar"></div>
        <div class="pulse-line"></div>
        <div class="pulse-line short"></div>
      </div>
    </div>

    <!-- Match Card -->
    <div v-else-if="currentMatch" class="match-content">
      <div class="user-card glass-card vibrant-shadow animate-scale-in" :key="currentMatch.userId">
        <!-- Decorative glow blob -->
        <div class="card-glow"></div>
        
        <!-- Avatar Section -->
        <div class="avatar-section">
          <div class="avatar-wrapper">
            <img
              v-if="currentMatch.avatar"
              :src="getFullAvatarUrl(currentMatch.avatar)"
              class="avatar-image"
              alt=""
            />
            <div v-else class="avatar-placeholder">
              {{ currentMatch.nickname?.charAt(0) || '?' }}
            </div>
            <!-- Similarity badge -->
            <div class="similarity-badge">
              {{ Math.round(currentMatch.similarity * 100) }}%
            </div>
          </div>
        </div>

        <!-- User Info -->
        <div class="user-info">
          <h2 class="user-name">
            {{ currentMatch.nickname || '匿名用户' }}
          </h2>
          <p class="match-label">口味匹配度</p>
          <p v-if="hasFoodName" class="food-name">推荐食物：{{ currentMatch.foodName }}</p>
        </div>

        <!-- Collected Params -->
        <div class="params-section" v-if="hasParams">
          <div class="params-grid">
            <div 
              v-for="(value, key) in currentMatch.collectedParams" 
              :key="key"
              class="param-item"
            >
              <span class="param-label">{{ getParamLabel(String(key)) }}</span>
              <span class="param-value">{{ value }}</span>
            </div>
          </div>
        </div>

        <!-- Action Buttons -->
        <div class="action-buttons">
          <button 
            v-if="!currentMatch.isFollowing"
            class="btn-primary"
            @click="followMatchUser"
          >
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"/>
              <circle cx="9" cy="7" r="4"/>
              <line x1="19" y1="8" x2="19" y2="14"/>
              <line x1="22" y1="11" x2="16" y2="11"/>
            </svg>
            关注
          </button>

          <button 
            class="btn-secondary"
            @click="goToChat"
          >
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
            </svg>
            发消息
          </button>
        </div>

        <!-- Next Button -->
        <button 
          class="btn-ghost"
          @click="fetchNextMatch"
        >
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M23 4v6h-6M1 20v-6h6"/>
            <path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"/>
          </svg>
          换一个
        </button>
      </div>
    </div>

    <!-- Empty State -->
    <div v-else class="empty-state">
      <div class="empty-card glass-card animate-fade-up">
        <div class="empty-icon-wrapper">
          <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
            <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/>
          </svg>
        </div>
        <h3 class="empty-title">暂无匹配</h3>
        <p class="empty-hint">发布推荐记录，让更多人发现你</p>
        <button 
          class="btn-back"
          @click="goBackToHall"
        >
          返回大厅
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount, onDeactivated } from 'vue'
import { useRouter } from 'vue-router'
import { matchApi, followApi, chatApi } from '@/api'

const router = useRouter()

const loading = ref(true)
const currentMatch = ref<any>(null)
const matchedUserIds = ref<number[]>([])

/**
 * 重置当前进入匹配页期间的临时匹配状态，离开页面后允许重新匹配到之前用户。
 */
function resetMatchSessionState() {
  matchedUserIds.value = []
  currentMatch.value = null
}

const hasParams = computed(() => {
  return currentMatch.value?.collectedParams && 
    Object.keys(currentMatch.value.collectedParams).length > 0
})

const hasFoodName = computed(() => !!currentMatch.value?.foodName)

async function fetchRandomMatch() {
  loading.value = true
  try {
    const data = await matchApi.getRandomMatch(
      matchedUserIds.value.length > 0 ? matchedUserIds.value : undefined
    )
    currentMatch.value = data
  } catch {
    currentMatch.value = null
  } finally {
    loading.value = false
  }
}

async function fetchNextMatch() {
  if (currentMatch.value) {
    matchedUserIds.value.push(currentMatch.value.userId)
  }
  loading.value = true
  try {
    const data = await matchApi.getRandomMatch(matchedUserIds.value)
    currentMatch.value = data
  } catch {
    currentMatch.value = null
  } finally {
    loading.value = false
  }
}

/**
 * 关注当前匹配到的用户，并以服务端返回状态为准更新按钮状态。
 */
async function followMatchUser() {
  if (!currentMatch.value) return
  try {
    const result = await followApi.toggleFollow(currentMatch.value.userId)
    currentMatch.value.isFollowing = !!result?.isFollowing
  } catch {
    // ignore
  }
}

/**
 * 打开与当前匹配用户的聊天页，沿用项目统一的 chat-room 路由与 query 格式。
 */
async function goToChat() {
  if (!currentMatch.value) return
  try {
    const conversation = await chatApi.getOrCreateConversationWith(currentMatch.value.userId)
    router.push({
      path: '/chat-room',
      query: {
        userId: currentMatch.value.userId,
        conversationId: conversation?.conversationId,
        nickname: currentMatch.value.nickname || '',
        avatar: currentMatch.value.avatar || ''
      }
    })
  } catch {
    router.push({
      path: '/chat-room',
      query: {
        userId: currentMatch.value.userId,
        nickname: currentMatch.value.nickname || '',
        avatar: currentMatch.value.avatar || ''
      }
    })
  }
}

function goBack() {
  router.back()
}

function goBackToHall() {
  router.push('/feed')
}

function getFullAvatarUrl(path: string): string {
  if (!path) return ''
  if (path.startsWith('http')) return path
  return import.meta.env.VITE_API_URL + path
}

function getParamLabel(key: string): string {
  const labels: Record<string, string> = {
    taste: '口味',
    mood: '心情',
    time: '用餐时间',
    location: '地点',
    budget: '预算',
    companion: '同行人员',
    weather: '天气',
  }
  return labels[key] || key
}

onMounted(() => {
  resetMatchSessionState()
  fetchRandomMatch()
})

onBeforeUnmount(() => {
  resetMatchSessionState()
})

onDeactivated(() => {
  resetMatchSessionState()
})
</script>

<style lang="scss" scoped>
.match-page {
  min-height: 100vh;
  background: var(--color-surface);
  padding: 20px 16px 100px;
  position: relative;
  overflow: hidden;
}

.page-glow {
  position: absolute;
  top: -100px;
  right: -80px;
  width: 300px;
  height: 300px;
  background: var(--color-secondary-fixed);
  border-radius: 50%;
  filter: blur(100px);
  opacity: 0.15;
  pointer-events: none;
}

.match-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 32px;
  position: relative;
  z-index: 1;
}

.back-btn {
  width: 40px;
  height: 40px;
  border: none;
  border-radius: 50%;
  background: var(--color-surface-container-lowest);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: var(--color-on-surface-variant);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  transition: all 0.2s;
  &:active {
    transform: scale(0.95);
    background: var(--color-surface-container-low);
  }
}

.page-title {
  font-family: var(--font-serif);
  font-style: italic;
  font-size: 24px;
  font-weight: 400;
  color: var(--color-on-surface);
  em {
    font-style: italic;
    color: var(--color-primary);
  }
}

.header-spacer {
  width: 40px;
}

.loading-container {
  display: flex;
  justify-content: center;
  padding-top: 60px;
  position: relative;
  z-index: 1;
}

.loading-card {
  width: 280px;
  padding: 40px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20px;
}

.pulse-avatar {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--color-primary-container), var(--color-primary));
  opacity: 0.3;
  animation: pulse 1.5s ease-in-out infinite;
}

.pulse-line {
  width: 160px;
  height: 16px;
  border-radius: 8px;
  background: var(--color-surface-container-low);
  animation: pulse 1.5s ease-in-out infinite;
  &.short {
    width: 100px;
  }
}

@keyframes pulse {
  0%, 100% { opacity: 0.3; transform: scale(0.98); }
  50% { opacity: 0.5; transform: scale(1); }
}

.match-content {
  display: flex;
  justify-content: center;
  position: relative;
  z-index: 1;
}

.user-card {
  width: 100%;
  max-width: 380px;
  padding: 40px 32px;
  position: relative;
  overflow: hidden;
}

.card-glow {
  position: absolute;
  top: -60px;
  right: -60px;
  width: 200px;
  height: 200px;
  background: var(--color-secondary-fixed);
  border-radius: 50%;
  filter: blur(60px);
  opacity: 0.15;
  pointer-events: none;
  transition: opacity 0.5s;
}

.avatar-section {
  display: flex;
  justify-content: center;
  margin-bottom: 24px;
}

.avatar-wrapper {
  position: relative;
}

.avatar-image {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  object-fit: cover;
  border: 4px solid var(--color-surface-container-lowest);
  box-shadow: 0 8px 24px rgba(0, 89, 182, 0.15);
}

.avatar-placeholder {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--color-primary-container), var(--color-primary));
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-family: var(--font-serif);
  font-style: italic;
  font-size: 48px;
  font-weight: 400;
  border: 4px solid var(--color-surface-container-lowest);
  box-shadow: 0 8px 24px rgba(0, 89, 182, 0.15);
}

.similarity-badge {
  position: absolute;
  bottom: 0;
  right: 0;
  background: linear-gradient(135deg, var(--color-primary-container), var(--color-primary));
  color: white;
  font-size: 13px;
  font-weight: 700;
  padding: 6px 14px;
  border-radius: 100px;
  box-shadow: 0 4px 12px rgba(0, 89, 182, 0.3);
}

.user-info {
  text-align: center;
  margin-bottom: 24px;
}

.user-name {
  font-family: var(--font-serif);
  font-style: italic;
  font-size: 28px;
  font-weight: 400;
  color: var(--color-on-surface);
  margin-bottom: 8px;
}

.match-label {
  font-size: 12px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.15em;
  color: var(--color-on-surface-variant);
}

.params-section {
  margin-bottom: 28px;
}

.params-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.param-item {
  background: var(--color-surface-container-low);
  border-radius: 1rem;
  padding: 12px 16px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.param-label {
  font-size: 10px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  color: var(--color-on-surface-variant);
}

.param-value {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-on-surface);
}

.action-buttons {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.btn-primary {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 14px 24px;
  border: none;
  border-radius: 1.5rem;
  background: linear-gradient(135deg, var(--color-primary-container), var(--color-primary));
  color: white;
  font-family: var(--font-sans);
  font-size: 14px;
  font-weight: 700;
  letter-spacing: 0.05em;
  cursor: pointer;
  box-shadow: 0 8px 20px rgba(0, 89, 182, 0.25);
  transition: all 0.2s;
  &:hover {
    box-shadow: 0 12px 28px rgba(0, 89, 182, 0.35);
    transform: translateY(-2px);
  }
  &:active {
    transform: scale(0.98) translateY(0);
  }
}

.btn-secondary {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 14px 24px;
  border: none;
  border-radius: 1.5rem;
  background: var(--color-inverse-surface);
  color: white;
  font-family: var(--font-sans);
  font-size: 14px;
  font-weight: 700;
  letter-spacing: 0.05em;
  cursor: pointer;
  box-shadow: 0 8px 20px rgba(11, 15, 16, 0.2);
  transition: all 0.2s;
  &:hover {
    box-shadow: 0 12px 28px rgba(11, 15, 16, 0.3);
    transform: translateY(-2px);
  }
  &:active {
    transform: scale(0.98) translateY(0);
  }
}

.btn-ghost {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 12px 24px;
  border: 1.5px solid var(--color-surface-container-low);
  border-radius: 1.5rem;
  background: transparent;
  color: var(--color-on-surface-variant);
  font-family: var(--font-sans);
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  &:hover {
    background: var(--color-surface-container-low);
    border-color: transparent;
    transform: translateY(-1px);
  }
  &:active {
    transform: scale(0.99);
  }
}

.empty-state {
  display: flex;
  justify-content: center;
  padding-top: 80px;
  position: relative;
  z-index: 1;
}

.empty-card {
  width: 100%;
  max-width: 320px;
  padding: 48px 32px;
  text-align: center;
}

.empty-icon-wrapper {
  width: 100px;
  height: 100px;
  margin: 0 auto 24px;
  border-radius: 50%;
  background: linear-gradient(135deg, rgba(0, 89, 182, 0.08), rgba(140, 225, 243, 0.08));
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-on-surface-variant);
  opacity: 0.4;
}

.empty-title {
  font-family: var(--font-serif);
  font-style: italic;
  font-size: 24px;
  font-weight: 400;
  color: var(--color-on-surface);
  margin-bottom: 12px;
}

.empty-hint {
  font-size: 14px;
  color: var(--color-on-surface-variant);
  margin-bottom: 28px;
}

.btn-back {
  padding: 14px 32px;
  border: none;
  border-radius: 1.5rem;
  background: linear-gradient(135deg, var(--color-primary-container), var(--color-primary));
  color: white;
  font-family: var(--font-sans);
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  box-shadow: 0 8px 20px rgba(0, 89, 182, 0.25);
  transition: all 0.2s;
  &:hover {
    box-shadow: 0 12px 28px rgba(0, 89, 182, 0.35);
    transform: translateY(-2px);
  }
  &:active {
    transform: scale(0.98) translateY(0);
  }
}

@media (min-width: 640px) {
  .match-page {
    padding: 40px 20px 100px;
  }
  .user-card {
    padding: 48px 40px;
  }
}

@media (min-width: 1024px) {
  .match-page {
    max-width: 60%;
    margin: 0 auto;
    padding: 40px 40px 100px;
  }
}
</style>

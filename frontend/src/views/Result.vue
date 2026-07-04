<template>
  <div class="result-page">
    <!-- Decorative glows -->
    <div class="result-glow glow-1"></div>
    <div class="result-glow glow-2"></div>

    <!-- Loading -->
    <div v-if="loading" class="loading-state">
      <div class="loading-spinner"></div>
    </div>

    <div v-else class="result-content">
      <!-- Header -->
      <div class="result-header animate-fade-up">
        <div class="check-circle animate-scale-in delay-100 animate-start-hidden">
          <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M20 6 9 17l-5-5"/></svg>
        </div>
        <h1 class="result-title animate-fade-up delay-200 animate-start-hidden">
          <em>为你推荐</em>
        </h1>
      </div>

      <!-- Food Card -->
      <ResultFoodCard :food-name="foodName" :reason="reason" />

      <!-- Params Card(轻量,留父组件内联) -->
      <div
        class="params-card animate-fade-up delay-400 animate-start-hidden"
        v-if="collectedParams.length > 0"
      >
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

      <!-- Photo Manager -->
      <ResultPhotoManager
        :session-id="sessionId"
        :photo="uploadedPhoto"
        @photo-uploaded="onPhotoUploaded"
        @photo-deleted="handleDeletePhoto"
      />

      <!-- Comment -->
      <ResultComment
        v-if="sessionId"
        :session-id="sessionId"
        :saving="savingComment"
        @save="saveComment"
      />

      <!-- Share + Publish(合并到 ResultActions) -->
      <ResultActions
        v-if="sessionId"
        :session-id="sessionId"
        :share-url="shareUrl"
        :is-published="isPublished"
        :sharing="sharing"
        :publishing="publishing"
        :unpublishing="unpublishing"
        :initial-publish-preview="initialPublishPreview"
        @share="handleShare"
        @copy-share-url="copyShareUrl"
        @confirm-publish="handlePublish"
        @unpublish="handleUnpublish"
      />

      <!-- Recommendation Actions -->
      <ResultRecommendation
        @new-chat="startNewChat"
        @go-home="goHome"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
// Result.vue - 推荐结果页面父组件
// 职责: 加载数据 + 持有共享 state + 调用 API + 编排 5 个子组件
// 子组件只通过 props + emit 通信,API 调用全部在本组件
import { ref, computed, onMounted } from 'vue'
import type { PendingRecommendation } from '@/types/result'
import { useRouter } from 'vue-router'
import { useChatStore } from '@/stores/chat'
import { recordApi, shareApi, feedApi } from '@/api'
import { showSuccess, showError } from '@/utils/toast'
import type { Photo } from '@/types/result'
import ResultFoodCard from './components/Result/ResultFoodCard.vue'
import ResultPhotoManager from './components/Result/ResultPhotoManager.vue'
import ResultComment from './components/Result/ResultComment.vue'
import ResultActions from './components/Result/ResultActions.vue'
import ResultRecommendation from './components/Result/ResultRecommendation.vue'

const router = useRouter()
const chatStore = useChatStore()

// 加载状态
const loading = ref(true)
const pendingData = ref<PendingRecommendation | null>(null)

// 照片(子组件 ResultPhotoManager 拥有 UI 状态,这里只持有数据)
const uploadedPhoto = ref<Photo | null>(null)

// 评论 / 分享 / 发布 相关 state(子组件维护 UI 状态,这里持有 loading + 数据)
const savingComment = ref(false)
const shareUrl = ref('')
const sharing = ref(false)
const isPublished = ref(false)
const publishing = ref(false)
const unpublishing = ref(false)

// onMounted: 加载待推荐数据 + 检查发布状态
onMounted(async () => {
  try {
    const res = await recordApi.getPendingRecommendation()
    if (res?.hasPending && res.sessionId) {
      pendingData.value = res
      chatStore.sessionId = res.sessionId
    } else if (chatStore.recommendationResult) {
      pendingData.value = {
        sessionId: chatStore.sessionId,
        content: chatStore.recommendationResult
      }
    }
    // Check if already published
    const sid = sessionId.value
    if (sid) {
      try {
        const pubRes = await feedApi.checkPublished(sid)
        isPublished.value = pubRes?.published || false
      } catch {
        /* ignore */
      }
    }
  } catch {
    // Fallback to chat store
    if (chatStore.recommendationResult) {
      pendingData.value = {
        sessionId: chatStore.sessionId,
        content: chatStore.recommendationResult
      }
    }
  } finally {
    loading.value = false
  }
})

// 参数标签映射(从 pendingData 或 chatStore 解析)
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

// 计算属性: 从 chat store / pendingData 提取展示数据
const sessionId = computed(() => {
  return pendingData.value?.sessionId || chatStore.sessionId
})

const foodName = computed(() => {
  try {
    const result = JSON.parse(chatStore.recommendationResult || '{}')
    return result.foodName || '暂无推荐'
  } catch {
    return '暂无推荐'
  }
})

const reason = computed(() => {
  try {
    const result = JSON.parse(chatStore.recommendationResult || '{}')
    return result.reason || ''
  } catch {
    return ''
  }
})

const collectedParams = computed(() => {
  const paramValues = pendingData.value?.paramValues || {}
  if (Object.keys(paramValues).length > 0) {
    return Object.entries(paramValues).map(([name, value]) => ({
      name,
      label: paramLabels[name] || name,
      value
    }))
  }
  return chatStore.progress.collected.map((name) => ({
    name,
    label: paramLabels[name] || name,
    value: '已收集'
  }))
})

// 发布弹窗的初始预览值(取自 chatStore.recommendationResult.reason,截前 30 字)
const initialPublishPreview = computed(() => {
  try {
    const result = JSON.parse(chatStore.recommendationResult || '{}')
    return result.reason ? result.reason.substring(0, 30) : ''
  } catch {
    return ''
  }
})

// ===== Photo handlers =====
function onPhotoUploaded(data: Photo) {
  uploadedPhoto.value = data
  // 保存到数据库并清除 Redis 缓存
  const sid = sessionId.value
  if (sid) {
    recordApi.updatePhoto(sid, data.thumbnailUrl).catch(() => {})
  }
}

async function handleDeletePhoto() {
  const sid = sessionId.value
  if (!sid) return
  try {
    await recordApi.deletePhoto(sid)
    uploadedPhoto.value = null
    showSuccess('照片已删除')
  } catch {
    showError('删除失败')
  }
}

// ===== Comment handler =====
async function saveComment(text: string) {
  const sid = sessionId.value
  if (!sid) return
  savingComment.value = true
  try {
    await recordApi.updateComment(sid, text || '')
    showSuccess('评价已保存')
  } catch {
    showError('保存失败')
  } finally {
    savingComment.value = false
  }
}

// ===== Share handlers =====
async function handleShare() {
  const sid = sessionId.value
  if (!sid) return
  sharing.value = true
  try {
    const res = await shareApi.createShare(sid)
    const appUrl = import.meta.env.VITE_APP_URL || window.location.origin
    shareUrl.value = `${appUrl}/share/${res.shareToken}`
  } catch {
    showError('创建分享链接失败')
  } finally {
    sharing.value = false
  }
}

async function copyShareUrl() {
  if (!shareUrl.value) return
  try {
    await navigator.clipboard.writeText(shareUrl.value)
    showSuccess('链接已复制')
  } catch {
    showError('复制失败，请手动复制')
  }
}

// ===== Publish handlers =====
async function handlePublish(preview: string) {
  const sid = sessionId.value
  if (!sid) return
  publishing.value = true
  try {
    await feedApi.publish({ sessionId: sid, commentPreview: preview || undefined })
    isPublished.value = true
    showSuccess('发布成功')
  } catch (e: any) {
    showError(e?.message || '发布失败')
  } finally {
    publishing.value = false
  }
}

async function handleUnpublish() {
  const sid = sessionId.value
  if (!sid) return
  unpublishing.value = true
  try {
    await feedApi.unpublish(sid)
    isPublished.value = false
    showSuccess('已取消发布')
  } catch (e: any) {
    showError(e?.message || '取消失败')
  } finally {
    unpublishing.value = false
  }
}

// ===== Navigation =====
const startNewChat = () => {
  chatStore.clearChat()
  router.push('/')
}

const goHome = () => {
  router.push('/')
}
</script>

<style lang="scss" scoped>
/* ===== Layout (保留父组件布局相关样式) ===== */
.result-page {
  min-height: 100vh;
  background: var(--color-surface);
  padding: 40px 20px 60px;
  position: relative;
  overflow: hidden;
}

.loading-state {
  min-height: 60vh;
  display: flex;
  align-items: center;
  justify-content: center;
}

.loading-spinner {
  width: 32px;
  height: 32px;
  border: 3px solid var(--color-surface-low);
  border-top-color: var(--color-primary);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
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
  background: var(--color-cyan);
  opacity: 0.15;
}

.glow-2 {
  bottom: 80px;
  right: -80px;
  width: 220px;
  height: 220px;
  background: var(--color-primary-soft);
  opacity: 0.1;
  animation-delay: 3s;
}

.result-content {
  max-width: 480px;
  margin: 0 auto;
  position: relative;
  z-index: 1;
}

/* ===== Header ===== */
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

/* ===== Params Card (轻量,留父组件) ===== */
.params-card {
  background: var(--color-surface-lowest);
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
  background: var(--color-cyan);
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

/* ponytail: 食物卡片 / 照片管理 / 评价 / Share+Publish / 重新推荐 样式
   已迁移到 ResultFoodCard / ResultPhotoManager / ResultComment /
   ResultActions / ResultRecommendation 子组件 */

/* ===== Responsive ===== */
@media (min-width: 640px) {
  .result-page {
    padding: 60px 40px 80px;
  }
}

/* ===== 共享 Modal Overlay ===== */
/* :deep() 让子组件 ResultActions (publish dialog) 和 ResultPhotoManager
   (全屏 photo preview) 共用同一份 overlay 样式,避免 100 行重复 CSS。 */
:deep(.photo-modal) {
  position: fixed;
  inset: 0;
  z-index: 200;
  background: rgba(0, 0, 0, 0.85);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}
</style>

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

      <!-- Photo section -->
      <template v-if="sessionId">
        <!-- Already uploaded thumbnail -->
        <div v-if="uploadedPhoto && !showUpload" class="uploaded-photo-card animate-fade-up delay-450 animate-start-hidden">
          <div class="photo-header">
            <div class="upload-label">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14.5 4h-5L7 7H4a2 2 0 0 0-2 2v9a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2V9a2 2 0 0 0-2-2h-3l-2.5-3z"/><circle cx="12" cy="13" r="3"/></svg>
              <span>美食照片</span>
            </div>
            <div class="photo-actions">
              <button class="photo-action-btn replace" @click="showUpload = true" title="更换照片">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" x2="12" y1="3" y2="15"/></svg>
              </button>
              <button class="photo-action-btn delete" @click="handleDeletePhoto" title="删除照片">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M3 6h18"/><path d="M19 6v14c0 1-1 2-2 2H7c-1 0-2-1-2-2V6"/><path d="M8 6V4c0-1 1-2 2-2h4c1 0 2 1 2 2v2"/></svg>
              </button>
            </div>
          </div>
          <img :src="uploadedPhoto.thumbnailUrl" class="photo-thumbnail" alt="美食照片" @click="openFullPhoto" />
        </div>

        <!-- Upload / Re-upload component -->
        <UploadPhoto
          v-if="!uploadedPhoto || showUpload"
          :session-id="sessionId"
          @uploaded="onPhotoUploaded"
        />
      </template>

      <!-- Comment section -->
      <div v-if="sessionId" class="comment-section animate-fade-up delay-480 animate-start-hidden">
        <div class="comment-label">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>
          <span>美食评价</span>
        </div>
        <div class="comment-edit">
          <textarea
            v-model="commentInput"
            class="comment-textarea"
            placeholder="写下你对这道美食的评价..."
            rows="3"
            maxlength="500"
          ></textarea>
          <div class="comment-actions">
            <button class="emoji-trigger" @click="openEmoji('comment')">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><path d="M8 14s1.5 2 4 2 4-2 4-2"/><line x1="9" x2="9.01" y1="9" y2="9"/><line x1="15" x2="15.01" y1="9" y2="9"/></svg>
            </button>
            <span class="comment-count">{{ (commentInput || '').length }}/500</span>
            <button class="comment-save-btn" @click="saveComment" :disabled="savingComment">
              {{ savingComment ? '保存中...' : '保存评价' }}
            </button>
          </div>
        </div>
      </div>

    <!-- Emoji Picker -->
    <EmojiPicker :show="showEmoji" @select="insertEmoji" @close="showEmoji = false" />

      <!-- Share section -->
      <div v-if="sessionId" class="share-section animate-fade-up delay-490 animate-start-hidden">
        <div class="share-card" v-if="!shareUrl">
          <button class="share-btn" @click="handleShare" :disabled="sharing">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="18" cy="5" r="3"/><circle cx="6" cy="12" r="3"/><circle cx="18" cy="19" r="3"/><line x1="8.59" x2="15.42" y1="13.51" y2="17.49"/><line x1="15.41" x2="8.59" y1="6.51" y2="10.49"/></svg>
            {{ sharing ? '创建中...' : '分享此美食' }}
          </button>
        </div>
        <div class="share-link-card" v-else>
          <div class="share-link-label">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="18" cy="5" r="3"/><circle cx="6" cy="12" r="3"/><circle cx="18" cy="19" r="3"/><line x1="8.59" x2="15.42" y1="13.51" y2="17.49"/><line x1="15.41" x2="8.59" y1="6.51" y2="10.49"/></svg>
            <span>分享链接</span>
          </div>
          <div class="share-link-row">
            <input class="share-link-input" :value="shareUrl" readonly />
            <button class="share-copy-btn" @click="copyShareUrl">复制链接</button>
          </div>
        </div>
      </div>

      <!-- Publish to feed section -->
      <div v-if="sessionId" class="publish-section animate-fade-up delay-495 animate-start-hidden">
        <div v-if="!isPublished" class="publish-card">
          <button class="publish-btn" @click="openPublishDialog">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" x2="12" y1="3" y2="15"/></svg>
            发布到大厅
          </button>
        </div>
        <div v-else class="publish-card">
          <button class="unpublish-btn" @click="handleUnpublish" :disabled="unpublishing">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" x2="12" y1="3" y2="15"/></svg>
            {{ unpublishing ? '取消中...' : '取消发布' }}
          </button>
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

    <!-- Full photo modal -->
    <Transition name="fade">
      <div v-if="showFullPhoto && uploadedPhoto" class="photo-modal" @click.self="showFullPhoto = false">
        <div class="photo-modal-content">
          <img :src="uploadedPhoto.originalUrl" class="full-photo" alt="原始照片" />
          <button class="photo-modal-close" @click="showFullPhoto = false">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M18 6 6 18"/><path d="m6 6 12 12"/></svg>
          </button>
        </div>
      </div>
    </Transition>

    <!-- Publish dialog -->
    <Transition name="fade">
      <div v-if="publishDialog" class="photo-modal" @click.self="publishDialog = false">
        <div class="publish-dialog">
          <div class="publish-dialog-title">发布到大厅</div>
          <p class="publish-dialog-hint">编辑评论预览（展示在大厅卡片上，最多30字）</p>
          <textarea
            v-model="publishPreview"
            class="publish-textarea"
            placeholder="写下你的推荐感言..."
            rows="3"
            maxlength="30"
          ></textarea>
          <div class="publish-dialog-count">
            <button class="emoji-trigger" @click="openEmoji('publish')">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><path d="M8 14s1.5 2 4 2 4-2 4-2"/><line x1="9" x2="9.01" y1="9" y2="9"/><line x1="15" x2="15.01" y1="9" y2="9"/></svg>
            </button>
            <span>{{ (publishPreview || '').length }}/30</span>
          </div>
          <div class="publish-dialog-actions">
            <button class="publish-cancel-btn" @click="publishDialog = false">取消</button>
            <button class="publish-confirm-btn" @click="handlePublish" :disabled="publishing">
              {{ publishing ? '发布中...' : '确认发布' }}
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useChatStore } from '@/stores/chat'
import { recordApi, shareApi, feedApi } from '@/api'
import { showSuccess, showError } from '@/utils/toast'
import UploadPhoto from '@/components/UploadPhoto.vue'
import EmojiPicker from '@/components/EmojiPicker.vue'

const router = useRouter()
const chatStore = useChatStore()

const showEmoji = ref(false)
const emojiTarget = ref<'comment' | 'publish'>('comment')

function insertEmoji(icon: string) {
  if (emojiTarget.value === 'comment') {
    commentInput.value = (commentInput.value || '') + icon
  } else {
    publishPreview.value = (publishPreview.value || '') + icon
  }
  showEmoji.value = false
}

function openEmoji(target: 'comment' | 'publish') {
  emojiTarget.value = target
  showEmoji.value = !showEmoji.value
}
const loading = ref(true)
const pendingData = ref<any>(null)
const uploadedPhoto = ref<{ thumbnailUrl: string; originalUrl: string } | null>(null)
const showFullPhoto = ref(false)
const showUpload = ref(false)
const commentInput = ref('')
const savingComment = ref(false)
const shareUrl = ref('')
const sharing = ref(false)
const isPublished = ref(false)
const publishDialog = ref(false)
const publishPreview = ref('')
const publishing = ref(false)
const unpublishing = ref(false)

onMounted(async () => {
  try {
    // Check if navigated from Feed reminder (sessionId passed via store, not URL)
    const pendingSid = chatStore.pendingSessionId
    if (pendingSid) {
      try {
        const detail = await recordApi.getRecordDetail(pendingSid)
        if (detail) {
          pendingData.value = {
            sessionId: pendingSid,
            content: detail.recommendation ? JSON.stringify({
              foodName: detail.recommendation.foodName,
              reason: detail.recommendation.reason
            }) : null,
            paramValues: detail.collectedParams
          }
          if (detail.recommendation) {
            chatStore.recommendationResult = JSON.stringify({
              foodName: detail.recommendation.foodName,
              reason: detail.recommendation.reason
            })
            chatStore.collectedParamValues = detail.collectedParams || []
          }
          chatStore.sessionId = pendingSid
          if (detail.photo) {
            uploadedPhoto.value = {
              thumbnailUrl: detail.photo.thumbnailPath,
              originalUrl: detail.photo.originalPath
            }
          }
        }
      } catch (e: any) {
        showError(e?.message || '记录不存在或已被删除')
        router.back()
        return
      } finally {
        chatStore.pendingSessionId = ''
      }
    } else {
      // Try fetching pending recommendation from backend
      const res = await recordApi.getPendingRecommendation()
      if (res?.hasPending && res.sessionId) {
        pendingData.value = res
        chatStore.sessionId = res.sessionId
      } else if (chatStore.recommendationResult) {
        // Fallback to chat store data
        pendingData.value = {
          sessionId: chatStore.sessionId,
          content: chatStore.recommendationResult
        }
      }
    }
    // Check if already published
    const sid = sessionId.value
    if (sid) {
      try {
        const pubRes = await feedApi.checkPublished(sid)
        isPublished.value = pubRes?.published || false
      } catch { /* ignore */ }
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

const sessionId = computed(() => {
  return pendingData.value?.sessionId || chatStore.sessionId
})

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

function onPhotoUploaded(data: { thumbnailUrl: string; originalUrl: string }) {
  uploadedPhoto.value = data
  showUpload.value = false
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
    showUpload.value = false
    showSuccess('照片已删除')
  } catch {
    showError('删除失败')
  }
}

async function saveComment() {
  const sid = sessionId.value
  if (!sid) return
  savingComment.value = true
  try {
    await recordApi.updateComment(sid, commentInput.value || '')
    showSuccess('评价已保存')
  } catch {
    showError('保存失败')
  } finally {
    savingComment.value = false
  }
}

function openFullPhoto() {
  showFullPhoto.value = true
}

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

function openPublishDialog() {
  const resultText = chatStore.recommendationResult
  try {
    const parsed = JSON.parse(resultText || '{}')
    publishPreview.value = parsed.reason ? parsed.reason.substring(0, 30) : ''
  } catch {
    publishPreview.value = ''
  }
  publishDialog.value = true
}

async function handlePublish() {
  const sid = sessionId.value
  if (!sid) return
  publishing.value = true
  try {
    await feedApi.publish({ sessionId: sid, commentPreview: publishPreview.value || undefined })
    isPublished.value = true
    publishDialog.value = false
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

@keyframes spin {
  to { transform: rotate(360deg); }
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

/* Uploaded photo */
.uploaded-photo-card {
  margin-bottom: 20px;
}

.photo-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.photo-actions {
  display: flex;
  gap: 8px;
}

.photo-action-btn {
  width: 32px;
  height: 32px;
  border: none;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;

  &.replace {
    background: rgba(0, 89, 182, 0.08);
    color: var(--color-primary);
    &:active { background: rgba(0, 89, 182, 0.15); }
  }

  &.delete {
    background: rgba(239, 68, 68, 0.08);
    color: #ef4444;
    &:active { background: rgba(239, 68, 68, 0.15); }
  }
}

.upload-label {
  display: flex;
  align-items: center;
  gap: 8px;
  font-family: var(--font-serif);
  font-style: italic;
  font-size: 16px;
  font-weight: 500;
  color: var(--color-on-surface);
  margin-bottom: 12px;

  svg {
    color: var(--color-primary);
  }
}

.photo-thumbnail {
  width: 100%;
  border-radius: 1.5rem;
  display: block;
  max-height: 300px;
  object-fit: cover;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  cursor: pointer;
  transition: transform 0.3s cubic-bezier(0.22, 1, 0.36, 1);

  &:hover {
    transform: scale(1.01);
  }
}

/* Photo modal */
.photo-modal {
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

.photo-modal-content {
  position: relative;
  max-width: 100%;
  max-height: 100%;
}

.full-photo {
  max-width: 100%;
  max-height: 85vh;
  border-radius: 1.5rem;
  object-fit: contain;
}

.photo-modal-close {
  position: absolute;
  top: -12px;
  right: -12px;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  border: none;
  background: rgba(255, 255, 255, 0.15);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: background 0.2s;

  &:hover {
    background: rgba(255, 255, 255, 0.3);
  }
}

/* Comment section */
.comment-section {
  margin-bottom: 20px;
  z-index: 1;
}

.comment-label {
  display: flex;
  align-items: center;
  gap: 8px;
  font-family: var(--font-serif);
  font-style: italic;
  font-size: 16px;
  font-weight: 500;
  color: var(--color-on-surface);
  margin-bottom: 12px;

  svg { color: var(--color-primary); }
}

.comment-edit {
  padding: 4px;
  background: var(--color-surface-container-lowest);
  border-radius: 1.25rem;
  border: 1px solid var(--color-surface-container-low);
}

.comment-textarea {
  width: 100%;
  padding: 14px 18px;
  border: none;
  background: none;
  font-family: var(--font-sans);
  font-size: 14px;
  line-height: 1.6;
  color: var(--color-on-surface);
  resize: none;
  outline: none;
  &::placeholder { color: var(--color-on-surface-variant); opacity: 0.5; }
}

.comment-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 18px 12px;
}

.emoji-trigger {
  width: 36px; height: 36px;
  display: flex; align-items: center; justify-content: center;
  background: none; border: none; color: var(--color-on-surface-variant);
  cursor: pointer; border-radius: 50%; flex-shrink: 0;
  transition: background 0.2s;
  &:active { background: var(--color-surface-container-low); }
}

.comment-count {
  font-size: 11px;
  color: var(--color-on-surface-variant);
  opacity: 0.5;
}

.comment-save-btn {
  padding: 8px 20px;
  border: none;
  border-radius: 100px;
  background: linear-gradient(135deg, var(--color-primary-container), var(--color-primary));
  color: white;
  font-family: var(--font-sans);
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  box-shadow: 0 4px 12px rgba(0, 89, 182, 0.2);
  transition: all 0.2s;
  &:hover { transform: translateY(-1px); }
  &:active { transform: scale(0.97); }
  &:disabled { opacity: 0.5; cursor: not-allowed; transform: none; }
}

/* Actions */
.actions {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 20px;
}

/* Share section */
.share-section {
  margin-bottom: 4px;
  z-index: 1;
}

.share-card {
  display: flex;
}

.share-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 12px 24px;
  border: 1.5px solid var(--color-primary);
  border-radius: 2rem;
  background: none;
  color: var(--color-primary);
  font-family: var(--font-sans);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    background: rgba(0, 89, 182, 0.06);
  }

  &:active {
    transform: scale(0.97);
  }

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
}

.share-link-card {
  background: var(--color-surface-container-lowest);
  border-radius: 1.25rem;
  padding: 16px 20px;
  border: 1px solid var(--color-surface-container-low);
  width: 100%;
}

.share-link-label {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  font-weight: 600;
  color: var(--color-on-surface-variant);
  margin-bottom: 12px;

  svg {
    color: var(--color-primary);
  }
}

.share-link-row {
  display: flex;
  gap: 8px;
  align-items: center;
}

.share-link-input {
  flex: 1;
  padding: 10px 14px;
  border: 1px solid var(--color-surface-container-low);
  border-radius: 1rem;
  background: var(--color-surface);
  font-family: var(--font-sans);
  font-size: 12px;
  color: var(--color-on-surface);
  outline: none;
  min-width: 0;
}

.share-copy-btn {
  flex-shrink: 0;
  padding: 10px 16px;
  border: none;
  border-radius: 1rem;
  background: linear-gradient(135deg, var(--color-primary-container), var(--color-primary));
  color: white;
  font-family: var(--font-sans);
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;

  &:hover {
    transform: translateY(-1px);
  }

  &:active {
    transform: scale(0.97);
  }
}

/* Publish section */
.publish-section {
  margin-bottom: 4px;
  z-index: 1;
}

.publish-card {
  display: flex;
}

.publish-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 12px 24px;
  border: 1.5px solid #22d3ee;
  border-radius: 2rem;
  background: none;
  color: #22d3ee;
  font-family: var(--font-sans);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    background: rgba(34, 211, 238, 0.06);
  }

  &:active {
    transform: scale(0.97);
  }
}

.unpublish-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 12px 24px;
  border: 1.5px solid rgba(239, 68, 68, 0.4);
  border-radius: 2rem;
  background: rgba(239, 68, 68, 0.06);
  color: #ef4444;
  font-family: var(--font-sans);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    background: rgba(239, 68, 68, 0.12);
  }

  &:active {
    transform: scale(0.97);
  }

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
}

.publish-dialog {
  width: calc(100% - 48px);
  max-width: 400px;
  background: var(--color-surface-container-lowest);
  border-radius: 2rem;
  padding: 28px 24px;
}

.publish-dialog-title {
  font-family: var(--font-serif);
  font-style: italic;
  font-size: 22px;
  font-weight: 500;
  color: var(--color-on-surface);
  margin-bottom: 8px;
}

.publish-dialog-hint {
  font-size: 12px;
  color: var(--color-on-surface-variant);
  margin-bottom: 16px;
  opacity: 0.7;
}

.publish-textarea {
  width: 100%;
  padding: 14px 18px;
  border: 1.5px solid var(--color-surface-container-low);
  border-radius: 1.25rem;
  background: var(--color-surface);
  font-family: var(--font-sans);
  font-size: 14px;
  line-height: 1.6;
  color: var(--color-on-surface);
  resize: none;
  outline: none;

  &:focus {
    border-color: var(--color-primary);
  }

  &::placeholder {
    color: var(--color-on-surface-variant);
    opacity: 0.5;
  }
}

.publish-dialog-count {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 4px;
  font-size: 11px;
  color: var(--color-on-surface-variant);
  opacity: 0.5;
  margin-top: 6px;
  margin-bottom: 16px;
}

.publish-dialog-actions {
  display: flex;
  gap: 10px;
}

.publish-cancel-btn {
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

.publish-confirm-btn {
  flex: 1;
  padding: 12px;
  border: none;
  border-radius: 1rem;
  background: linear-gradient(135deg, #22d3ee, #0891b2);
  color: white;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
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

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.25s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

@media (min-width: 640px) {
  .result-page {
    padding: 60px 40px 80px;
  }
}
</style>

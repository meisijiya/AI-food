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

      <!-- Photo section -->
      <template v-if="detail.recommendation">
        <!-- Already uploaded photo with thumbnail -->
        <div v-if="detail.photo && !showUpload" class="photo-card animate-fade-up delay-150 animate-start-hidden">
          <div class="photo-header">
            <div class="photo-label">
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
          <img
            :src="detail.photo.thumbnailPath"
            class="photo-image"
            alt="美食照片"
            @click="openPhotoModal(detail.photo.originalPath)"
          />
        </div>

        <!-- Upload / Re-upload -->
        <UploadPhoto
          v-if="!detail.photo || showUpload"
          :session-id="sessionId"
          @uploaded="onPhotoUploaded"
        />
      </template>

      <!-- Comment section -->
      <div v-if="detail.recommendation" class="comment-section animate-fade-up delay-180 animate-start-hidden">
        <div class="comment-label">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>
          <span>美食评价</span>
        </div>
        <div v-if="!editingComment && commentText" class="comment-display">
          <p class="comment-text">{{ commentText }}</p>
          <button class="comment-edit-btn" @click="startEditComment">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M17 3a2.85 2.83 0 1 1 4 4L7.5 20.5 2 22l1.5-5.5Z"/></svg>
          </button>
        </div>
        <div v-else class="comment-edit">
          <textarea
            v-model="commentInput"
            class="comment-textarea"
            placeholder="写下你对这道美食的评价..."
            rows="3"
            maxlength="500"
          ></textarea>
          <div class="comment-actions">
            <span class="comment-count">{{ (commentInput || '').length }}/500</span>
            <button class="comment-save-btn" @click="saveComment" :disabled="savingComment">
              {{ savingComment ? '保存中...' : '保存评价' }}
            </button>
          </div>
        </div>
      </div>

      <!-- Share section -->
      <div v-if="sessionId" class="share-section animate-fade-up delay-190 animate-start-hidden">
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
      <div v-if="sessionId && detail.recommendation" class="publish-section animate-fade-up delay-195 animate-start-hidden">
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

    <!-- Photo modal -->
    <Transition name="fade">
      <div v-if="photoModalUrl" class="photo-modal" @click.self="photoModalUrl = null">
        <div class="photo-modal-content">
          <img :src="photoModalUrl" class="full-photo" alt="原始照片" />
          <button class="photo-modal-close" @click="photoModalUrl = null">
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
          <div class="publish-dialog-count">{{ (publishPreview || '').length }}/30</div>
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
import { useRoute, useRouter } from 'vue-router'
import { recordApi, shareApi, feedApi } from '@/api'
import { showSuccess, showError } from '@/utils/toast'
import UploadPhoto from '@/components/UploadPhoto.vue'

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const detail = ref<any>(null)
const photoModalUrl = ref<string | null>(null)
const showUpload = ref(false)
const editingComment = ref(false)
const commentInput = ref('')
const savingComment = ref(false)
const shareUrl = ref('')
const sharing = ref(false)
const isPublished = ref(false)
const publishDialog = ref(false)
const publishPreview = ref('')
const publishing = ref(false)
const unpublishing = ref(false)

const sessionId = computed(() => route.params.sessionId as string)

const commentText = computed(() => {
  return detail.value?.recommendation?.comment || ''
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

function paramLabel(key: string) {
  return paramLabels[key] || key
}

function formatDate(dateStr: string) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

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
  const sid = route.params.sessionId as string
  if (!sid) {
    loading.value = false
    return
  }

  try {
    const res = await recordApi.getRecordDetail(sid)
    detail.value = res
    // 如果没有评价，默认进入编辑模式
    if (!res?.recommendation?.comment) {
      editingComment.value = true
    }
  } catch {
    // ignore
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await fetchDetail()
  const sid = sessionId.value
  if (sid) {
    try {
      const res = await feedApi.checkPublished(sid)
      isPublished.value = res?.published || false
    } catch { /* ignore */ }
  }
})

async function openPublishDialog() {
  const sid = sessionId.value
  if (!sid) return
  publishPreview.value = commentText.value || ''
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

function startEditComment() {
  commentInput.value = commentText.value
  editingComment.value = true
}

async function saveComment() {
  const sid = sessionId.value
  if (!sid) return
  savingComment.value = true
  try {
    await recordApi.updateComment(sid, commentInput.value || '')
    if (detail.value?.recommendation) {
      detail.value.recommendation.comment = commentInput.value
    }
    editingComment.value = false
    showSuccess('评价已保存')
  } catch {
    showError('保存失败')
  } finally {
    savingComment.value = false
  }
}

function onPhotoUploaded(data: { thumbnailUrl: string; originalUrl: string }) {
  if (detail.value) {
    detail.value.photo = {
      thumbnailPath: data.thumbnailUrl,
      originalPath: data.originalUrl
    }
  }
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
    if (detail.value) {
      detail.value.photo = null
    }
    showUpload.value = false
    showSuccess('照片已删除')
  } catch {
    showError('删除失败')
  }
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

function openPhotoModal(url: string) {
  photoModalUrl.value = url
}
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

/* Photo card */
.photo-card {
  margin-bottom: 20px;
  z-index: 1;
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

.photo-label {
  display: flex;
  align-items: center;
  gap: 8px;
  font-family: var(--font-serif);
  font-style: italic;
  font-size: 16px;
  font-weight: 500;
  color: var(--color-on-surface);

  svg {
    color: var(--color-primary);
  }
}

.photo-image {
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

  svg {
    color: var(--color-primary);
  }
}

.comment-display {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 16px 20px;
  background: var(--color-surface-container-lowest);
  border-radius: 1.25rem;
  border: 1px solid rgba(255, 255, 255, 0.8);
}

.comment-text {
  flex: 1;
  font-size: 14px;
  line-height: 1.7;
  color: var(--color-on-surface);
  white-space: pre-wrap;
}

.comment-edit-btn {
  flex-shrink: 0;
  width: 32px;
  height: 32px;
  border: none;
  border-radius: 50%;
  background: rgba(0, 89, 182, 0.08);
  color: var(--color-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;

  &:active { background: rgba(0, 89, 182, 0.15); }
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

  &::placeholder {
    color: var(--color-on-surface-variant);
    opacity: 0.5;
  }
}

.comment-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 18px 12px;
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

/* Params */
.params-section {
  margin-bottom: 20px;
  z-index: 1;
}

/* Share section */
.share-section {
  margin-bottom: 20px;
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
  margin-bottom: 20px;
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
  text-align: right;
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

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.25s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>

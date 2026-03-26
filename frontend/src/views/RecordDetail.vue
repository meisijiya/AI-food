<template>
  <div class="detail-container">
    <div class="bg-glow bg-glow-1"></div>
    <div class="bg-glow bg-glow-2"></div>

    <!-- Loading -->
    <div v-if="loading" class="loading-state">
      <div class="loading-spinner"></div>
    </div>

    <template v-if="detail">
      <!-- Recommendation card (light) -->
      <div class="recommend-card card-enter card-delay-1">
        <div class="recommend-accent"></div>
        <div class="recommend-glow"></div>
        <div class="recommend-food">{{ foodName }}</div>
        <div class="recommend-reason" v-if="reason">{{ reason }}</div>
        <div v-if="detail.session" class="recommend-meta">
          <span v-if="detail.session.mode">{{ detail.session.mode === 'inertia' ? '惯性模式' : '随机模式' }}</span>
          <span v-if="detail.session.totalQuestions">{{ detail.session.currentQuestionCount }}/{{ detail.session.totalQuestions }} 轮</span>
          <span v-if="detail.session.createdAt">{{ formatDate(detail.session.createdAt) }}</span>
        </div>
      </div>

      <!-- Photo section -->
      <template v-if="detail.recommendation">
        <div v-if="detail.photo && !showUpload" class="photo-card card-enter card-delay-2">
          <div class="section-title">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14.5 4h-5L7 7H4a2 2 0 0 0-2 2v9a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2V9a2 2 0 0 0-2-2h-3l-2.5-3z"/><circle cx="12" cy="13" r="3"/></svg>
            <span>美食照片</span>
          </div>
          <div class="photo-body">
            <div class="photo-glow"></div>
            <img
              :src="detail.photo.thumbnailPath"
              class="photo-image"
              alt="美食照片"
              @click="openPhotoModal(detail.photo.originalPath)"
            />
            <div class="photo-actions">
              <button class="photo-action-btn" @click="showUpload = true" title="更换照片">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" x2="12" y1="3" y2="15"/></svg>
              </button>
              <button class="photo-action-btn delete" @click="handleDeletePhoto" title="删除照片">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M3 6h18"/><path d="M19 6v14c0 1-1 2-2 2H7c-1 0-2-1-2-2V6"/><path d="M8 6V4c0-1 1-2 2-2h4c1 0 2 1 2 2v2"/></svg>
              </button>
            </div>
          </div>
        </div>

        <UploadPhoto
          v-if="!detail.photo || showUpload"
          :session-id="sessionId"
          @uploaded="onPhotoUploaded"
        />
      </template>

      <!-- Comment section -->
      <div v-if="detail.recommendation" class="comment-section card-enter card-delay-3">
        <div class="section-title">
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

      <EmojiPicker :show="showEmoji" @select="insertEmoji" @close="showEmoji = false" />

      <!-- Actions row: Share + Publish -->
      <div class="actions-row card-enter card-delay-4">
        <!-- Share -->
        <div v-if="sessionId && !shareUrl" class="action-chip">
          <button class="chip-btn share-chip" @click="handleShare" :disabled="sharing">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="18" cy="5" r="3"/><circle cx="6" cy="12" r="3"/><circle cx="18" cy="19" r="3"/><line x1="8.59" x2="15.42" y1="13.51" y2="17.49"/><line x1="15.41" x2="8.59" y1="6.51" y2="10.49"/></svg>
            {{ sharing ? '创建中...' : '分享' }}
          </button>
        </div>
        <div v-if="sessionId && detail.recommendation && !isPublished" class="action-chip">
          <button class="chip-btn publish-chip" @click="openPublishDialog">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" x2="12" y1="3" y2="15"/></svg>
            发布到大厅
          </button>
        </div>
        <div v-if="sessionId && detail.recommendation && isPublished" class="action-chip">
          <button class="chip-btn unpublish-chip" @click="handleUnpublish" :disabled="unpublishing">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M18 6 6 18"/><path d="m6 6 12 12"/></svg>
            {{ unpublishing ? '取消中...' : '取消发布' }}
          </button>
        </div>
      </div>

      <!-- Share link (after copy) -->
      <div v-if="shareUrl" class="share-link-card card-enter">
        <div class="section-title">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="18" cy="5" r="3"/><circle cx="6" cy="12" r="3"/><circle cx="18" cy="19" r="3"/><line x1="8.59" x2="15.42" y1="13.51" y2="17.49"/><line x1="15.41" x2="8.59" y1="6.51" y2="10.49"/></svg>
          <span>分享链接</span>
        </div>
        <div class="share-link-row">
          <input class="share-link-input" :value="shareUrl" readonly />
          <button class="share-copy-btn" @click="copyShareUrl">复制</button>
        </div>
      </div>

      <!-- Collected params -->
      <div v-if="paramsList.length" class="params-section card-enter card-delay-5">
        <div class="section-label">收集信息</div>
        <div class="params-chips">
          <span v-for="p in paramsList" :key="p.paramName" class="param-chip">
            <span class="param-key">{{ paramLabel(p.paramName) }}</span>
            <span class="param-val">{{ p.paramValue }}</span>
          </span>
        </div>
      </div>

      <!-- Q&A timeline -->
      <div v-if="qaList.length" class="timeline-section card-enter card-delay-6">
        <div class="section-label">对话记录</div>
        <div class="timeline">
          <template v-for="(qa, index) in qaList" :key="index">
            <div class="timeline-item">
              <div class="timeline-dot assistant"></div>
              <div class="timeline-line"></div>
              <div class="timeline-content">
                <div class="timeline-role">AI</div>
                <div class="timeline-text">{{ qa.aiQuestion }}</div>
              </div>
            </div>
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
    </template>

    <!-- Back button -->
    <button class="back-btn card-enter card-delay-7" @click="router.back()">
      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m12 19-7-7 7-7"/><path d="M19 12H5"/></svg>
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
          <div class="publish-dialog-glow"></div>
          <div class="publish-dialog-title">发布动态</div>
          <div class="publish-visibility-row">
            <button
              class="visibility-option"
              :class="{ active: publishVisibility === 'public' }"
              @click="publishVisibility = 'public'"
            >
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="2" x2="22" y1="12" y2="12"/><path d="M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10 15.3 15.3 0 0 1 4-10z"/></svg>
              <span>发布到大厅</span>
            </button>
            <button
              class="visibility-option"
              :class="{ active: publishVisibility === 'friends' }"
              @click="publishVisibility = 'friends'"
            >
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M22 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>
              <span>仅粉丝可见</span>
            </button>
          </div>
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
import { useRoute, useRouter } from 'vue-router'
import { recordApi, shareApi, feedApi } from '@/api'
import { showSuccess, showError } from '@/utils/toast'
import UploadPhoto from '@/components/UploadPhoto.vue'
import EmojiPicker from '@/components/EmojiPicker.vue'

const route = useRoute()
const router = useRouter()

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
const publishVisibility = ref<'public' | 'friends'>('public')
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
  } catch (e: any) {
    showError(e?.message || '记录不存在或已被删除')
    router.back()
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
  publishVisibility.value = 'public'
  publishDialog.value = true
}

async function handlePublish() {
  const sid = sessionId.value
  if (!sid) return
  publishing.value = true
  try {
    await feedApi.publish({ sessionId: sid, commentPreview: publishPreview.value || undefined, visibility: publishVisibility.value })
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
/* ===== Container ===== */
.detail-container {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: var(--color-surface);
  padding: 40px 20px 100px;
  position: relative;
  overflow-y: auto;
}

/* ===== Background Glows ===== */
.bg-glow {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  pointer-events: none;
}

.bg-glow-1 {
  top: -80px;
  right: -60px;
  width: 280px;
  height: 280px;
  background: var(--color-primary-container);
  opacity: 0.1;
  animation: glow-drift 8s ease-in-out infinite;
}

.bg-glow-2 {
  bottom: 200px;
  left: -80px;
  width: 200px;
  height: 200px;
  background: var(--color-secondary-fixed);
  opacity: 0.06;
  animation: glow-drift 10s ease-in-out infinite reverse;
}

@keyframes glow-drift {
  0%, 100% { transform: translate(0, 0); }
  50% { transform: translate(10px, -10px); }
}

/* ===== Card Entrance Animation ===== */
.card-enter {
  animation: card-slide-up 0.5s cubic-bezier(0.22, 1, 0.36, 1) both;
}

.card-delay-1 { animation-delay: 0.05s; }
.card-delay-2 { animation-delay: 0.1s; }
.card-delay-3 { animation-delay: 0.15s; }
.card-delay-4 { animation-delay: 0.2s; }
.card-delay-5 { animation-delay: 0.25s; }
.card-delay-6 { animation-delay: 0.3s; }
.card-delay-7 { animation-delay: 0.35s; }

@keyframes card-slide-up {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}

/* ===== Loading ===== */
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

/* ===== Page Title ===== */
.page-title {
  font-family: var(--font-serif);
  font-style: italic;
  font-size: 36px;
  font-weight: 500;
  color: var(--color-on-surface);
  margin-bottom: 28px;
  letter-spacing: -0.01em;

  em {
    font-style: italic;
    color: var(--color-primary);
  }
}

/* ===== Recommend Card (Light) ===== */
.recommend-card {
  background: var(--color-surface-container-lowest);
  border: 1px solid rgba(255, 255, 255, 0.8);
  border-radius: 2.5rem;
  padding: 32px 28px;
  margin-bottom: 24px;
  position: relative;
  overflow: hidden;
  box-shadow: 0 8px 32px rgba(0, 89, 182, 0.06);
  transition: box-shadow 0.4s ease, transform 0.4s cubic-bezier(0.22, 1, 0.36, 1);

  &:hover {
    box-shadow: 0 12px 40px rgba(0, 89, 182, 0.1);
    transform: translateY(-2px);
  }
}

.recommend-accent {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: linear-gradient(90deg, var(--color-primary-container), var(--color-primary), var(--color-secondary-fixed));
  border-radius: 2.5rem 2.5rem 0 0;
}

.recommend-glow {
  position: absolute;
  top: -60px;
  right: -40px;
  width: 200px;
  height: 200px;
  background: var(--color-secondary-fixed);
  opacity: 0.08;
  border-radius: 50%;
  filter: blur(50px);
  pointer-events: none;
}

.recommend-food {
  font-family: var(--font-serif);
  font-style: italic;
  font-size: 28px;
  font-weight: 500;
  color: var(--color-primary);
  margin-bottom: 12px;
  position: relative;
  z-index: 1;
}

.recommend-reason {
  font-size: 14px;
  line-height: 1.9;
  color: var(--color-on-surface);
  opacity: 0.75;
  position: relative;
  z-index: 1;
}

.recommend-meta {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin-top: 16px;
  position: relative;
  z-index: 1;

  span {
    font-size: 10px;
    font-weight: 700;
    text-transform: uppercase;
    letter-spacing: 0.15em;
    color: var(--color-on-surface-variant);
    padding: 5px 14px;
    background: var(--color-surface-container-low);
    border-radius: 100px;
  }
}

/* ===== Section Title (shared) ===== */
.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-family: var(--font-serif);
  font-style: italic;
  font-size: 18px;
  font-weight: 500;
  color: var(--color-on-surface);
  margin-bottom: 14px;

  svg {
    color: var(--color-primary);
  }
}

/* ===== Photo Card ===== */
.photo-card {
  margin-bottom: 24px;
}

.photo-body {
  position: relative;
  overflow: hidden;
  border-radius: 2rem;
  background: var(--color-surface-container-lowest);
  border: 1px solid rgba(255, 255, 255, 0.8);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.04);
}

.photo-glow {
  position: absolute;
  top: -40px;
  left: -30px;
  width: 140px;
  height: 140px;
  background: var(--color-primary-container);
  opacity: 0.06;
  border-radius: 50%;
  filter: blur(40px);
  pointer-events: none;
  z-index: 0;
}

.photo-image {
  width: 100%;
  display: block;
  max-height: 360px;
  object-fit: cover;
  cursor: pointer;
  transition: transform 0.5s cubic-bezier(0.22, 1, 0.36, 1);
  position: relative;
  z-index: 1;

  &:hover {
    transform: scale(1.02);
  }
}

.photo-actions {
  display: flex;
  gap: 6px;
  position: absolute;
  top: 12px;
  right: 12px;
  z-index: 2;
}

.photo-action-btn {
  width: 36px;
  height: 36px;
  border: none;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.25s;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  color: var(--color-on-surface);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);

  &:hover {
    background: rgba(255, 255, 255, 0.95);
    transform: scale(1.08);
  }

  &:active {
    transform: scale(0.95);
  }

  &.delete {
    color: #ef4444;
    &:hover { background: rgba(255, 240, 240, 0.95); }
  }
}

/* ===== Photo Modal ===== */
.photo-modal {
  position: fixed;
  inset: 0;
  z-index: 200;
  background: rgba(11, 15, 16, 0.7);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
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
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
}

.photo-modal-close {
  position: absolute;
  top: -12px;
  right: -12px;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  border: none;
  background: rgba(255, 255, 255, 0.15);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    background: rgba(255, 255, 255, 0.3);
    transform: scale(1.1);
  }
}

/* ===== Comment Section ===== */
.comment-section {
  margin-bottom: 24px;
}

.comment-display {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 20px 24px;
  background: var(--color-surface-container-lowest);
  border-radius: 2rem;
  border: 1px solid rgba(255, 255, 255, 0.8);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.03);
}

.comment-text {
  flex: 1;
  font-size: 14px;
  line-height: 1.8;
  color: var(--color-on-surface);
  white-space: pre-wrap;
}

.comment-edit-btn {
  flex-shrink: 0;
  width: 36px;
  height: 36px;
  border: none;
  border-radius: 50%;
  background: rgba(0, 89, 182, 0.06);
  color: var(--color-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.25s;

  &:hover { background: rgba(0, 89, 182, 0.12); }
  &:active { transform: scale(0.92); }
}

.comment-edit {
  padding: 4px;
  background: var(--color-surface-container-lowest);
  border-radius: 2rem;
  border: 1px solid var(--color-surface-container-low);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.03);
}

.comment-textarea {
  width: 100%;
  padding: 16px 22px;
  border: none;
  background: none;
  font-family: var(--font-sans);
  font-size: 14px;
  line-height: 1.7;
  color: var(--color-on-surface);
  resize: none;
  outline: none;

  &::placeholder {
    color: var(--color-on-surface-variant);
    opacity: 0.45;
  }
}

.comment-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 22px 14px;
}

.emoji-trigger {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: none;
  border: none;
  color: var(--color-on-surface-variant);
  cursor: pointer;
  border-radius: 50%;
  flex-shrink: 0;
  transition: background 0.2s;
  &:active { background: var(--color-surface-container-low); }
}

.comment-count {
  font-size: 11px;
  color: var(--color-on-surface-variant);
  opacity: 0.45;
  font-variant-numeric: tabular-nums;
}

.comment-save-btn {
  margin-left: auto;
  padding: 10px 24px;
  border: none;
  border-radius: 100px;
  background: linear-gradient(135deg, var(--color-primary-container), var(--color-primary));
  color: white;
  font-family: var(--font-sans);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.05em;
  cursor: pointer;
  box-shadow: 0 4px 16px rgba(0, 89, 182, 0.2);
  transition: all 0.25s;

  &:hover { transform: translateY(-1px); box-shadow: 0 6px 20px rgba(0, 89, 182, 0.3); }
  &:active { transform: scale(0.97); }
  &:disabled { opacity: 0.5; cursor: not-allowed; transform: none; }
}

/* ===== Actions Row ===== */
.actions-row {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 20px;
}

.chip-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 12px 22px;
  border-radius: 2rem;
  font-family: var(--font-sans);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.25s;

  &:active { transform: scale(0.96); }
  &:disabled { opacity: 0.45; cursor: not-allowed; transform: none; }
}

.share-chip {
  border: 1.5px solid var(--color-primary);
  background: none;
  color: var(--color-primary);

  &:hover { background: rgba(0, 89, 182, 0.06); }
}

.publish-chip {
  border: 1.5px solid #06b6d4;
  background: none;
  color: #06b6d4;

  &:hover { background: rgba(6, 182, 212, 0.06); }
}

.unpublish-chip {
  border: 1.5px solid rgba(239, 68, 68, 0.35);
  background: rgba(239, 68, 68, 0.04);
  color: #ef4444;

  &:hover { background: rgba(239, 68, 68, 0.08); }
}

/* ===== Share Link Card ===== */
.share-link-card {
  background: var(--color-surface-container-lowest);
  border-radius: 2rem;
  padding: 20px 24px;
  border: 1px solid rgba(255, 255, 255, 0.8);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.03);
  margin-bottom: 24px;
}

.share-link-row {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-top: 4px;
}

.share-link-input {
  flex: 1;
  padding: 12px 16px;
  border: 1px solid var(--color-surface-container-low);
  border-radius: 1.25rem;
  background: var(--color-surface);
  font-family: var(--font-sans);
  font-size: 12px;
  color: var(--color-on-surface);
  outline: none;
  min-width: 0;
}

.share-copy-btn {
  flex-shrink: 0;
  padding: 12px 20px;
  border: none;
  border-radius: 1.25rem;
  background: linear-gradient(135deg, var(--color-primary-container), var(--color-primary));
  color: white;
  font-family: var(--font-sans);
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.25s;
  white-space: nowrap;

  &:hover { transform: translateY(-1px); }
  &:active { transform: scale(0.97); }
}

/* ===== Params Section ===== */
.params-section {
  margin-bottom: 24px;
}

.section-label {
  font-size: 10px;
  font-weight: 700;
  color: var(--color-on-surface-variant);
  text-transform: uppercase;
  letter-spacing: 0.2em;
  margin-bottom: 14px;
}

.params-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.param-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  background: var(--color-surface-container-lowest);
  border-radius: 100px;
  border: 1px solid var(--color-surface-container-low);
  font-size: 12px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.02);
  transition: box-shadow 0.2s;

  &:hover {
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  }
}

.param-key {
  font-weight: 700;
  color: var(--color-primary);
  font-size: 10px;
  text-transform: uppercase;
  letter-spacing: 0.1em;
}

.param-val {
  color: var(--color-on-surface);
}

/* ===== Timeline ===== */
.timeline-section {
  margin-bottom: 24px;
}

.timeline {
  position: relative;
  padding-left: 20px;
}

.timeline-item {
  position: relative;
  padding-bottom: 16px;
  padding-left: 24px;

  &:last-child {
    padding-bottom: 0;
  }
}

.timeline-dot {
  position: absolute;
  left: -12px;
  top: 6px;
  width: 14px;
  height: 14px;
  border-radius: 50%;
  border: 2.5px solid var(--color-primary);
  background: var(--color-surface);
  z-index: 1;

  &.assistant {
    border-color: var(--color-secondary-fixed);
    background: radial-gradient(circle, var(--color-secondary-fixed) 30%, transparent 30%);
  }

  &.user {
    border-color: var(--color-primary);
    background: radial-gradient(circle, var(--color-primary) 30%, transparent 30%);
  }
}

.timeline-line {
  position: absolute;
  left: -6px;
  top: 22px;
  bottom: 0;
  width: 2px;
  background: linear-gradient(to bottom, var(--color-surface-container-low), transparent);
}

.timeline-content {
  background: var(--color-surface-container-lowest);
  border-radius: 1.5rem;
  padding: 16px 20px;
  border: 1px solid rgba(255, 255, 255, 0.8);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.02);
  transition: box-shadow 0.2s;

  &:hover {
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.05);
  }
}

.timeline-role {
  font-size: 10px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.15em;
  color: var(--color-secondary-fixed);
  margin-bottom: 6px;

  &.user-role {
    color: var(--color-primary);
  }
}

.timeline-text {
  font-size: 13px;
  line-height: 1.8;
  color: var(--color-on-surface);
}

/* ===== Back Button ===== */
.back-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 14px 28px;
  border: 1.5px solid var(--color-surface-container-low);
  border-radius: 2rem;
  background: var(--color-surface-container-lowest);
  color: var(--color-on-surface-variant);
  font-family: var(--font-sans);
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.25s;
  margin-top: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.03);

  &:hover {
    border-color: var(--color-primary);
    color: var(--color-primary);
    transform: translateY(-1px);
    box-shadow: 0 4px 16px rgba(0, 89, 182, 0.08);
  }

  &:active {
    transform: scale(0.98);
  }
}

.nav-spacer {
  height: 80px;
}

/* ===== Publish Dialog ===== */
.publish-dialog {
  width: calc(100% - 48px);
  max-width: 400px;
  background: var(--color-surface-container-lowest);
  border-radius: 2.5rem;
  padding: 32px 28px;
  position: relative;
  overflow: hidden;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.12);
}

.publish-dialog-glow {
  position: absolute;
  top: -60px;
  right: -40px;
  width: 180px;
  height: 180px;
  background: var(--color-primary-container);
  opacity: 0.06;
  border-radius: 50%;
  filter: blur(40px);
  pointer-events: none;
}

.publish-dialog-title {
  font-family: var(--font-serif);
  font-style: italic;
  font-size: 24px;
  font-weight: 500;
  color: var(--color-on-surface);
  margin-bottom: 8px;
  position: relative;
  z-index: 1;
}

.publish-dialog-hint {
  font-size: 12px;
  color: var(--color-on-surface-variant);
  margin-bottom: 18px;
  opacity: 0.65;
  line-height: 1.6;
  position: relative;
  z-index: 1;
}

.publish-visibility-row {
  display: flex;
  gap: 10px;
  margin: 16px 0 12px;
  position: relative;
  z-index: 1;
}

.visibility-option {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 12px 8px;
  border: 1.5px solid var(--color-surface-container-low);
  border-radius: 1rem;
  background: none;
  color: var(--color-on-surface-variant);
  font-family: var(--font-sans);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;

  svg {
    opacity: 0.5;
    transition: all 0.2s;
  }

  &.active {
    background: linear-gradient(135deg, var(--color-primary-container), var(--color-primary));
    border-color: transparent;
    color: white;
    svg {
      opacity: 1;
      stroke: white;
    }
  }

  &:not(.active):active {
    background: var(--color-surface-container-low);
  }
}

.publish-textarea {
  width: 100%;
  padding: 14px 18px;
  border: 1.5px solid var(--color-surface-container-low);
  border-radius: 1.5rem;
  background: var(--color-surface);
  font-family: var(--font-sans);
  font-size: 14px;
  line-height: 1.6;
  color: var(--color-on-surface);
  resize: none;
  outline: none;
  position: relative;
  z-index: 1;

  &:focus {
    border-color: var(--color-primary);
    box-shadow: 0 0 0 3px rgba(0, 89, 182, 0.08);
  }

  &::placeholder {
    color: var(--color-on-surface-variant);
    opacity: 0.4;
  }
}

.publish-dialog-count {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 4px;
  font-size: 11px;
  color: var(--color-on-surface-variant);
  opacity: 0.45;
  margin-top: 6px;
  margin-bottom: 18px;
  font-variant-numeric: tabular-nums;
  position: relative;
  z-index: 1;
}

.publish-dialog-actions {
  display: flex;
  gap: 10px;
  position: relative;
  z-index: 1;
}

.publish-cancel-btn {
  flex: 1;
  padding: 14px;
  border: 1.5px solid var(--color-surface-container-low);
  border-radius: 1.5rem;
  background: none;
  color: var(--color-on-surface-variant);
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    border-color: var(--color-on-surface-variant);
  }
}

.publish-confirm-btn {
  flex: 1;
  padding: 14px;
  border: none;
  border-radius: 1.5rem;
  background: linear-gradient(135deg, var(--color-primary-container), var(--color-primary));
  color: white;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  box-shadow: 0 4px 16px rgba(0, 89, 182, 0.2);
  transition: all 0.25s;

  &:hover {
    transform: translateY(-1px);
    box-shadow: 0 6px 20px rgba(0, 89, 182, 0.3);
  }

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
    transform: none;
  }
}

/* ===== Responsive ===== */
@media (min-width: 1024px) {
  .detail-container {
    max-width: 60%;
    margin: 0 auto;
  }
}

/* ===== Transitions ===== */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>

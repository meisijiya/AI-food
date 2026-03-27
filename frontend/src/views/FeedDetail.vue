<template>
  <div class="detail-page" @click="closePanels">
    <div class="bg-glow bg-glow-1"></div>

    <div v-if="loading" class="loading-state">
      <div class="spinner"></div>
    </div>

    <template v-if="post">
      <!-- Header -->
      <h1 class="page-title animate-fade-up">
        <em>{{ post.foodName }}</em>
        <span v-if="post.visibility === 'friends'" class="detail-fans-badge">仅粉丝可见</span>
      </h1>

      <!-- Photo -->
      <div v-if="post.originalPhotoUrl || post.thumbnailUrl" class="photo-section animate-fade-up delay-100 animate-start-hidden">
        <CachedImage :src="post.originalPhotoUrl || post.thumbnailUrl" :alt="post.foodName" class="detail-photo" @click="openPhotoModal(post.originalPhotoUrl || post.thumbnailUrl)" />
      </div>

      <!-- Reason (dark card) -->
      <div v-if="post.reason" class="reason-card animate-fade-up delay-200 animate-start-hidden">
        <div class="reason-food">{{ post.foodName }}</div>
        <div class="reason-text">{{ post.reason }}</div>
      </div>

      <!-- Collected params -->
      <div v-if="collectedParams.length" class="params-section animate-fade-up delay-250 animate-start-hidden">
        <div class="section-label">收集信息</div>
        <div class="params-grid">
          <div v-for="p in collectedParams" :key="p.name" class="param-item">
            <span class="param-key">{{ paramLabel(p.name) }}</span>
            <span class="param-val">{{ p.value }}</span>
          </div>
        </div>
      </div>

      <!-- User info + Follow + Like -->
      <div class="user-like-row animate-fade-up delay-300 animate-start-hidden">
        <div class="user-info">
            <img v-if="post.avatar" :src="post.avatar" class="user-avatar" alt="" />
          <div v-else class="user-avatar-placeholder">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M19 21v-2a4 4 0 0 0-4-4H9a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
          </div>
          <div>
            <div class="user-name">{{ post.nickname || '匿名用户' }}</div>
            <div class="post-time">{{ formatDate(post.publishedAt) }}</div>
          </div>
        </div>
        <div class="user-actions">
          <button
            v-if="!isOwnPost"
            class="follow-btn"
            :class="{ following: isFollowing }"
            :disabled="checkingFollow"
            @click="toggleFollow"
          >
            {{ isFollowing ? '已关注' : '关注' }}
          </button>
          <button class="like-btn" :class="{ liked: isLiked }" @click="toggleLike">
            <svg width="20" height="20" viewBox="0 0 24 24" :fill="isLiked ? 'currentColor' : 'none'" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M19 14c1.49-1.46 3-3.21 3-5.5A5.5 5.5 0 0 0 16.5 3c-1.76 0-3 .5-4.5 2-1.5-1.5-2.74-2-4.5-2A5.5 5.5 0 0 0 2 8.5c0 2.3 1.5 4.05 3 5.5l7 7Z"/></svg>
            <span>{{ likeCount }}</span>
          </button>
        </div>
      </div>

      <!-- Divider -->
      <div class="divider animate-fade-up delay-350 animate-start-hidden"></div>

      <!-- Comments -->
      <div class="comments-section animate-fade-up delay-400 animate-start-hidden">
        <div class="section-label">评论 ({{ commentTotal }})</div>

        <div v-if="comments.length === 0 && !loadingComments" class="no-comments">
          暂无评论，来抢沙发吧
        </div>

        <div v-for="c in comments" :key="c.id" class="comment-item">
          <img v-if="c.avatar" :src="c.avatar" class="comment-avatar" alt="" />
          <div v-else class="comment-avatar-placeholder">
            <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M19 21v-2a4 4 0 0 0-4-4H9a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
          </div>
          <div class="comment-body">
            <div class="comment-header">
              <span class="comment-nickname">{{ c.nickname || '匿名' }}</span>
              <div class="comment-meta">
                <span class="comment-time">{{ formatTime(c.createdAt) }}</span>
                <button v-if="isOwnComment(c)" class="comment-delete-btn" @click.stop="deleteComment(c)">删除</button>
              </div>
            </div>
            <div class="comment-content">{{ c.content }}</div>
            <div v-if="c.imageUrl" class="comment-image" @click.stop="openPhotoModal(c.imageUrl)">
              <CachedImage :src="c.imageUrl" alt="评论图片" />
            </div>
          </div>
        </div>

        <button v-if="hasMoreComments && !loadingComments" class="load-more-btn" @click="loadMoreComments">
          加载更多评论
        </button>
        <div v-if="loadingComments" class="loading-more"><div class="spinner"></div></div>
      </div>
    </template>

    <!-- Bottom comment input -->
    <div class="comment-input-bar" v-if="post">
      <button class="plus-btn-comment" :class="{ active: showAttach }" @click.stop="toggleAttach">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="12" x2="12" y1="5" y2="19"/><line x1="5" x2="19" y1="12" y2="12"/></svg>
      </button>
      <button class="emoji-trigger-comment" :class="{ active: showEmoji }" @click.stop="toggleEmoji">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><path d="M8 14s1.5 2 4 2 4-2 4-2"/><line x1="9" x2="9.01" y1="9" y2="9"/><line x1="15" x2="15.01" y1="9" y2="9"/></svg>
      </button>
      <!-- Image preview -->
      <div v-if="commentImagePreview" class="comment-image-preview">
        <img :src="commentImagePreview" alt="" />
        <button class="comment-image-remove" @click.stop="removeCommentImage">
          <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
        </button>
      </div>
      <input
        v-model="newComment"
        class="comment-input"
        placeholder="写下你的评论..."
        @keyup.enter="submitComment"
        @focus="closePanels"
      />
      <button class="comment-send" :disabled="(!newComment.trim() && !commentImageFile) || sending" @click="submitComment">
        {{ sending ? '...' : '发送' }}
      </button>
    </div>

    <!-- Attachment Panel (comment) -->
    <Transition name="panel-slide">
      <div v-if="showAttach" class="comment-attach-panel" @click.stop>
        <div class="attach-panel-header">
          <span class="attach-panel-title"><em>附件</em></span>
          <button class="attach-close-btn" @click="showAttach = false">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
          </button>
        </div>
        <div class="attach-grid">
          <button class="attach-item" @click="triggerCommentPhoto">
            <div class="attach-icon attach-photo-icon">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="18" height="18" rx="2" ry="2"/><circle cx="8.5" cy="8.5" r="1.5"/><polyline points="21 15 16 10 5 21"/></svg>
            </div>
            <span class="attach-label">照片</span>
          </button>
        </div>
      </div>
    </Transition>

    <!-- Emoji Picker (comment) -->
    <Transition name="panel-slide">
      <div v-if="showEmoji" class="comment-emoji-wrapper" @click.stop>
        <EmojiPicker :show="showEmoji" @select="insertEmoji" @close="showEmoji = false" />
      </div>
    </Transition>

    <!-- Hidden file input for comment image -->
    <input ref="commentPhotoInput" type="file" accept="image/*" style="display:none" @change="handleCommentPhoto" />

    <!-- Back button -->
    <button class="back-btn animate-fade-up delay-450 animate-start-hidden" @click="router.back()">返回</button>

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
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { feedApi, followApi, uploadApi } from '@/api'
import { useAuthStore } from '@/stores/auth'
import { showSuccess, showError } from '@/utils/toast'
import { showConfirmDialog } from 'vant'
import CachedImage from '@/components/CachedImage.vue'
import EmojiPicker from '@/components/EmojiPicker.vue'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const post = ref<any>(null)
const loading = ref(true)
const photoModalUrl = ref<string | null>(null)
const isLiked = ref(false)
const likeCount = ref(0)
const comments = ref<any[]>([])
const commentPage = ref(0)
const commentTotal = ref(0)
const loadingComments = ref(false)
const hasMoreComments = ref(false)
const newComment = ref('')
const sending = ref(false)
const isFollowing = ref(false)
const showEmoji = ref(false)
const showAttach = ref(false)
const commentImageFile = ref<File | null>(null)
const commentImagePreview = ref<string | null>(null)
const commentPhotoInput = ref<HTMLInputElement>()

function insertEmoji(icon: string) {
  newComment.value += icon
  showEmoji.value = false
}

function toggleEmoji() {
  showAttach.value = false
  showEmoji.value = !showEmoji.value
}

function toggleAttach() {
  showEmoji.value = false
  showAttach.value = !showAttach.value
}

function closePanels() {
  showEmoji.value = false
  showAttach.value = false
}
const checkingFollow = ref(false)

const postId = computed(() => Number(route.params.postId))
const isOwnPost = computed(() => {
  const postUserId = post.value?.userId
  if (!postUserId || !authStore.userInfo?.userId) return false
  return String(postUserId) === String(authStore.userInfo.userId)
})

function isOwnComment(comment: any) {
  if (!comment?.userId || !authStore.userInfo?.userId) return false
  return String(comment.userId) === String(authStore.userInfo.userId)
}

const paramLabels: Record<string, string> = {
  time: '用餐时间', location: '用餐地点', weather: '天气情况',
  mood: '当前心情', companion: '同行人员', budget: '预算范围',
  taste: '口味偏好', restriction: '饮食禁忌', preference: '特殊偏好',
  health: '健康需求'
}

function paramLabel(key: string) { return paramLabels[key] || key }

const collectedParams = computed(() => {
  if (!post.value?.collectedParams) return []
  try {
    const parsed = typeof post.value.collectedParams === 'string'
      ? JSON.parse(post.value.collectedParams)
      : post.value.collectedParams
    if (Array.isArray(parsed)) return parsed
    return Object.entries(parsed).map(([name, value]) => ({ name, value }))
  } catch { return [] }
})

function formatDate(d: string) {
  if (!d) return ''
  const date = new Date(d)
  return `${date.getFullYear()}-${String(date.getMonth()+1).padStart(2,'0')}-${String(date.getDate()).padStart(2,'0')}`
}

function formatTime(d: string) {
  if (!d) return ''
  const date = new Date(d)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前'
  if (diff < 86400000) return Math.floor(diff / 3600000) + '小时前'
  return formatDate(d)
}

async function fetchDetail() {
  try {
    const res = await feedApi.getDetail(postId.value)
    post.value = res
    isLiked.value = res.isLiked || false
    likeCount.value = res.likeCount || 0
    commentTotal.value = res.commentCount || 0
  } catch {
    showError('该内容已被删除')
    router.back()
  } finally {
    loading.value = false
  }
}

async function fetchComments(reset = false) {
  loadingComments.value = true
  const page = reset ? 0 : commentPage.value
  try {
    const res = await feedApi.getComments(postId.value, { page, size: 10 })
    const list = res?.items || []
    if (reset) {
      comments.value = list
    } else {
      comments.value.push(...list)
    }
    hasMoreComments.value = list.length >= 10
    commentPage.value = page + 1
  } catch { /* ignore */ }
  loadingComments.value = false
}

function loadMoreComments() {
  fetchComments(false)
}

async function toggleLike() {
  try {
    const res = await feedApi.toggleLike(postId.value)
    isLiked.value = res.liked
    likeCount.value = res.likeCount
  } catch {
    showError('操作失败')
  }
}

async function toggleFollow() {
  if (!post.value?.userId || isOwnPost.value) return
  checkingFollow.value = true
  try {
    const res = await followApi.toggleFollow(post.value.userId)
    if (res) {
      isFollowing.value = res.isFollowing
    }
  } catch {
    showError('操作失败')
  } finally {
    checkingFollow.value = false
  }
}

async function submitComment() {
  const text = newComment.value.trim()
  if (!text && !commentImageFile.value) return
  sending.value = true
  try {
    let imageUrl: string | undefined
    if (commentImageFile.value) {
      const res = await uploadApi.uploadChatPhoto(commentImageFile.value)
      imageUrl = res.originalUrl || res.thumbnailUrl
    }
    await feedApi.addComment(postId.value, text, imageUrl)
    // Optimistic update with cached user info
    const me = authStore.userInfo
    comments.value.unshift({
      id: Date.now(),
      content: text,
      imageUrl: imageUrl || undefined,
      userId: me?.userId,
      nickname: me?.nickname || '我',
      avatar: me?.avatar || null,
      createdAt: new Date().toISOString()
    })
    commentTotal.value++
    newComment.value = ''
    removeCommentImage()
    closePanels()
    showSuccess('评论成功')
  } catch {
    showError('评论失败')
  } finally {
    sending.value = false
  }
}

async function deleteComment(comment: any) {
  if (!comment?.id) return
  try {
    await showConfirmDialog({ title: '删除评论', message: '确定删除这条评论吗？删除后将无法恢复。' })
    await feedApi.deleteComment(comment.id)
    comments.value = comments.value.filter(c => c.id !== comment.id)
    commentTotal.value = Math.max(0, commentTotal.value - 1)
    if (post.value) {
      post.value.commentCount = Math.max(0, Number(post.value.commentCount || 0) - 1)
    }
    showSuccess('评论已删除')
  } catch (err: any) {
    if (err === 'cancel' || err?.name === 'Cancel') return
    showError(err?.message || '删除评论失败')
  }
}

function openPhotoModal(url: string) {
  photoModalUrl.value = url
}

function triggerCommentPhoto() {
  showAttach.value = false
  commentPhotoInput.value?.click()
}

function handleCommentPhoto(e: Event) {
  const file = (e.target as HTMLInputElement).files?.[0]
  if (!file) return
  commentImageFile.value = file
  // Local preview
  if (commentImagePreview.value) URL.revokeObjectURL(commentImagePreview.value)
  commentImagePreview.value = URL.createObjectURL(file)
  // Reset input
  if (commentPhotoInput.value) commentPhotoInput.value.value = ''
}

function removeCommentImage() {
  if (commentImagePreview.value) {
    URL.revokeObjectURL(commentImagePreview.value)
  }
  commentImageFile.value = null
  commentImagePreview.value = null
}

onMounted(async () => {
  await fetchDetail()
  fetchComments(true)
  // Check follow status for post author
  if (post.value?.userId && !isOwnPost.value) {
    try {
      const res = await followApi.checkFollow(post.value.userId)
      isFollowing.value = res?.isFollowing || false
    } catch { /* ignore */ }
  }
})
</script>

<style lang="scss" scoped>
.detail-page {
  min-height: 100vh;
  background: var(--color-surface);
  padding: 40px 20px 100px;
  position: relative;
  overflow-y: auto;
}

.bg-glow {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  pointer-events: none;
}

.bg-glow-1 {
  top: -60px;
  right: -40px;
  width: 240px;
  height: 240px;
  background: var(--color-primary-container);
  opacity: 0.1;
}

/* Loading */
.loading-state {
  min-height: 50vh;
  display: flex;
  align-items: center;
  justify-content: center;
}

.spinner {
  width: 32px;
  height: 32px;
  border: 3px solid var(--color-surface-container-low);
  border-top-color: var(--color-primary);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin { to { transform: rotate(360deg); } }

/* Header */
.page-title {
  font-family: var(--font-serif); font-style: italic;
  font-size: 32px; font-weight: 400; color: var(--color-on-surface);
  margin-bottom: 20px; text-align: center; z-index: 1; position: relative;
  display: flex; align-items: center; justify-content: center; gap: 10px;
  em { font-style: italic; color: var(--color-primary); }
}

.detail-fans-badge {
  font-family: var(--font-sans); font-style: normal;
  font-size: 11px; font-weight: 700;
  padding: 4px 10px; border-radius: 6px;
  background: rgba(0, 89, 182, 0.08);
  color: var(--color-primary);
}

/* Photo */
.photo-section { margin-bottom: 20px; z-index: 1; position: relative; }

.detail-photo {
  width: 100%; border-radius: 2rem; display: block;
  max-height: 400px; object-fit: cover;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
  cursor: pointer;
  transition: transform 0.3s cubic-bezier(0.22, 1, 0.36, 1);
  &:hover {
    transform: scale(1.01);
  }
}

/* Reason */
.reason-card {
  background: var(--color-inverse-surface);
  border-radius: 2rem; padding: 28px 24px;
  margin-bottom: 20px; position: relative; overflow: hidden; z-index: 1;
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

/* Params */
.params-section { margin-bottom: 20px; z-index: 1; position: relative; }

.section-label {
  font-size: 10px; font-weight: 700; color: var(--color-on-surface-variant);
  text-transform: uppercase; letter-spacing: 0.15em; margin-bottom: 12px;
}

.params-grid {
  display: grid; grid-template-columns: 1fr 1fr; gap: 8px;
}

.param-item {
  display: flex; flex-direction: column; gap: 2px;
  padding: 12px 16px; background: var(--color-surface-container-lowest);
  border-radius: 1.25rem; border: 1px solid rgba(255, 255, 255, 0.8);
}

.param-key { font-size: 10px; color: var(--color-on-surface-variant); font-weight: 600; text-transform: uppercase; letter-spacing: 0.08em; }
.param-val { font-size: 14px; color: var(--color-on-surface); font-weight: 500; }

/* User + Like */
.user-like-row {
  display: flex; justify-content: space-between; align-items: center;
  margin-bottom: 20px; z-index: 1; position: relative;
}

.user-info {
  display: flex; align-items: center; gap: 10px;
}

.user-avatar {
  width: 40px; height: 40px; border-radius: 50%;
  object-fit: cover; border: 2px solid var(--color-surface-container-low);
}

.user-avatar-placeholder {
  width: 40px; height: 40px; border-radius: 50%;
  background: var(--color-surface-container-low);
  display: flex; align-items: center; justify-content: center;
  color: var(--color-on-surface-variant);
}

.user-name { font-size: 14px; font-weight: 600; color: var(--color-on-surface); }
.post-time { font-size: 11px; color: var(--color-on-surface-variant); margin-top: 2px; }

.user-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.follow-btn {
  padding: 6px 16px;
  border: 1.5px solid var(--color-primary);
  border-radius: 100px;
  background: none;
  color: var(--color-primary);
  font-family: var(--font-sans);
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  &:active { transform: scale(0.95); }
  &.following {
    background: var(--color-surface-container-low);
    border-color: var(--color-surface-container-low);
    color: var(--color-on-surface-variant);
  }
  &:disabled { opacity: 0.5; cursor: not-allowed; }
}

.like-btn {
  display: flex; align-items: center; gap: 4px;
  padding: 8px 16px; border: 1.5px solid var(--color-surface-container-low);
  border-radius: 100px; background: none;
  color: var(--color-on-surface-variant); font-size: 13px; font-weight: 600;
  cursor: pointer; transition: all 0.2s;
  &.liked {
    border-color: #ef4444; color: #ef4444;
    background: rgba(239, 68, 68, 0.06);
  }
  &:active { transform: scale(0.95); }
}

/* Divider */
.divider {
  height: 1px; background: var(--color-surface-container-low);
  margin: 20px 0; z-index: 1; position: relative;
}

/* Comments */
.comments-section { margin-bottom: 80px; z-index: 1; position: relative; }

.no-comments {
  text-align: center; padding: 30px 0;
  color: var(--color-on-surface-variant); font-size: 14px; opacity: 0.6;
}

.comment-item {
  display: flex; gap: 10px; padding: 12px 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
}

.comment-avatar {
  width: 32px; height: 32px; border-radius: 50%;
  object-fit: cover; flex-shrink: 0;
}

.comment-avatar-placeholder {
  width: 32px; height: 32px; border-radius: 50%;
  background: var(--color-surface-container-low);
  display: flex; align-items: center; justify-content: center;
  color: var(--color-on-surface-variant); flex-shrink: 0;
}

.comment-body { flex: 1; min-width: 0; }

.comment-header {
  display: flex; justify-content: space-between; align-items: center;
  margin-bottom: 4px;
}

.comment-nickname { font-size: 13px; font-weight: 600; color: var(--color-on-surface); }
.comment-meta { display: flex; align-items: center; gap: 10px; }
.comment-time { font-size: 11px; color: var(--color-on-surface-variant); }
.comment-delete-btn {
  border: none;
  background: transparent;
  color: var(--color-danger, #ef4444);
  font-size: 12px;
  cursor: pointer;
  padding: 0;
}
.comment-content { font-size: 14px; line-height: 1.5; color: var(--color-on-surface); }

.comment-image {
  margin-top: 8px;
  max-width: 180px;
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  img {
    width: 100%;
    display: block;
    border-radius: 12px;
  }
}

.load-more-btn {
  display: block; width: 100%; padding: 12px;
  border: 1px solid var(--color-surface-container-low);
  border-radius: 1rem; background: none;
  color: var(--color-primary); font-size: 13px; font-weight: 600;
  cursor: pointer; margin-top: 12px;
}

.loading-more {
  display: flex; justify-content: center; padding: 16px 0;
}

/* Comment input bar */
.comment-input-bar {
  position: fixed; bottom: 0; left: 0; right: 0;
  display: flex; gap: 8px; padding: 12px 16px;
  background: var(--color-surface-container-lowest);
  border-top: 1px solid var(--color-surface-container-low);
  z-index: 100;
}

.emoji-trigger-comment {
  width: 36px; height: 36px;
  display: flex; align-items: center; justify-content: center;
  background: none; border: none; color: var(--color-on-surface-variant);
  cursor: pointer; border-radius: 50%; flex-shrink: 0;
  transition: background 0.2s, color 0.2s;
  &:active { background: var(--color-surface-container-low); }
  &.active { color: var(--color-primary); background: rgba(0, 89, 182, 0.08); }
}

.plus-btn-comment {
  width: 36px; height: 36px;
  display: flex; align-items: center; justify-content: center;
  background: none; border: none; color: var(--color-on-surface-variant);
  cursor: pointer; border-radius: 50%; flex-shrink: 0;
  transition: background 0.2s, transform 0.2s, color 0.2s;
  &:active { background: var(--color-surface-container-low); }
  &.active {
    color: var(--color-primary);
    background: rgba(0, 89, 182, 0.08);
    svg { transform: rotate(45deg); }
  }
}

.comment-image-preview {
  position: relative;
  width: 36px; height: 36px; flex-shrink: 0;
  border-radius: 8px; overflow: hidden;
  img { width: 100%; height: 100%; object-fit: cover; }
}

.comment-image-remove {
  position: absolute; top: 2px; right: 2px;
  width: 16px; height: 16px;
  border: none; border-radius: 50%;
  background: rgba(0, 0, 0, 0.5); color: white;
  display: flex; align-items: center; justify-content: center;
  cursor: pointer;
}

.comment-input {
  flex: 1; padding: 10px 16px;
  border: 1.5px solid var(--color-surface-container-low);
  border-radius: 100px; background: var(--color-surface);
  font-family: var(--font-sans); font-size: 14px;
  color: var(--color-on-surface); outline: none;
  &:focus { border-color: var(--color-primary); }
  &::placeholder { color: var(--color-on-surface-variant); opacity: 0.5; }
}

.comment-send {
  padding: 10px 20px; border: none; border-radius: 100px;
  background: linear-gradient(135deg, var(--color-primary-container), var(--color-primary));
  color: white; font-size: 13px; font-weight: 700; cursor: pointer;
  &:disabled { opacity: 0.5; cursor: not-allowed; }
}

/* Comment attachment panel */
.comment-attach-panel {
  position: fixed; bottom: 64px; left: 0; right: 0;
  background: rgba(255, 255, 255, 0.6);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-radius: 1.5rem 1.5rem 0 0;
  border: 1px solid rgba(255, 255, 255, 0.8);
  box-shadow: 0 -4px 24px rgba(0, 0, 0, 0.06);
  z-index: 101;
}

.attach-panel-header {
  display: flex; justify-content: space-between; align-items: center;
  padding: 12px 16px 8px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.3);
}

.attach-panel-title {
  font-family: var(--font-serif); font-style: italic;
  font-size: 16px; font-weight: 500;
  color: var(--color-on-surface-variant);
  em { font-style: italic; }
}

.attach-close-btn {
  background: none; border: none;
  color: var(--color-on-surface-variant);
  cursor: pointer; padding: 4px;
  display: flex; align-items: center;
  border-radius: 50%;
  transition: background 0.2s;
  &:active { background: rgba(0, 0, 0, 0.05); }
}

.attach-grid {
  display: flex; gap: 16px; padding: 20px 24px;
}

.attach-item {
  display: flex; flex-direction: column; align-items: center;
  gap: 8px; padding: 16px 24px;
  border: none; border-radius: 1.25rem;
  background: rgba(255, 255, 255, 0.5);
  cursor: pointer;
  transition: background 0.2s, transform 0.15s;
  &:active {
    background: rgba(0, 89, 182, 0.08);
    transform: scale(0.95);
  }
}

.attach-icon {
  width: 48px; height: 48px; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  &.attach-photo-icon {
    background: linear-gradient(135deg, rgba(0, 89, 182, 0.12), rgba(0, 89, 182, 0.06));
    color: var(--color-primary);
  }
}

.attach-label {
  font-size: 13px; font-weight: 600; color: var(--color-on-surface);
}

.comment-emoji-wrapper {
  position: fixed; bottom: 64px; left: 0; right: 0;
  z-index: 101;
}

/* Panel slide transition */
.panel-slide-enter-active {
  transition: transform 0.25s cubic-bezier(0.4, 0, 0.2, 1), opacity 0.2s;
}
.panel-slide-leave-active {
  transition: transform 0.2s cubic-bezier(0.4, 0, 0.2, 1), opacity 0.15s;
}
.panel-slide-enter-from {
  transform: translateY(100%);
  opacity: 0;
}
.panel-slide-leave-to {
  transform: translateY(100%);
  opacity: 0;
}

.back-btn {
  position: fixed; top: 16px; left: 16px;
  width: 36px; height: 36px; border: none; border-radius: 50%;
  background: rgba(0, 0, 0, 0.3); color: white;
  display: flex; align-items: center; justify-content: center;
  cursor: pointer; z-index: 100; backdrop-filter: blur(8px);
  &:active { transform: scale(0.9); }
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

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.25s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

@media (min-width: 1024px) {
  .detail-page {
    max-width: 60%;
    margin: 0 auto;
  }
}
</style>

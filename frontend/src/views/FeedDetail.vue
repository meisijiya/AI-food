<template>
  <div class="detail-page">
    <div class="bg-glow bg-glow-1"></div>

    <div v-if="loading" class="loading-state">
      <div class="spinner"></div>
    </div>

    <template v-if="post">
      <!-- Header -->
      <h1 class="page-title animate-fade-up"><em>{{ post.foodName }}</em></h1>

      <!-- Photo -->
      <div v-if="post.thumbnailUrl || post.originalPhotoUrl" class="photo-section animate-fade-up delay-100 animate-start-hidden">
        <CachedImage :src="post.thumbnailUrl || post.originalPhotoUrl" :alt="post.foodName" class="detail-photo" />
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

      <!-- User info + Like -->
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
        <button class="like-btn" :class="{ liked: isLiked }" @click="toggleLike">
          <svg width="20" height="20" viewBox="0 0 24 24" :fill="isLiked ? 'currentColor' : 'none'" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M19 14c1.49-1.46 3-3.21 3-5.5A5.5 5.5 0 0 0 16.5 3c-1.76 0-3 .5-4.5 2-1.5-1.5-2.74-2-4.5-2A5.5 5.5 0 0 0 2 8.5c0 2.3 1.5 4.05 3 5.5l7 7Z"/></svg>
          <span>{{ likeCount }}</span>
        </button>
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
              <span class="comment-time">{{ formatTime(c.createdAt) }}</span>
            </div>
            <div class="comment-content">{{ c.content }}</div>
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
      <input
        v-model="newComment"
        class="comment-input"
        placeholder="写下你的评论..."
        @keyup.enter="submitComment"
      />
      <button class="comment-send" :disabled="!newComment.trim() || sending" @click="submitComment">
        {{ sending ? '...' : '发送' }}
      </button>
    </div>

    <!-- Back button -->
    <button class="back-btn animate-fade-up delay-450 animate-start-hidden" @click="router.back()">返回</button>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { feedApi } from '@/api'
import { showSuccess, showError } from '@/utils/toast'
import CachedImage from '@/components/CachedImage.vue'

const route = useRoute()
const router = useRouter()

const post = ref<any>(null)
const loading = ref(true)
const isLiked = ref(false)
const likeCount = ref(0)
const comments = ref<any[]>([])
const commentPage = ref(0)
const commentTotal = ref(0)
const loadingComments = ref(false)
const hasMoreComments = ref(false)
const newComment = ref('')
const sending = ref(false)

const postId = computed(() => Number(route.params.postId))

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
    // ignore
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

async function submitComment() {
  if (!newComment.value.trim()) return
  sending.value = true
  try {
    await feedApi.addComment(postId.value, newComment.value.trim())
    comments.value.unshift({
      id: Date.now(),
      content: newComment.value.trim(),
      userNickname: '我',
      createdAt: new Date().toISOString()
    })
    commentTotal.value++
    newComment.value = ''
    showSuccess('评论成功')
  } catch {
    showError('评论失败')
  } finally {
    sending.value = false
  }
}

onMounted(() => {
  fetchDetail()
  fetchComments(true)
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
  em { font-style: italic; color: var(--color-primary); }
}

/* Photo */
.photo-section { margin-bottom: 20px; z-index: 1; position: relative; }

.detail-photo {
  width: 100%; border-radius: 2rem; display: block;
  max-height: 400px; object-fit: cover;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
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
.comment-time { font-size: 11px; color: var(--color-on-surface-variant); }
.comment-content { font-size: 14px; line-height: 1.5; color: var(--color-on-surface); }

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

.back-btn {
  position: fixed; top: 16px; left: 16px;
  width: 36px; height: 36px; border: none; border-radius: 50%;
  background: rgba(0, 0, 0, 0.3); color: white;
  display: flex; align-items: center; justify-content: center;
  cursor: pointer; z-index: 100; backdrop-filter: blur(8px);
  &:active { transform: scale(0.9); }
}
</style>

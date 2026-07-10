<template>
  <div class="detail-container bg-cold-canvas">

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
          <span>AI 推荐</span>
          <span v-if="detail.session.totalQuestions">{{ detail.session.currentQuestionCount }}/{{ detail.session.totalQuestions }} 轮</span>
          <span v-if="detail.session.createdAt">{{ formatDate(detail.session.createdAt) }}</span>
        </div>
      </div>

      <!-- Photo section: 照片展示 + 替换 + 删除 (子组件) -->
      <RecordPhotoGallery
        v-if="detail.recommendation"
        :photo="detail.photo"
        :session-id="sessionId"
        @uploaded="onPhotoUploaded"
        @delete="handleDeletePhoto"
      />

      <!-- Comment section: 美食评价 (子组件) -->
      <RecordCommentList
        v-if="detail.recommendation"
        ref="commentListRef"
        :comment="commentText"
        @save="onSaveComment"
      />

      <!-- Actions row: Share + Publish + Share link + Publish dialog (子组件) -->
      <RecordActions
        ref="actionsRef"
        :session-id="sessionId"
        :share-url="shareUrl"
        :sharing="sharing"
        :is-published="isPublished"
        :unpublishing="unpublishing"
        :show-recommendation="!!detail.recommendation"
        :publish-dialog="publishDialog"
        :publishing="publishing"
        @share="handleShare"
        @unpublish="handleUnpublish"
        @open-publish="onOpenPublishDialog"
        @close-publish="publishDialog = false"
        @confirm-publish="onConfirmPublish"
        @copy-share-url="copyShareUrl"
      />

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
  </div>
</template>

<script setup lang="ts">
// 记录详情页父组件
// 职责: 拉取详情数据 / 持有 page 级别 state / 编排子组件 / 转发 API 调用
// 拆出去的子组件: RecordPhotoGallery / RecordCommentList / RecordActions
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { recordApi, shareApi, feedApi } from '@/api'
import { showSuccess, showError } from '@/utils/toast'
import RecordPhotoGallery from './components/RecordDetail/RecordPhotoGallery.vue'
import RecordCommentList from './components/RecordDetail/RecordCommentList.vue'
import RecordActions from './components/RecordDetail/RecordActions.vue'

const route = useRoute()
const router = useRouter()

// 子组件 ref —— 用于在 API 回调中通知子组件更新内部 UI 状态
const commentListRef = ref<InstanceType<typeof RecordCommentList> | null>(null)
const actionsRef = ref<InstanceType<typeof RecordActions> | null>(null)

// Page 级别 state
const loading = ref(true)
const detail = ref<any>(null)
const shareUrl = ref('')
const sharing = ref(false)
const isPublished = ref(false)
const publishDialog = ref(false)
const publishing = ref(false)
const unpublishing = ref(false)

const sessionId = computed(() => route.params.sessionId as string)

// 从 detail 派生的派生 state
const commentText = computed(() => {
  return detail.value?.recommendation?.comment || ''
})

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

// 静态参数标签映射
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

// 拉取详情 + 发布状态
async function fetchDetail() {
  const sid = route.params.sessionId as string
  if (!sid) {
    loading.value = false
    return
  }

  try {
    const res = await recordApi.getRecordDetail(sid)
    detail.value = res
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

// ===== 照片相关(子组件 emit,父组件做 API) =====
function onPhotoUploaded(data: { thumbnailUrl: string; originalUrl: string }) {
  if (detail.value) {
    detail.value.photo = {
      thumbnailPath: data.thumbnailUrl,
      originalPath: data.originalUrl
    }
  }
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
    showSuccess('照片已删除')
  } catch {
    showError('删除失败')
  }
}

// ===== 评价相关(子组件 emit,父组件做 API) =====
async function onSaveComment(content: string) {
  const sid = sessionId.value
  if (!sid) return
  try {
    await recordApi.updateComment(sid, content || '')
    if (detail.value?.recommendation) {
      detail.value.recommendation.comment = content
    }
    commentListRef.value?.onSaveSuccess()
    showSuccess('评价已保存')
  } catch {
    commentListRef.value?.onSaveError()
    showError('保存失败')
  }
}

// ===== 分享 + 发布相关(子组件 emit,父组件做 API) =====
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

function onOpenPublishDialog() {
  // 通过子组件暴露的方法,设置发布预览的初始值(从评价同步过来),保持原行为
  actionsRef.value?.setPublishPreview(commentText.value)
  publishDialog.value = true
}

async function onConfirmPublish(preview: string, visibility: 'public' | 'friends') {
  const sid = sessionId.value
  if (!sid) return
  publishing.value = true
  try {
    await feedApi.publish({ sessionId: sid, commentPreview: preview || undefined, visibility })
    isPublished.value = true
    publishDialog.value = false
    showSuccess('发布成功')
  } catch (e: any) {
    showError(e?.message || '发布失败')
  } finally {
    publishing.value = false
  }
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
  background: var(--color-primary-soft);
  opacity: 0.1;
  animation: glow-drift 8s ease-in-out infinite;
}

.bg-glow-2 {
  bottom: 200px;
  left: -80px;
  width: 200px;
  height: 200px;
  background: var(--color-cyan);
  opacity: 0.06;
  animation: glow-drift 10s ease-in-out infinite reverse;
}

@keyframes glow-drift {
  0%, 100% { transform: translate(0, 0); }
  50% { transform: translate(10px, -10px); }
}

/* ===== Card Entrance Animation (parent-owned cards) ===== */
.card-enter {
  animation: card-slide-up 0.5s var(--ease-out-soft) both;
}
.card-delay-1 { animation-delay: 0.05s; }
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
  border: 3px solid var(--color-surface-low);
  border-top-color: var(--color-primary);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* ===== Recommend Card (Light) ===== */
.recommend-card {
  background: var(--color-surface-lowest);
  border: 1px solid var(--color-on-inverse-overlay-sm);
  border-radius: 2.5rem;
  padding: 32px 28px;
  margin-bottom: 24px;
  position: relative;
  overflow: hidden;
  box-shadow: 0 8px 32px /* ponytail: rgba(74, 141, 213, 0.06) */ rgba(74, 141, 213, 0.06);
  transition: box-shadow 0.4s ease, transform 0.4s var(--ease-out-soft);

  &:hover {
    box-shadow: 0 12px 40px /* ponytail: rgba(74, 141, 213, 0.1) */ rgba(74, 141, 213, 0.1);
    transform: translateY(-2px);
  }
}

.recommend-accent {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: linear-gradient(90deg, var(--color-primary-soft), var(--color-primary), var(--color-cyan));
  border-radius: 2.5rem 2.5rem 0 0;
}

.recommend-glow {
  position: absolute;
  top: -60px;
  right: -40px;
  width: 200px;
  height: 200px;
  background: var(--color-cyan);
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
    background: var(--color-surface-low);
    border-radius: 100px;
  }
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
  background: var(--color-surface-lowest);
  border-radius: 100px;
  border: 1px solid var(--color-surface-low);
  font-size: 12px;
  box-shadow: var(--shadow-flat-xs);
  transition: box-shadow 0.2s;

  &:hover {
    box-shadow: var(--shadow-flat-sm);
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
    border-color: var(--color-cyan);
    background: radial-gradient(circle, var(--color-cyan) 30%, transparent 30%);
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
  background: linear-gradient(to bottom, var(--color-surface-low), transparent);
}

.timeline-content {
  background: var(--color-surface-lowest);
  border-radius: 1.5rem;
  padding: 16px 20px;
  border: 1px solid var(--color-on-inverse-overlay-sm);
  box-shadow: var(--shadow-flat-xs);
  transition: box-shadow 0.2s;

  &:hover {
    box-shadow: var(--shadow-flat-sm);
  }
}

.timeline-role {
  font-size: 10px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.15em;
  color: var(--color-cyan);
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
  border: 1.5px solid var(--color-surface-low);
  border-radius: 2rem;
  background: var(--color-surface-lowest);
  color: var(--color-on-surface-variant);
  font-family: var(--font-sans);
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.25s;
  margin-top: 8px;
  box-shadow: var(--shadow-flat-xs);

  &:hover {
    border-color: var(--color-primary);
    color: var(--color-primary);
    transform: translateY(-1px);
    box-shadow: 0 4px 16px var(--focus-ring-color);
  }

  &:active {
    transform: scale(0.98);
  }
}

.nav-spacer {
  height: 80px;
}

/* ===== Responsive ===== */
@media (min-width: 1024px) {
  .detail-container {
    max-width: 60%;
    margin: 0 auto;
  }
}
</style>

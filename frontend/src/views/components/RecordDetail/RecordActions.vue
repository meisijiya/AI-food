<template>
  <!-- Actions row: Share + Publish/Unpublish -->
  <div v-if="sessionId" class="actions-row card-enter card-delay-4">
    <div v-if="!shareUrl" class="action-chip">
      <button class="chip-btn share-chip" @click="emit('share')" :disabled="sharing">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="18" cy="5" r="3"/><circle cx="6" cy="12" r="3"/><circle cx="18" cy="19" r="3"/><line x1="8.59" x2="15.42" y1="13.51" y2="17.49"/><line x1="15.41" x2="8.59" y1="6.51" y2="10.49"/></svg>
        {{ sharing ? '创建中...' : '分享' }}
      </button>
    </div>
    <div v-if="showRecommendation && !isPublished" class="action-chip">
      <button class="chip-btn publish-chip" @click="emit('open-publish')">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" x2="12" y1="3" y2="15"/></svg>
        发布到大厅
      </button>
    </div>
    <div v-if="showRecommendation && isPublished" class="action-chip">
      <button class="chip-btn unpublish-chip" @click="emit('unpublish')" :disabled="unpublishing">
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
      <button class="share-copy-btn" @click="emit('copy-share-url')">复制</button>
    </div>
  </div>

  <!-- Publish dialog -->
  <Transition name="fade">
    <div v-if="publishDialog" class="photo-modal" @click.self="closePublishDialog">
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
          <button class="emoji-trigger" @click="openEmoji">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><path d="M8 14s1.5 2 4 2 4-2 4-2"/><line x1="9" x2="9.01" y1="9" y2="9"/><line x1="15" x2="15.01" y1="9" y2="9"/></svg>
          </button>
          <span>{{ (publishPreview || '').length }}/30</span>
        </div>
        <div class="publish-dialog-actions">
          <button class="publish-cancel-btn" @click="closePublishDialog">取消</button>
          <button class="publish-confirm-btn" @click="confirmPublish" :disabled="publishing">
            {{ publishing ? '发布中...' : '确认发布' }}
          </button>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup lang="ts">
// 记录详情 - 操作按钮子组件
// 负责: 分享 / 发布 / 取消发布 / 分享链接展示 / 发布动态弹窗
// 所有 API 调用由父组件完成,子组件通过 emit 回传事件
import { ref } from 'vue'

// 父组件传入的 props
defineProps<{
  sessionId: string
  shareUrl: string
  sharing: boolean
  isPublished: boolean
  unpublishing: boolean
  showRecommendation: boolean
  publishDialog: boolean
  publishing: boolean
}>()

// 事件回传父组件(API 调用由父组件负责)
const emit = defineEmits<{
  (e: 'share'): void
  (e: 'unpublish'): void
  (e: 'open-publish'): void
  (e: 'close-publish'): void
  (e: 'confirm-publish', preview: string, visibility: 'public' | 'friends'): void
  (e: 'copy-share-url'): void
}>()

// 发布弹窗内部状态
const publishPreview = ref('')
const publishVisibility = ref<'public' | 'friends'>('public')
const showEmoji = ref(false)

// 切换 emoji 面板
function openEmoji() {
  showEmoji.value = !showEmoji.value
}

// 插入 emoji 到发布预览
function insertEmoji(icon: string) {
  publishPreview.value = (publishPreview.value || '') + icon
  showEmoji.value = false
}

// 关闭发布弹窗
function closePublishDialog() {
  emit('close-publish')
}

// 确认发布: 通知父组件执行 API
function confirmPublish() {
  emit('confirm-publish', publishPreview.value, publishVisibility.value)
}

// 暴露给父组件的方法 —— 父组件在 open-publish 事件中调用
// 用于设置发布预览的初始值(从评价同步过来)
function setPublishPreview(preview: string) {
  publishPreview.value = preview || ''
  publishVisibility.value = 'public'
}

defineExpose({ setPublishPreview, insertEmoji })
</script>

<style lang="scss" scoped>
/* ===== Card Entrance Animation (self-contained, no parent dependency) ===== */
.card-enter {
  animation: card-slide-up 0.5s cubic-bezier(0.22, 1, 0.36, 1) both;
}
.card-delay-4 { animation-delay: 0.2s; }

@keyframes card-slide-up {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}

/* ===== Fade Transition (for publish dialog) ===== */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* ===== Section Title (shared shape) ===== */
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

  &:hover { background: rgba(74, 141, 213, 0.06); }
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
  background: var(--color-surface-lowest);
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
  border: 1px solid var(--color-surface-low);
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
  background: linear-gradient(135deg, var(--color-primary-soft), var(--color-primary));
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

/* ===== Publish Dialog (reuses .photo-modal base) ===== */
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

.publish-dialog {
  width: calc(100% - 48px);
  max-width: 400px;
  background: var(--color-surface-lowest);
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
  background: var(--color-primary-soft);
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
  border: 1.5px solid var(--color-surface-low);
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
    background: linear-gradient(135deg, var(--color-primary-soft), var(--color-primary));
    border-color: transparent;
    color: white;
    svg {
      opacity: 1;
      stroke: white;
    }
  }

  &:not(.active):active {
    background: var(--color-surface-low);
  }
}

.publish-textarea {
  width: 100%;
  padding: 14px 18px;
  border: 1.5px solid var(--color-surface-low);
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
    box-shadow: 0 0 0 3px rgba(74, 141, 213, 0.08);
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
  &:active { background: var(--color-surface-low); }
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
  border: 1.5px solid var(--color-surface-low);
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
  background: linear-gradient(135deg, var(--color-primary-soft), var(--color-primary));
  color: white;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  box-shadow: 0 4px 16px rgba(74, 141, 213, 0.2);
  transition: all 0.25s;

  &:hover {
    transform: translateY(-1px);
    box-shadow: 0 6px 20px rgba(74, 141, 213, 0.3);
  }

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
    transform: none;
  }
}
</style>

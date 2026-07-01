<template>
  <!--
    ResultActions - 操作按钮(Share + Publish + 取消发布 + 发布弹窗)
    内部维护 publishDialog + publishPreview + showEmoji 状态
    所有 API 调用(shareApi.createShare / feedApi.publish / feedApi.unpublish)由父组件完成
    本组件是受控的:父组件传入状态,子组件发起事件
  -->
  <div v-if="sessionId" class="actions-stack">
    <!-- 分享区域 -->
    <div class="share-section animate-fade-up delay-490 animate-start-hidden">
      <div class="share-card" v-if="!shareUrl">
        <button class="share-btn" @click="emit('share')" :disabled="sharing">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="18" cy="5" r="3"/><circle cx="6" cy="12" r="3"/><circle cx="18" cy="19" r="3"/><line x1="8.59" x2="15.42" y1="13.51" y2="17.49"/><line x1="15.41" x2="8.59" y1="6.51" y2="17.49"/></svg>
          {{ sharing ? '创建中...' : '分享此美食' }}
        </button>
      </div>
      <div class="share-link-card" v-else>
        <div class="share-link-label">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="18" cy="5" r="3"/><circle cx="6" cy="12" r="3"/><circle cx="18" cy="19" r="3"/><line x1="8.59" x2="15.42" y1="13.51" y2="17.49"/><line x1="15.41" x2="8.59" y1="6.51" y2="17.49"/></svg>
          <span>分享链接</span>
        </div>
        <div class="share-link-row">
          <input class="share-link-input" :value="shareUrl" readonly />
          <button class="share-copy-btn" @click="emit('copy-share-url')">复制链接</button>
        </div>
      </div>
    </div>

    <!-- 发布区域 -->
    <div class="publish-section animate-fade-up delay-495 animate-start-hidden">
      <div class="publish-card">
        <button v-if="!isPublished" class="publish-btn" @click="openPublishDialog">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" x2="12" y1="3" y2="15"/></svg>
          发布到大厅
        </button>
        <button v-else class="unpublish-btn" @click="emit('unpublish')" :disabled="unpublishing">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" x2="12" y1="3" y2="15"/></svg>
          {{ unpublishing ? '取消中...' : '取消发布' }}
        </button>
      </div>
    </div>

    <!-- 发布弹窗 -->
    <Transition name="fade">
      <div v-if="publishDialog" class="photo-modal" @click.self="closePublishDialog">
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
            <button class="emoji-trigger" @click="showEmoji = !showEmoji">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><path d="M8 14s1.5 2 4 2 4-2 4-2"/><line x1="9" x2="9.01" y1="9" y2="9"/><line x1="15" x2="15.01" y1="9" y2="9"/></svg>
            </button>
            <span>{{ publishPreview.length }}/30</span>
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

    <!-- Emoji Picker(子组件自己拥有,跟 comment 互不影响) -->
    <EmojiPicker :show="showEmoji" @select="insertEmoji" @close="showEmoji = false" />
  </div>
</template>

<script setup lang="ts">
// ResultActions - Share + Publish 合并(子组件)
// 父组件传入 sessionId + 各种 loading 状态 + shareUrl + isPublished
// 子组件通过 emit 把 share / unpublish / confirm-publish 等事件回传
// API 调用全部由父组件完成
import { ref } from 'vue'
import EmojiPicker from '@/components/EmojiPicker.vue'

const props = defineProps<{
  sessionId: string
  shareUrl: string
  isPublished: boolean
  sharing: boolean
  publishing: boolean
  unpublishing: boolean
  initialPublishPreview?: string  // 打开弹窗时的初始预览值(取自 chatStore.recommendationResult.reason)
}>()

const emit = defineEmits<{
  (e: 'share'): void
  (e: 'copy-share-url'): void
  (e: 'open-publish'): void
  (e: 'close-publish'): void
  (e: 'confirm-publish', preview: string): void
  (e: 'unpublish'): void
}>()

// 内部 UI 状态
const publishDialog = ref(false)
const publishPreview = ref('')
const showEmoji = ref(false)

// 打开发布弹窗 —— 设置初始 preview 值
function openPublishDialog() {
  publishPreview.value = props.initialPublishPreview || ''
  publishDialog.value = true
  emit('open-publish')
}

// 关闭发布弹窗
function closePublishDialog() {
  publishDialog.value = false
  emit('close-publish')
}

// 确认发布 —— 回传 preview 给父组件
function confirmPublish() {
  emit('confirm-publish', publishPreview.value || '')
}

// 插入 emoji
function insertEmoji(icon: string) {
  publishPreview.value = (publishPreview.value || '') + icon
  showEmoji.value = false
}
</script>

<style lang="scss" scoped>
.actions-stack {
  display: contents;  /* 让内部 div 保持原有布局(子组件不引入额外 wrapper) */
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

/* Publish dialog */
.photo-modal {
  /* overlay 样式已抽到 Result.vue 父组件 :deep(.photo-modal),避免 100 行重复 */
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

.emoji-trigger {
  width: 36px; height: 36px;
  display: flex; align-items: center; justify-content: center;
  background: none; border: none; color: var(--color-on-surface-variant);
  cursor: pointer; border-radius: 50%; flex-shrink: 0;
  transition: background 0.2s;
  &:active { background: var(--color-surface-container-low); }
}

/* Transition */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.25s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
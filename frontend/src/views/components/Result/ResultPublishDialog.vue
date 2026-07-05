<!--
  ResultPublishDialog - 发布按钮 + 取消发布 + 发布弹窗(含 emoji picker)
  父组件 ResultActions 传入 isPublished / publishing / unpublishing / initialPublishPreview,
  内部维护 publishDialog + publishPreview + showEmoji 三个 UI 状态,
  事件回传给父组件处理实际 API 调用。
  .photo-modal overlay 样式由 Result.vue 父组件 :deep(.photo-modal) 共享(避免 100 行重复)。
-->
<template>
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
        <p class="publish-dialog-hint">编辑评论预览(展示在大厅卡片上,最多30字)</p>
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
</template>

<script setup lang="ts">
// ResultPublishDialog - 发布/取消发布 + 弹窗 + emoji 状态机
// 父组件拥有真实 API 调用(publishApi.publish / feedApi.unpublish),本组件只负责 UI + 状态
import { ref } from 'vue'
import EmojiPicker from '@/components/EmojiPicker.vue'

const props = defineProps<{
  isPublished: boolean
  publishing: boolean
  unpublishing: boolean
  initialPublishPreview?: string  // 打开弹窗时的初始预览值(取自 chatStore.recommendationResult.reason)
}>()

const emit = defineEmits<{
  (e: 'unpublish'): void
  (e: 'open-publish'): void
  (e: 'close-publish'): void
  (e: 'confirm-publish', preview: string): void
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
  border: 1.5px solid var(--color-cyan-bright);
  border-radius: 2rem;
  background: none;
  color: var(--color-cyan-bright);
  font-family: var(--font-sans);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    background: /* ponytail: rgba(34, 211, 238, 0.06) */ rgba(34, 211, 238, 0.06);
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
  border: 1.5px solid /* ponytail: rgba(239, 68, 68, 0.4) */ rgba(239, 68, 68, 0.4);
  border-radius: 2rem;
  background: var(--color-danger-06);
  color: var(--color-danger-bright);
  font-family: var(--font-sans);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    background: /* ponytail: rgba(239, 68, 68, 0.12) */ rgba(239, 68, 68, 0.12);
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
  background: var(--color-surface-lowest);
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
  border: 1.5px solid var(--color-surface-low);
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
  border: 1.5px solid var(--color-surface-low);
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
  background: linear-gradient(135deg, var(--color-cyan-bright), /* ponytail: cyan-600 渐变端点，无 token */ #0891b2);
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
  &:active { background: var(--color-surface-low); }
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

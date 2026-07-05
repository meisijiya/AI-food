<template>
  <!--
    ResultComment - 美食评价(评论输入 + emoji 插入 + 保存)
    内部维护 commentText + showEmoji 状态
    保存事件通过 emit('save', text) 回调,API 调用由父组件完成
    saving 状态由父组件传入(用于按钮 disabled / 文案切换)
  -->
  <div v-if="sessionId" class="comment-section animate-fade-up delay-480 animate-start-hidden">
    <div class="comment-label">
      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>
      <span>美食评价</span>
    </div>
    <div class="comment-edit">
      <textarea
        v-model="commentText"
        class="comment-textarea"
        placeholder="写下你对这道美食的评价..."
        rows="3"
        maxlength="500"
      ></textarea>
      <div class="comment-actions">
        <button class="emoji-trigger" @click="showEmoji = !showEmoji">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><path d="M8 14s1.5 2 4 2 4-2 4-2"/><line x1="9" x2="9.01" y1="9" y2="9"/><line x1="15" x2="15.01" y1="9" y2="9"/></svg>
        </button>
        <span class="comment-count">{{ commentText.length }}/500</span>
        <button class="comment-save-btn" @click="onSave" :disabled="saving">
          {{ saving ? '保存中...' : '保存评价' }}
        </button>
      </div>
    </div>

    <!-- Emoji Picker(子组件自己拥有,跟 publish 互不影响) -->
    <EmojiPicker :show="showEmoji" @select="insertEmoji" @close="showEmoji = false" />
  </div>
</template>

<script setup lang="ts">
// ResultComment - 美食评价(子组件)
// 父组件传入 sessionId + saving 状态,子组件维护 commentText + showEmoji
// 通过 emit('save', text) 触发保存,API 调用在父组件
import { ref } from 'vue'
import EmojiPicker from '@/components/EmojiPicker.vue'

defineProps<{
  sessionId: string
  saving: boolean
}>()

const emit = defineEmits<{
  (e: 'save', comment: string): void
}>()

// 内部 UI 状态
const commentText = ref('')
const showEmoji = ref(false)

// ponytail: 原 Result.vue 通过 emojiTarget 状态在 comment/publish 间切换 emoji picker;
// 拆组件后每个组件各自管理 picker,不影响 UX(同一时刻用户只能聚焦一处)
function insertEmoji(icon: string) {
  commentText.value = (commentText.value || '') + icon
  showEmoji.value = false
}

function onSave() {
  emit('save', commentText.value || '')
}
</script>

<style lang="scss" scoped>
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
  background: var(--color-surface-lowest);
  border-radius: 1.25rem;
  border: 1px solid var(--color-surface-low);
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
  &:active { background: var(--color-surface-low); }
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
  background: linear-gradient(135deg, var(--color-primary-soft), var(--color-primary));
  color: white;
  font-family: var(--font-sans);
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  box-shadow: 0 4px 12px /* ponytail: rgba(74, 141, 213, 0.2) */ rgba(74, 141, 213, 0.2);
  transition: all 0.2s;
  &:hover { transform: translateY(-1px); }
  &:active { transform: scale(0.97); }
  &:disabled { opacity: 0.5; cursor: not-allowed; transform: none; }
}
</style>

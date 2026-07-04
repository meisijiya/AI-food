<template>
  <div class="comment-section card-enter card-delay-3">
    <div class="section-title">
      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>
      <span>美食评价</span>
    </div>
    <div v-if="!editingComment && comment" class="comment-display">
      <p class="comment-text">{{ comment }}</p>
      <button class="comment-edit-btn" @click="startEdit">
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
        <button class="emoji-trigger" @click="openEmoji">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><path d="M8 14s1.5 2 4 2 4-2 4-2"/><line x1="9" x2="9.01" y1="9" y2="9"/><line x1="15" x2="15.01" y1="9" y2="9"/></svg>
        </button>
        <span class="comment-count">{{ (commentInput || '').length }}/500</span>
        <button class="comment-save-btn" @click="handleSave" :disabled="savingComment">
          {{ savingComment ? '保存中...' : '保存评价' }}
        </button>
      </div>
    </div>

    <EmojiPicker :show="showEmoji" @select="insertEmoji" @close="showEmoji = false" />
  </div>
</template>

<script setup lang="ts">
// 记录详情 - 美食评价子组件
// 负责: 评价展示、编辑模式切换、emoji 插入、保存提交
// 评价的 API 调用由父组件完成,子组件通过 emit('save') 提交
import { ref, watch } from 'vue'
import EmojiPicker from '@/components/EmojiPicker.vue'

// 父组件传入的 props: 父组件持有的评价文本
const props = defineProps<{
  comment: string
}>()

// 事件回传父组件(API 调用由父组件负责)
const emit = defineEmits<{
  (e: 'save', content: string): void
}>()

// 子组件内部 UI 状态
// 若初始无评价,默认进入编辑模式(原行为)
const editingComment = ref(!props.comment)
const commentInput = ref('')
const savingComment = ref(false)
const showEmoji = ref(false)

// 监听父组件传入的 comment 变化,同步本地编辑态
// 初始 detail 加载时: comment 从 '' 变为实际值
//   - 有评价:退出编辑模式(进入展示模式,匹配原行为)
//   - 无评价:保持编辑模式
// 父组件外部清空评价(理论不会发生):进入编辑模式
watch(() => props.comment, (val) => {
  if (val) {
    editingComment.value = false
  } else {
    editingComment.value = true
  }
})

// 点击编辑按钮:进入编辑模式,预填当前评价
function startEdit() {
  commentInput.value = props.comment
  editingComment.value = true
}

// 切换 emoji 面板
function openEmoji() {
  showEmoji.value = !showEmoji.value
}

// 插入 emoji 到评价输入框
function insertEmoji(icon: string) {
  commentInput.value = (commentInput.value || '') + icon
  showEmoji.value = false
}

// 点击保存: 通知父组件执行 API
function handleSave() {
  savingComment.value = true
  emit('save', commentInput.value || '')
}

// 暴露给父组件的回调 —— 父组件 API 成功后调用,关闭编辑模式
function onSaveSuccess() {
  savingComment.value = false
  editingComment.value = false
}

// 暴露给父组件的回调 —— 父组件 API 失败后调用,保留编辑模式以便重试
function onSaveError() {
  savingComment.value = false
}

defineExpose({ onSaveSuccess, onSaveError })
</script>

<style lang="scss" scoped>
/* ===== Card Entrance Animation (self-contained, no parent dependency) ===== */
.card-enter {
  animation: card-slide-up 0.5s cubic-bezier(0.22, 1, 0.36, 1) both;
}
.card-delay-3 { animation-delay: 0.15s; }

@keyframes card-slide-up {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
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

/* ===== Comment Section ===== */
.comment-section {
  margin-bottom: 24px;
}

.comment-display {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 20px 24px;
  background: var(--color-surface-lowest);
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
  background: rgba(74, 141, 213, 0.06);
  color: var(--color-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.25s;

  &:hover { background: rgba(74, 141, 213, 0.12); }
  &:active { transform: scale(0.92); }
}

.comment-edit {
  padding: 4px;
  background: var(--color-surface-lowest);
  border-radius: 2rem;
  border: 1px solid var(--color-surface-low);
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
  &:active { background: var(--color-surface-low); }
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
  background: linear-gradient(135deg, var(--color-primary-soft), var(--color-primary));
  color: white;
  font-family: var(--font-sans);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.05em;
  cursor: pointer;
  box-shadow: 0 4px 16px rgba(74, 141, 213, 0.2);
  transition: all 0.25s;

  &:hover { transform: translateY(-1px); box-shadow: 0 6px 20px rgba(74, 141, 213, 0.3); }
  &:active { transform: scale(0.97); }
  &:disabled { opacity: 0.5; cursor: not-allowed; transform: none; }
}
</style>

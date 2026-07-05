<template>
  <!-- 消息输入区：附件 / 表情 / 文字 / 发送按钮 + 权限提示 -->
  <div class="input-wrapper">
    <!-- Permission hint -->
    <div v-if="sendPermission === 'not_allowed'" class="permission-hint">
      对方未关注你，无法发送消息
    </div>
    <div v-else-if="sendPermission === 'max_reached'" class="permission-hint">
      非互关好友最多发送 5 条消息，已达上限
    </div>
    <div v-else-if="remaining > 0" class="permission-hint permission-hint-info">
      非互关好友，还可发送 {{ remaining }} 条消息
    </div>

    <!-- Input row -->
    <div class="input-container">
      <button class="plus-btn" :class="{ active: showAttach }" @click="toggleAttach" :disabled="sendPermission !== 'ok'" title="附件">
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="12" x2="12" y1="5" y2="19"/><line x1="5" x2="19" y1="12" y2="12"/></svg>
      </button>
      <button class="emoji-trigger" @click="toggleEmoji" :disabled="sendPermission !== 'ok'" :class="{ active: showEmoji }">
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><path d="M8 14s1.5 2 4 2 4-2 4-2"/><line x1="9" x2="9.01" y1="9" y2="9"/><line x1="15" x2="15.01" y1="9" y2="9"/></svg>
      </button>
      <input
        v-model="inputMessage"
        class="message-input"
        :placeholder="sendPermission === 'ok' ? '输入消息...' : '无法发送消息'"
        :disabled="sendPermission !== 'ok'"
        @keyup.enter="emit('send-text', inputMessage)"
        @focus="emit('input-focus')"
      />
      <button class="send-btn" @click="emit('send-text', inputMessage)" :disabled="!inputMessage.trim() || sendPermission !== 'ok'">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="22" x2="11" y1="2" y2="13"/><polygon points="22 2 15 22 11 13 2 9 22 2"/></svg>
      </button>
    </div>

    <!-- Attachment Panel -->
    <Transition name="panel-slide">
      <div v-if="showAttach" class="attachment-panel" @click.stop>
        <div class="attachment-panel-header">
          <span class="attachment-panel-title"><em>附件</em></span>
          <button class="attachment-close-btn" @click="showAttach = false">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
          </button>
        </div>
        <div class="attachment-grid">
          <button class="attachment-item" @click="triggerPhoto" :disabled="sendPermission !== 'ok' || uploading">
            <div class="attachment-icon photo-icon">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="18" height="18" rx="2" ry="2"/><circle cx="8.5" cy="8.5" r="1.5"/><polyline points="21 15 16 10 5 21"/></svg>
            </div>
            <span class="attachment-label">照片</span>
          </button>
          <button class="attachment-item" @click="triggerFile" :disabled="sendPermission !== 'ok' || uploading">
            <div class="attachment-icon file-icon">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M21.44 11.05l-9.19 9.19a6 6 0 0 1-8.49-8.49l9.19-9.19a4 4 0 0 1 5.66 5.66l-9.2 9.19a2 2 0 0 1-2.83-2.83l8.49-8.48"/></svg>
            </div>
            <span class="attachment-label">文件</span>
          </button>
        </div>
      </div>
    </Transition>

    <!-- Emoji Picker -->
    <Transition name="panel-slide">
      <div v-if="showEmoji" class="emoji-panel-wrapper" @click.stop>
        <EmojiPicker :show="showEmoji" @select="insertEmoji" @close="showEmoji = false" />
      </div>
    </Transition>

    <!-- Hidden file inputs -->
    <input ref="photoInput" type="file" accept="image/*" style="display:none" @change="onPhotoChange" />
    <input ref="fileInput" type="file" style="display:none" @change="onFileChange" />
  </div>
</template>

<script setup lang="ts">
// 消息输入区子组件：纯 UI + 受控文本；发送 / 上传 / emoji 选择全部以事件交给父组件。
import { ref, watch } from 'vue'
import EmojiPicker from '@/components/EmojiPicker.vue'

const props = defineProps<{
  sendPermission: 'ok' | 'max_reached' | 'not_allowed'
  remaining: number
  uploading: boolean
  /** 父级发起的关闭面板信号（递增），用于外部点击 / 输入聚焦时收起 emoji/attach 面板 */
  closePanelsSignal: number
}>()

const emit = defineEmits<{
  (e: 'send-text', text: string): void
  (e: 'send-image', file: File): void
  (e: 'send-file', file: File): void
  (e: 'input-focus'): void
  (e: 'clear-photo-input'): void
  (e: 'clear-file-input'): void
}>()

const inputMessage = ref('')
const showEmoji = ref(false)
const showAttach = ref(false)
const photoInput = ref<HTMLInputElement>()
const fileInput = ref<HTMLInputElement>()

// 监听父级 closePanelsSignal 信号，收起表情 / 附件面板。
watch(() => props.closePanelsSignal, () => {
  showEmoji.value = false
  showAttach.value = false
})

function toggleEmoji() {
  if (props.sendPermission !== 'ok') return
  showAttach.value = false
  showEmoji.value = !showEmoji.value
}

function toggleAttach() {
  if (props.sendPermission !== 'ok') return
  showEmoji.value = false
  showAttach.value = !showAttach.value
}

function insertEmoji(icon: string) {
  inputMessage.value += icon
  showEmoji.value = false
}

function triggerPhoto() {
  if (props.sendPermission !== 'ok') return
  showAttach.value = false
  photoInput.value?.click()
}

function triggerFile() {
  if (props.sendPermission !== 'ok') return
  showAttach.value = false
  fileInput.value?.click()
}

function onPhotoChange(e: Event) {
  const file = (e.target as HTMLInputElement).files?.[0]
  if (!file || props.uploading) return
  emit('send-image', file)
  emit('clear-photo-input')
}

function onFileChange(e: Event) {
  const file = (e.target as HTMLInputElement).files?.[0]
  if (!file || props.uploading) return
  emit('send-file', file)
  emit('clear-file-input')
}

// 暴露给父级：发送成功后清空输入框 / 重置隐藏 input 的 value
defineExpose({
  clearInput() {
    inputMessage.value = ''
  },
  resetPhotoInput() {
    if (photoInput.value) photoInput.value.value = ''
  },
  resetFileInput() {
    if (fileInput.value) fileInput.value.value = ''
  }
})
</script>

<style lang="scss" scoped>
.input-wrapper {
  flex-shrink: 0;
}

.permission-hint {
  padding: 8px 16px;
  text-align: center;
  font-size: 12px;
  color: var(--color-danger-bright);
  background: rgba(239, 68, 68, 0.06);
  flex-shrink: 0;
}

.permission-hint-info {
  color: var(--color-on-surface-variant);
  background: var(--color-surface-low);
}

.input-container {
  display: flex;
  gap: 8px;
  padding: 12px;
  background: var(--color-surface-lowest);
  border-top: 1px solid var(--color-surface-low);
  flex-shrink: 0;
}

.emoji-trigger {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: none;
  border: none;
  color: var(--color-on-surface-variant);
  cursor: pointer;
  border-radius: 50%;
  flex-shrink: 0;
  transition: background 0.2s, color 0.2s;
  &:active { background: var(--color-surface-low); }
  &:disabled { opacity: 0.3; cursor: not-allowed; }
  &.active { color: var(--color-primary); background: rgba(74, 141, 213, 0.08); }
}

.plus-btn {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: none;
  border: none;
  color: var(--color-on-surface-variant);
  cursor: pointer;
  border-radius: 50%;
  flex-shrink: 0;
  transition: background 0.2s, transform 0.2s, color 0.2s;
  &:active { background: var(--color-surface-low); }
  &:disabled { opacity: 0.3; cursor: not-allowed; }
  &.active {
    color: var(--color-primary);
    background: rgba(74, 141, 213, 0.08);
    svg { transform: rotate(45deg); }
  }
}

.message-input {
  flex: 1;
  padding: 10px 16px;
  border: 1.5px solid var(--color-surface-low);
  border-radius: 100px;
  background: var(--color-surface);
  font-family: var(--font-sans);
  font-size: 14px;
  color: var(--color-on-surface);
  outline: none;
  &:focus { border-color: var(--color-primary); }
}

.send-btn {
  width: 44px;
  height: 44px;
  border: none;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--color-primary-soft), var(--color-primary));
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: transform 0.2s;
  flex-shrink: 0;

  &:active:not(:disabled) { transform: scale(0.95); }
  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
}

/* Attachment panel */
.attachment-panel {
  background: rgba(255, 255, 255, 0.6);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-radius: 1.5rem 1.5rem 0 0;
  border: 1px solid rgba(255, 255, 255, 0.8);
  box-shadow: var(--shadow-flat-bottom-sm);
  flex-shrink: 0;
}

.attachment-panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px 8px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.3);
}

.attachment-panel-title {
  font-family: var(--font-serif);
  font-style: italic;
  font-size: 16px;
  font-weight: 500;
  color: var(--color-on-surface-variant);

  em {
    font-style: italic;
  }
}

.attachment-close-btn {
  background: none;
  border: none;
  color: var(--color-on-surface-variant);
  cursor: pointer;
  padding: 4px;
  display: flex;
  align-items: center;
  border-radius: 50%;
  transition: background 0.2s;
  &:active { background: var(--color-overlay-pressed); }
}

.attachment-grid {
  display: flex;
  gap: 16px;
  padding: 20px 24px;
}

.attachment-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 16px 24px;
  border: none;
  border-radius: 1.25rem;
  background: rgba(255, 255, 255, 0.5);
  cursor: pointer;
  transition: background 0.2s, transform 0.15s;

  &:active:not(:disabled) {
    background: rgba(74, 141, 213, 0.08);
    transform: scale(0.95);
  }

  &:disabled {
    opacity: 0.3;
    cursor: not-allowed;
  }
}

.attachment-icon {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;

  &.photo-icon {
    background: linear-gradient(135deg, rgba(74, 141, 213, 0.12), rgba(74, 141, 213, 0.06));
    color: var(--color-primary);
  }

  &.file-icon {
    background: linear-gradient(135deg, rgba(34, 211, 238, 0.15), rgba(34, 211, 238, 0.06));
    color: #0891b2;
  }
}

.attachment-label {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-on-surface);
}

.emoji-panel-wrapper {
  flex-shrink: 0;
}

/* Panel slide transition */
.panel-slide-enter-active {
  transition: transform 0.25s var(--ease-material), opacity 0.2s;
}
.panel-slide-leave-active {
  transition: transform 0.2s var(--ease-material), opacity 0.15s;
}
.panel-slide-enter-from {
  transform: translateY(100%);
  opacity: 0;
}
.panel-slide-leave-to {
  transform: translateY(100%);
  opacity: 0;
}
</style>

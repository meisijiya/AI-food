<template>
  <div class="chat-page">
    <!-- Header -->
    <div class="chat-header animate-fade-up">
      <button class="header-btn" @click="onClickLeft">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m15 18-6-6 6-6"/></svg>
      </button>
      <div class="header-center">
        <div class="header-title">AI 美食助手</div>
        <div class="header-status">
          <span class="status-dot" :class="{ active: chatStore.isConnected }"></span>
          {{ chatStore.isConnected ? '在线' : '连接中...' }}
        </div>
      </div>
      <button class="header-btn" @click="showActions = true">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="1"/><circle cx="12" cy="5" r="1"/><circle cx="12" cy="19" r="1"/></svg>
      </button>
    </div>

    <!-- Progress bar -->
    <div class="progress-section animate-fade-up delay-100 animate-start-hidden">
      <div class="progress-track">
        <div class="progress-fill" :style="{ width: progressPercentage + '%' }"></div>
      </div>
      <span class="progress-label">{{ chatStore.progress.current }} / {{ chatStore.progress.total }}</span>
    </div>

    <!-- Message list -->
    <div class="message-list no-scrollbar" ref="messageListRef">
      <div v-if="!chatStore.isConnected && !connectionFailed" class="state-message">
        <div class="state-loading"></div>
        <p>正在连接 AI 助手...</p>
      </div>

      <div v-if="connectionFailed" class="state-message">
        <p>连接失败，请检查后端服务</p>
        <button class="retry-btn" @click="retryConnect">重新连接</button>
      </div>

      <div
        v-for="message in chatStore.messages"
        :key="message.id"
        :class="['msg', message.type === 'user' ? 'msg-user' : 'msg-ai']"
      >
        <div v-if="message.type !== 'user'" class="msg-avatar msg-avatar-ai">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 8V4H8"/><rect width="16" height="12" x="4" y="8" rx="2"/><path d="M2 14h2"/><path d="M20 14h2"/><path d="M15 13v2"/><path d="M9 13v2"/></svg>
        </div>
        <div :class="['msg-bubble', message.type === '2question' ? 'msg-retry' : '', message.type === 'interrupt' ? 'msg-interrupt' : '']">
          {{ message.content }}
        </div>
        <div v-if="message.type === 'user'" class="msg-avatar msg-avatar-user">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M19 21v-2a4 4 0 0 0-4-4H9a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
        </div>
      </div>

      <div v-if="chatStore.isLoading && chatStore.isConnected" class="thinking-indicator animate-fade-in">
        <div class="thinking-dots"><span></span><span></span><span></span></div>
        <span class="thinking-text">AI 正在思考...</span>
        <span class="thinking-hint">你可以继续说话</span>
      </div>
    </div>

    <!-- Input area -->
    <div class="input-area">
      <div class="input-wrapper">
        <input
          v-model="inputValue"
          type="text"
          :placeholder="chatStore.isLoading ? 'AI 正在思考，你可以继续说...' : '请输入你的回答...'"
          :disabled="!chatStore.isConnected"
          @keyup.enter="sendMessage"
          class="chat-input"
        />
        <button class="send-btn" :disabled="!inputValue.trim() || !chatStore.isConnected" @click="sendMessage">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="m22 2-7 20-4-9-9-4Z"/><path d="M22 2 11 13"/></svg>
        </button>
      </div>
    </div>

    <!-- Sanctuary Action Sheet -->
    <Transition name="overlay-fade">
      <div v-if="showActions" class="sanctuary-overlay" @click="showActions = false">
        <Transition name="sheet-slide">
          <div v-if="showActions" class="sanctuary-sheet" @click.stop>
            <div class="sheet-handle"></div>
            <div class="sheet-actions">
              <button class="sheet-item" @click="onActionSelect('complete')">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20 6 9 17l-5-5"/></svg>
                <span>结束对话</span>
              </button>
              <button class="sheet-item" @click="onActionSelect('restart')">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 12a9 9 0 0 0-9-9 9.75 9.75 0 0 0-6.74 2.74L3 8"/><path d="M3 3v5h5"/><path d="M3 12a9 9 0 0 0 9 9 9.75 9.75 0 0 0 6.74-2.74L21 16"/><path d="M16 16h5v5"/></svg>
                <span>重新开始</span>
              </button>
            </div>
            <button class="sheet-cancel" @click="showActions = false">取消</button>
          </div>
        </Transition>
      </div>
    </Transition>

    <!-- Sanctuary Confirm Dialog -->
    <Transition name="overlay-fade">
      <div v-if="confirmDialog.visible" class="sanctuary-overlay" @click="onConfirmCancel">
        <Transition name="dialog-pop">
          <div v-if="confirmDialog.visible" class="sanctuary-dialog" @click.stop>
            <div class="dialog-icon">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><path d="M12 16v-4"/><path d="M12 8h.01"/></svg>
            </div>
            <h3 class="dialog-title">{{ confirmDialog.title }}</h3>
            <p class="dialog-message">{{ confirmDialog.message }}</p>
            <div class="dialog-actions">
              <button class="dialog-btn dialog-btn-cancel" @click="onConfirmCancel">取消</button>
              <button class="dialog-btn dialog-btn-confirm" @click="onConfirmOk">确定</button>
            </div>
          </div>
        </Transition>
      </div>
    </Transition>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { conversationApi } from '@/api'
import { useChatStore } from '@/stores/chat'
import { WebSocketClient, type WebSocketMessage } from '@/websocket'

const router = useRouter()
const chatStore = useChatStore()
const messageListRef = ref<HTMLElement | null>(null)
const inputValue = ref('')
const showActions = ref(false)
const connectionFailed = ref(false)
const connectionTimeout = ref<ReturnType<typeof setTimeout> | null>(null)

let wsClient: WebSocketClient | null = null

// Custom confirm dialog state
const confirmDialog = reactive({
  visible: false,
  title: '',
  message: '',
  resolve: null as ((value: boolean) => void) | null
})

const showSanctuaryConfirm = (title: string, message: string): Promise<boolean> => {
  confirmDialog.title = title
  confirmDialog.message = message
  confirmDialog.visible = true
  return new Promise((resolve) => {
    confirmDialog.resolve = resolve
  })
}

const onConfirmOk = () => {
  confirmDialog.visible = false
  confirmDialog.resolve?.(true)
}

const onConfirmCancel = () => {
  confirmDialog.visible = false
  confirmDialog.resolve?.(false)
}

const progressPercentage = computed(() => {
  if (chatStore.progress.total === 0) return 0
  return Math.round((chatStore.progress.current / chatStore.progress.total) * 100)
})

const scrollToBottom = () => {
  nextTick(() => {
    if (messageListRef.value) {
      messageListRef.value.scrollTop = messageListRef.value.scrollHeight
    }
  })
}

const clearConnectionTimeout = () => {
  if (connectionTimeout.value) {
    clearTimeout(connectionTimeout.value)
    connectionTimeout.value = null
  }
}

const handleWebSocketMessage = (message: WebSocketMessage) => {
  clearConnectionTimeout()
  connectionFailed.value = false

  chatStore.addMessage({
    type: message.type,
    param: message.param,
    content: message.content
  })

  if (message.progress) {
    chatStore.updateProgress(message.progress)
  }

  if (message.type === 'recommend') {
    chatStore.setPhase('recommend')
    chatStore.setRecommendationResult(message.content)
    router.push('/result')
    return
  }

  if (message.type !== 'interrupt') {
    chatStore.setLoading(false)
  }

  scrollToBottom()
}

const handleWebSocketOpen = () => {
  clearConnectionTimeout()
  connectionFailed.value = false
  chatStore.setConnected(true)
  chatStore.setLoading(true)
}

const handleWebSocketError = () => {
  clearConnectionTimeout()
}

const handleWebSocketClose = () => {
  chatStore.setConnected(false)
  if (chatStore.messages.length === 0) {
    connectionFailed.value = true
  }
}

const sendMessage = () => {
  if (!inputValue.value.trim() || !chatStore.isConnected) return

  const isInterrupt = chatStore.isLoading

  chatStore.addMessage({
    type: 'user',
    content: inputValue.value
  })

  wsClient?.answer(inputValue.value)
  inputValue.value = ''

  if (!isInterrupt) {
    chatStore.setLoading(true)
  }
  scrollToBottom()
}

const retryConnect = () => {
  connectionFailed.value = false
  chatStore.setConnected(false)
  wsClient?.disconnect()
  startWebSocket()
}

const startWebSocket = () => {
  wsClient = new WebSocketClient({
    sessionId: chatStore.sessionId,
    onOpen: handleWebSocketOpen,
    onMessage: handleWebSocketMessage,
    onError: handleWebSocketError,
    onClose: handleWebSocketClose
  })
  wsClient.connect()

  connectionTimeout.value = setTimeout(() => {
    if (!chatStore.isConnected) {
      connectionFailed.value = true
    }
  }, 8000)
}

const onClickLeft = async () => {
  showActions.value = false
  const ok = await showSanctuaryConfirm('离开对话', '确定要离开当前对话吗？当前对话数据将不会保存。')
  if (ok) {
    const sid = chatStore.sessionId
    wsClient?.disconnect()
    // 用 HTTP API 确保服务端删除数据
    if (sid) {
      try { await conversationApi.cancel(sid) } catch { /* ignore */ }
    }
    chatStore.clearChat()
    router.push('/')
  }
}

const onActionSelect = async (action: string) => {
  showActions.value = false
  switch (action) {
    case 'complete': {
      const ok = await showSanctuaryConfirm('结束对话', '确定要结束对话并获取推荐吗？')
      if (ok) wsClient?.complete()
      break
    }
    case 'restart': {
      const ok = await showSanctuaryConfirm('重新开始', '确定要重新开始对话吗？当前对话数据将不会保存。')
      if (ok) {
        // 不断连，发 reset 让后端删除数据并重置，前端清空消息
        chatStore.messages = []
        chatStore.progress = { current: 0, total: 7, collected: [] }
        chatStore.collectedParamValues = {}
        chatStore.setLoading(true)
        chatStore.setPhase('chat')
        chatStore.setRecommendationResult(null)
        wsClient?.reset()
      }
      break
    }
  }
}

onMounted(() => {
  if (!chatStore.sessionId) { router.push('/'); return }
  startWebSocket()
})

onUnmounted(() => {
  clearConnectionTimeout()
  wsClient?.disconnect()
})
</script>

<style lang="scss" scoped>
.chat-page {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: var(--color-surface);
}

/* Header */
.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background: var(--color-surface-container-lowest);
  border-bottom: 1px solid var(--color-surface-container-low);
}

.header-btn {
  width: 40px; height: 40px; border: none; background: none;
  border-radius: 1rem; display: flex; align-items: center; justify-content: center;
  color: var(--color-on-surface-variant); cursor: pointer; transition: background 0.2s;
  &:active { background: var(--color-surface-container-low); }
}

.header-center { text-align: center; }

.header-title {
  font-family: var(--font-serif); font-style: italic;
  font-size: 17px; font-weight: 500; color: var(--color-on-surface);
}

.header-status {
  display: flex; align-items: center; justify-content: center; gap: 5px;
  font-size: 10px; font-weight: 600; text-transform: uppercase;
  letter-spacing: 0.15em; color: var(--color-on-surface-variant); margin-top: 2px;
}

.status-dot {
  width: 6px; height: 6px; border-radius: 50%; background: #ccc; transition: background 0.3s;
  &.active { background: #22c55e; box-shadow: 0 0 8px rgba(34,197,94,0.4); }
}

/* Progress */
.progress-section {
  display: flex; align-items: center; gap: 12px;
  padding: 10px 20px; background: var(--color-surface-container-lowest);
}

.progress-track {
  flex: 1; height: 4px; background: var(--color-surface-container-low);
  border-radius: 2px; overflow: hidden;
}

.progress-fill {
  height: 100%; background: linear-gradient(90deg, var(--color-primary-container), var(--color-primary));
  border-radius: 2px; transition: width 0.6s cubic-bezier(0.22,1,0.36,1);
}

.progress-label {
  font-size: 11px; font-weight: 700; color: var(--color-on-surface-variant);
  letter-spacing: 0.05em; white-space: nowrap;
}

/* Message list */
.message-list {
  flex: 1; overflow-y: auto; padding: 20px 16px;
  display: flex; flex-direction: column; gap: 16px;
}

.state-message {
  text-align: center; padding: 60px 0; color: var(--color-on-surface-variant); font-size: 14px;
  p { margin-top: 12px; }
}

.state-loading {
  width: 32px; height: 32px; border: 3px solid var(--color-surface-container-low);
  border-top-color: var(--color-primary); border-radius: 50%; margin: 0 auto;
  animation: spin 0.8s linear infinite;
}

.retry-btn {
  margin-top: 16px; padding: 10px 28px; border: none; border-radius: 2rem;
  background: var(--color-primary); color: white; font-size: 13px; font-weight: 600; cursor: pointer;
}

/* Messages */
.msg {
  display: flex; align-items: flex-start; gap: 10px; max-width: 85%;
  &.msg-user { align-self: flex-end; flex-direction: row-reverse; }
  &.msg-ai { align-self: flex-start; }
}

.msg-avatar {
  width: 34px; height: 34px; border-radius: 1rem;
  display: flex; align-items: center; justify-content: center; flex-shrink: 0;
}

.msg-avatar-ai {
  background: linear-gradient(135deg, var(--color-primary-container), var(--color-primary)); color: white;
}

.msg-avatar-user {
  background: var(--color-surface-container-low); color: var(--color-on-surface-variant);
}

.msg-bubble {
  padding: 14px 18px; border-radius: 1.25rem; font-size: 14px; line-height: 1.6; word-break: break-word;

  .msg-ai & {
    background: var(--color-surface-container-lowest); color: var(--color-on-surface);
    border: 1px solid rgba(255,255,255,0.8); border-bottom-left-radius: 0.5rem;
    box-shadow: 0 2px 8px rgba(0,0,0,0.04);
  }

  .msg-user & {
    background: linear-gradient(135deg, var(--color-primary-container), var(--color-primary));
    color: white; border-bottom-right-radius: 0.5rem;
    box-shadow: 0 4px 16px rgba(0,89,182,0.2);
  }

  &.msg-retry { background: #fff7ed !important; color: #92400e !important; border: 1px solid #fed7aa !important; }
  &.msg-interrupt { background: linear-gradient(135deg, #fefce8, #fef9c3) !important; color: #854d0e !important; border: 1px solid #fde68a !important; }
}

/* Thinking indicator */
.thinking-indicator {
  display: flex; align-items: center; gap: 10px; align-self: flex-start;
  padding: 12px 18px; background: var(--color-surface-container-lowest);
  border-radius: 1.25rem; border-bottom-left-radius: 0.5rem;
  border: 1px solid rgba(255,255,255,0.8); box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}

.thinking-dots {
  display: flex; gap: 4px;
  span {
    width: 6px; height: 6px; border-radius: 50%; background: var(--color-primary-container);
    animation: dot-bounce 1.4s ease-in-out infinite;
    &:nth-child(2) { animation-delay: 0.2s; }
    &:nth-child(3) { animation-delay: 0.4s; }
  }
}

@keyframes dot-bounce {
  0%, 80%, 100% { transform: scale(0.6); opacity: 0.4; }
  40% { transform: scale(1); opacity: 1; }
}

/* Input */
.input-area {
  padding: 12px 16px 24px; background: var(--color-surface-container-lowest);
  border-top: 1px solid var(--color-surface-container-low);
}

.input-wrapper {
  display: flex; align-items: center; gap: 10px; background: var(--color-surface);
  border-radius: 2rem; padding: 4px 4px 4px 20px; border: 1px solid var(--color-surface-container-low);
  transition: border-color 0.2s;
  &:focus-within { border-color: var(--color-primary-container); }
}

.chat-input {
  flex: 1; border: none; background: none; font-family: var(--font-sans);
  font-size: 14px; color: var(--color-on-surface); outline: none;
  &::placeholder { color: var(--color-on-surface-variant); opacity: 0.6; }
  &:disabled { opacity: 0.5; }
}

.send-btn {
  width: 40px; height: 40px; border: none; border-radius: 50%;
  background: linear-gradient(135deg, var(--color-primary-container), var(--color-primary));
  color: white; display: flex; align-items: center; justify-content: center;
  cursor: pointer; flex-shrink: 0; transition: all 0.2s;
  box-shadow: 0 4px 12px rgba(0,89,182,0.25);
  &:hover { transform: scale(1.05); }
  &:active { transform: scale(0.95); }
  &:disabled { opacity: 0.4; cursor: not-allowed; transform: none; box-shadow: none; }
}

/* ============================================
   Sanctuary Action Sheet
   ============================================ */

.sanctuary-overlay {
  position: fixed; inset: 0; z-index: 100;
  background: rgba(11, 15, 16, 0.4); backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  display: flex; align-items: flex-end; justify-content: center;
}

.sanctuary-sheet {
  width: 100%; max-width: 480px;
  background: var(--color-surface-container-lowest);
  border-radius: 2rem 2rem 0 0;
  padding: 12px 20px 32px;
  box-shadow: 0 -8px 40px rgba(0, 0, 0, 0.1);
}

.sheet-handle {
  width: 36px; height: 4px; border-radius: 2px;
  background: var(--color-surface-container-low);
  margin: 0 auto 20px;
}

.sheet-actions {
  display: flex; flex-direction: column; gap: 4px; margin-bottom: 16px;
}

.sheet-item {
  display: flex; align-items: center; gap: 14px;
  width: 100%; padding: 16px 20px;
  border: none; background: none; border-radius: 1.25rem;
  font-family: var(--font-sans); font-size: 15px; font-weight: 500;
  color: var(--color-on-surface); cursor: pointer; transition: background 0.2s;
  text-align: left;

  svg { color: var(--color-primary); flex-shrink: 0; }

  &:active { background: var(--color-surface-container-low); }
}

.sheet-cancel {
  width: 100%; padding: 14px;
  border: 1.5px solid var(--color-surface-container-low);
  border-radius: 1.5rem; background: none;
  font-family: var(--font-sans); font-size: 14px; font-weight: 600;
  color: var(--color-on-surface-variant); cursor: pointer; transition: all 0.2s;
  &:active { background: var(--color-surface-container-low); }
}

/* ============================================
   Sanctuary Confirm Dialog
   ============================================ */

.sanctuary-dialog {
  width: calc(100% - 48px); max-width: 360px;
  background: var(--color-surface-container-lowest);
  border-radius: 2rem; padding: 36px 28px 24px;
  text-align: center;
  box-shadow: 0 24px 64px rgba(0, 0, 0, 0.15);
  margin: auto 24px;
}

.dialog-icon {
  width: 52px; height: 52px; border-radius: 50%;
  background: var(--color-surface-container-low);
  display: flex; align-items: center; justify-content: center;
  margin: 0 auto 20px;
  color: var(--color-primary);
}

.dialog-title {
  font-family: var(--font-serif); font-style: italic;
  font-size: 22px; font-weight: 500; color: var(--color-on-surface);
  margin-bottom: 10px;
}

.dialog-message {
  font-size: 14px; color: var(--color-on-surface-variant);
  line-height: 1.6; margin-bottom: 28px;
}

.dialog-actions {
  display: flex; gap: 10px;
}

.dialog-btn {
  flex: 1; padding: 14px; border-radius: 1.25rem;
  font-family: var(--font-sans); font-size: 14px; font-weight: 600;
  cursor: pointer; transition: all 0.2s; border: none;
}

.dialog-btn-cancel {
  background: var(--color-surface-container-low);
  color: var(--color-on-surface-variant);
  &:active { background: var(--color-surface-container-low); opacity: 0.8; }
}

.dialog-btn-confirm {
  background: linear-gradient(135deg, var(--color-primary-container), var(--color-primary));
  color: white;
  box-shadow: 0 4px 16px rgba(0, 89, 182, 0.25);
  &:active { transform: scale(0.97); }
}

/* ============================================
   Transitions
   ============================================ */

.overlay-fade-enter-active { transition: opacity 0.25s ease; }
.overlay-fade-leave-active { transition: opacity 0.2s ease; }
.overlay-fade-enter-from,
.overlay-fade-leave-to { opacity: 0; }

.sheet-slide-enter-active { transition: transform 0.35s cubic-bezier(0.22, 1, 0.36, 1); }
.sheet-slide-leave-active { transition: transform 0.25s ease-in; }
.sheet-slide-enter-from,
.sheet-slide-leave-to { transform: translateY(100%); }

.dialog-pop-enter-active { transition: all 0.3s cubic-bezier(0.22, 1, 0.36, 1); }
.dialog-pop-leave-active { transition: all 0.2s ease-in; }
.dialog-pop-enter-from { opacity: 0; transform: scale(0.9); }
.dialog-pop-leave-to { opacity: 0; transform: scale(0.95); }

@keyframes spin { to { transform: rotate(360deg); } }
</style>

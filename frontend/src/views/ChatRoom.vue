<template>
  <div class="chat-room-container">
    <!-- Header -->
    <div class="room-header">
      <button class="back-btn" @click="router.back()">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m15 18-6-6 6-6"/></svg>
      </button>
      <div class="header-info">
        <div class="header-name">{{ nickname }}</div>
      </div>
      <div class="header-placeholder"></div>
    </div>

    <!-- Messages -->
    <div class="messages-container" ref="messagesContainer" @scroll="onScroll">
      <!-- Load more indicator -->
      <div v-if="loadingMore" class="load-more">
        <div class="spinner"></div>
      </div>
      <div v-if="!hasMore && messages.length > 0" class="no-more">没有更多消息了</div>

      <div 
        v-for="msg in messages" 
        :key="msg.id" 
        class="message-item"
        :class="{ 'message-self': msg.senderId === currentUserId }"
      >
        <div v-if="msg.senderId !== currentUserId" class="msg-avatar">
          <img v-if="partnerAvatar" :src="partnerAvatar" alt="" />
          <span v-else>{{ nickname?.charAt(0) || '?' }}</span>
        </div>
        <div class="message-bubble">
          <div class="message-content">{{ msg.content }}</div>
          <div class="message-time">{{ formatTime(msg.createdAt) }}</div>
        </div>
        <div v-if="msg.senderId === currentUserId" class="msg-avatar msg-avatar-self">
          <img v-if="myAvatar" :src="myAvatar" alt="" />
          <span v-else>{{ authStore.userInfo?.nickname?.charAt(0) || '?' }}</span>
        </div>
      </div>
    </div>

    <!-- Input -->
    <div class="input-container">
      <input 
        v-model="inputMessage" 
        class="message-input" 
        placeholder="输入消息..." 
        @keyup.enter="sendMessage"
      />
      <button class="send-btn" @click="sendMessage" :disabled="!inputMessage.trim()">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="22" x2="11" y1="2" y2="13"/><polygon points="22 2 15 22 11 13 2 9 22 2"/></svg>
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { chatApi } from '@/api'
import chatWs from '@/websocket/chat'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const currentUserId = computed(() => authStore.userInfo?.userId)
const targetUserId = computed(() => Number(route.query.userId))
const conversationId = computed(() => Number(route.query.conversationId))
const nickname = computed(() => route.query.nickname as string || '聊天')
const partnerAvatar = computed(() => route.query.avatar as string || '')
const myAvatar = computed(() => authStore.userInfo?.avatar || '')

const messages = ref<any[]>([])
const inputMessage = ref('')
const messagesContainer = ref<HTMLElement>()
const currentPage = ref(0)
const hasMore = ref(true)
const loadingMore = ref(false)
const isLoading = ref(false)

const CACHE_PREFIX = 'chat:cache:'
const CACHE_EXPIRE_MS = 5 * 60 * 1000 // 5 分钟
const PAGE_SIZE = 20

// ==================== 缓存 ====================

function getCacheKey(convId: number) {
  return `${CACHE_PREFIX}${convId}`
}

function loadFromCache(convId: number): any[] | null {
  try {
    const raw = localStorage.getItem(getCacheKey(convId))
    if (!raw) return null
    const { data, timestamp } = JSON.parse(raw)
    if (Date.now() - timestamp > CACHE_EXPIRE_MS) {
      localStorage.removeItem(getCacheKey(convId))
      return null
    }
    return data
  } catch {
    return null
  }
}

function saveToCache(convId: number, data: any[]) {
  try {
    localStorage.setItem(getCacheKey(convId), JSON.stringify({
      data,
      timestamp: Date.now()
    }))
  } catch { /* quota exceeded, ignore */ }
}

// ==================== 消息加载 ====================

async function fetchMessages(append = false) {
  const convId = conversationId.value
  if (!convId || isNaN(convId)) return
  if (isLoading.value) return

  isLoading.value = true
  if (append) loadingMore.value = true

  try {
    const res = await chatApi.getMessages(convId, { page: currentPage.value, size: PAGE_SIZE })
    const newItems = (res?.items || []).reverse()

    if (append) {
      // 追加到头部（更早的消息）
      messages.value = [...newItems, ...messages.value]
    } else {
      messages.value = newItems
      scrollToBottom()
    }

    hasMore.value = currentPage.value < (res?.totalPages ?? 1) - 1

    // 更新缓存
    saveToCache(convId, messages.value)
  } catch {
    /* ignore */
  } finally {
    isLoading.value = false
    loadingMore.value = false
  }
}

function onScroll() {
  const el = messagesContainer.value
  if (!el || loadingMore.value || !hasMore.value) return
  // 滚动到顶部时加载更多
  if (el.scrollTop < 60) {
    const prevScrollHeight = el.scrollHeight
    currentPage.value++
    fetchMessages(true).then(() => {
      nextTick(() => {
        // 保持滚动位置
        const newScrollHeight = el.scrollHeight
        el.scrollTop = newScrollHeight - prevScrollHeight
      })
    })
  }
}

// ==================== 发送消息 ====================

function sendMessage() {
  const content = inputMessage.value.trim()
  if (!content) return

  chatWs.sendMessage(targetUserId.value, content)

  const localMsg = {
    id: Date.now(),
    conversationId: conversationId.value,
    senderId: currentUserId.value,
    receiverId: targetUserId.value,
    content,
    messageType: 'text',
    isRead: false,
    createdAt: new Date().toISOString()
  }
  messages.value.push(localMsg)
  inputMessage.value = ''
  scrollToBottom()

  // 更新缓存
  saveToCache(conversationId.value, messages.value)
}

// ==================== WebSocket 消息 ====================

function handleNewMessage(data: any) {
  if (data.conversationId === conversationId.value && data.senderId !== currentUserId.value) {
    messages.value.push(data)
    scrollToBottom()
    saveToCache(conversationId.value, messages.value)
    chatWs.markRead(conversationId.value)
  }
}

function handleSentMessage(data: any) {
  if (data.conversationId === conversationId.value) {
    const lastIndex = messages.value.length - 1
    if (lastIndex >= 0 && messages.value[lastIndex].id > 1000000000000) {
      messages.value[lastIndex] = data
      saveToCache(conversationId.value, messages.value)
    }
  }
}

// ==================== 工具函数 ====================

function scrollToBottom() {
  nextTick(() => {
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
    }
  })
}

function formatTime(dateStr: string): string {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
}

// ==================== 生命周期 ====================

onMounted(() => {
  const convId = conversationId.value

  // 尝试从缓存加载
  if (convId && !isNaN(convId)) {
    const cached = loadFromCache(convId)
    if (cached && cached.length > 0) {
      messages.value = cached
      currentPage.value = Math.ceil(cached.length / PAGE_SIZE) - 1
      hasMore.value = cached.length >= PAGE_SIZE
      scrollToBottom()
      // 后台刷新最新数据
      currentPage.value = 0
      fetchMessages(false)
    } else {
      fetchMessages(false)
    }
  }

  chatWs.on('message', handleNewMessage)
  chatWs.on('sent', handleSentMessage)

  if (!chatWs.connected) {
    chatWs.connect()
  }

  if (convId && !isNaN(convId)) {
    chatWs.markRead(convId)
  }
})

onUnmounted(() => {
  chatWs.off('message', handleNewMessage)
  chatWs.off('sent', handleSentMessage)
})
</script>

<style lang="scss" scoped>
.chat-room-container {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--color-surface);
}

.room-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 40px 12px 12px;
  background: var(--color-surface-container-lowest);
  border-bottom: 1px solid var(--color-surface-container-low);
  flex-shrink: 0;
}

.back-btn {
  width: 36px;
  height: 36px;
  border: none;
  border-radius: 50%;
  background: none;
  color: var(--color-on-surface);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  &:active { background: var(--color-surface-container-low); }
}

.header-info {
  flex: 1;
  text-align: center;
}

.header-name {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-on-surface);
}

.header-placeholder {
  width: 36px;
}

.messages-container {
  flex: 1;
  overflow-y: auto;
  padding: 16px 12px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  -webkit-overflow-scrolling: touch;
}

.load-more {
  display: flex;
  justify-content: center;
  padding: 12px 0;
}

.spinner {
  width: 20px;
  height: 20px;
  border: 2px solid var(--color-surface-container-low);
  border-top-color: var(--color-primary);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin { to { transform: rotate(360deg); } }

.no-more {
  text-align: center;
  font-size: 12px;
  color: var(--color-on-surface-variant);
  opacity: 0.5;
  padding: 8px 0;
}

.message-item {
  display: flex;
  align-items: flex-end;
  gap: 8px;

  &.message-self {
    justify-content: flex-end;
    .message-bubble {
      background: linear-gradient(135deg, var(--color-primary-container), var(--color-primary));
      color: white;
      border-radius: 1.25rem 1.25rem 4px 1.25rem;
    }
    .message-time {
      color: rgba(255, 255, 255, 0.7);
    }
  }
}

.msg-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--color-primary-container), var(--color-primary));
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-family: var(--font-serif);
  font-style: italic;
  font-size: 13px;
  font-weight: 400;
  flex-shrink: 0;
  overflow: hidden;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}

.message-bubble {
  max-width: 70%;
  padding: 10px 14px;
  background: var(--color-surface-container-lowest);
  border-radius: 1.25rem 1.25rem 1.25rem 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.message-content {
  font-size: 14px;
  line-height: 1.5;
  word-break: break-word;
}

.message-time {
  font-size: 10px;
  color: var(--color-on-surface-variant);
  margin-top: 4px;
  text-align: right;
}

.input-container {
  display: flex;
  gap: 8px;
  padding: 12px;
  background: var(--color-surface-container-lowest);
  border-top: 1px solid var(--color-surface-container-low);
  flex-shrink: 0;
}

.message-input {
  flex: 1;
  padding: 10px 16px;
  border: 1.5px solid var(--color-surface-container-low);
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
  background: linear-gradient(135deg, var(--color-primary-container), var(--color-primary));
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
</style>

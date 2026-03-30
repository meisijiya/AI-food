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
      <button class="clear-btn" @click="handleClearChat">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg>
      </button>
    </div>

    <!-- Messages -->
    <div class="messages-container" ref="messagesContainer" @scroll="onScroll" @click="closePanels">
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

        <!-- 图片消息左侧删除按钮 -->
        <div v-if="msg.messageType === 'image'" class="delete-btn-wrapper">
          <button class="delete-btn delete-btn-image" @click.stop="handleDeletePhoto(msg)">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
          </button>
        </div>

        <div class="message-bubble" :class="{ 'bubble-clickable': msg.messageType === 'text' || msg.messageType === 'file' }" @click.stop="handleMessageClick(msg)">
          <!-- 文本消息 -->
          <div v-if="msg.messageType === 'text'" class="message-content">{{ msg.content }}</div>
          <!-- 图片消息 -->
          <div v-else-if="msg.messageType === 'image'" class="message-image" @click.stop="previewImage(msg)">
            <CachedImage :src="parseMediaUrl(msg.content, 'thumbnailUrl')" alt="图片" :lazy="true" />
          </div>
          <!-- 文件消息 -->
          <div v-else-if="msg.messageType === 'file'" class="message-file">
            <div class="file-icon">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>
            </div>
            <div class="file-info">
              <div class="file-name">{{ parseMediaUrl(msg.content, 'fileName') }}</div>
              <div class="file-size">{{ formatFileSize(parseMediaUrl(msg.content, 'fileSize')) }}</div>
            </div>
          </div>
          <!-- 兜底 -->
          <div v-else class="message-content">{{ msg.content }}</div>
          <div class="message-time">{{ formatTime(msg.createdAt) }}</div>
        </div>

        <!-- 消息操作面板 -->
        <div v-if="activeMessageId === msg.id" class="message-actions-panel" :class="{ 'panel-self': msg.senderId === currentUserId }">
          <template v-if="msg.messageType === 'text'">
            <button class="action-btn" @click.stop="handleCopyContent(msg)">复制内容</button>
            <button class="action-btn action-btn-delete" @click.stop="handleDeleteMessage(msg)">删除</button>
          </template>
          <template v-else-if="msg.messageType === 'file'">
            <button class="action-btn" @click.stop="handleDownloadFile(msg)">下载</button>
            <button class="action-btn action-btn-delete" @click.stop="handleDeleteFile(msg)">删除</button>
          </template>
        </div>

        <div v-if="msg.senderId === currentUserId" class="msg-avatar msg-avatar-self">
          <img v-if="myAvatar" :src="myAvatar" alt="" />
          <span v-else>{{ authStore.userInfo?.nickname?.charAt(0) || '?' }}</span>
        </div>
      </div>
    </div>

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

    <!-- Input -->
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
        @keyup.enter="sendMessage"
        @focus="closePanels"
      />
      <button class="send-btn" @click="sendMessage" :disabled="!inputMessage.trim() || sendPermission !== 'ok'">
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
    <input ref="photoInput" type="file" accept="image/*" style="display:none" @change="handlePhotoUpload" />
    <input ref="fileInput" type="file" style="display:none" @change="handleFileUpload" />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { chatApi, uploadApi } from '@/api'
import { showError, showSuccess } from '@/utils/toast'
import { getCachedUser } from '@/utils/userCache'
import { showImagePreview, showConfirmDialog } from 'vant'
import chatWs from '@/websocket/chat'
import EmojiPicker from '@/components/EmojiPicker.vue'
import CachedImage from '@/components/CachedImage.vue'
import type { ChatMessage } from '@/types/chat'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const currentUserId = computed(() => authStore.userInfo?.userId)
const targetUserId = computed(() => Number(route.query.userId))
const conversationId = ref<number>(Number(route.query.conversationId))
const nickname = computed(() => {
  const q = route.query.nickname as string
  if (q) return q
  const cached = getCachedUser(targetUserId.value)
  return cached?.nickname || '聊天'
})
const partnerAvatar = computed(() => {
  const q = route.query.avatar as string
  if (q) return q
  const cached = getCachedUser(targetUserId.value)
  return cached?.avatar || ''
})
const myAvatar = computed(() => authStore.userInfo?.avatar || '')

const messages = ref<ChatMessage[]>([])
const inputMessage = ref('')
const messagesContainer = ref<HTMLElement>()
const currentPage = ref(0)
const hasMore = ref(true)
const loadingMore = ref(false)
const isLoading = ref(false)
const showEmoji = ref(false)
const showAttach = ref(false)
const photoInput = ref<HTMLInputElement>()
const fileInput = ref<HTMLInputElement>()
const uploading = ref(false)
const activeMessageId = ref<number | null>(null)

// 发送权限
const sendPermission = ref<'ok' | 'max_reached' | 'not_allowed'>('ok')
const remaining = ref(-1) // -1 = 无限制

async function checkPermission() {
  try {
    const res = await chatApi.checkPermission(targetUserId.value)
    sendPermission.value = res?.permission || 'not_allowed'
    remaining.value = res?.remaining ?? 0
  } catch {
    sendPermission.value = 'not_allowed'
    remaining.value = 0
  }
}

const CACHE_PREFIX = 'aifood:chat:cache:'
const CACHE_EXPIRE_MS = 5 * 60 * 1000 // 5 分钟
const PAGE_SIZE = 20

// ==================== 缓存 ====================

function getCacheKey(convId: number) {
  return `${CACHE_PREFIX}${convId}`
}

function loadFromCache(convId: number): ChatMessage[] | null {
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

function insertEmoji(icon: string) {
  inputMessage.value += icon
  showEmoji.value = false
}

function toggleEmoji() {
  if (sendPermission.value !== 'ok') return
  showAttach.value = false
  showEmoji.value = !showEmoji.value
}

function toggleAttach() {
  if (sendPermission.value !== 'ok') return
  showEmoji.value = false
  showAttach.value = !showAttach.value
}

function closePanels() {
  showEmoji.value = false
  showAttach.value = false
  activeMessageId.value = null
}

/**
 * 获取当前登录用户 ID；缺失时中断发送链路，避免构造非法消息对象。
 */
function getRequiredCurrentUserId(): number | null {
  const userId = currentUserId.value
  if (typeof userId !== 'number' || Number.isNaN(userId)) {
    showError('当前登录状态无效，请重新进入聊天页面')
    return null
  }
  return userId
}

function sendMessage() {
  const content = inputMessage.value.trim()
  if (!content || sendPermission.value !== 'ok') return
  const senderId = getRequiredCurrentUserId()
  if (senderId === null) return

  chatWs.sendMessage(targetUserId.value, content)

  const localMsg: ChatMessage = {
    id: Date.now(),
    conversationId: conversationId.value,
    senderId,
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

  // 非互关：更新剩余条数
  if (remaining.value > 0) {
    remaining.value--
    if (remaining.value <= 0) {
      sendPermission.value = 'max_reached'
    }
  }
}

// ==================== 图片/文件收发 ====================

function triggerPhoto() {
  if (sendPermission.value !== 'ok') return
  showAttach.value = false
  photoInput.value?.click()
}

function triggerFile() {
  if (sendPermission.value !== 'ok') return
  showAttach.value = false
  fileInput.value?.click()
}

async function handlePhotoUpload(e: Event) {
  const file = (e.target as HTMLInputElement).files?.[0]
  if (!file || uploading.value) return
  const senderId = getRequiredCurrentUserId()
  if (senderId === null) return
  uploading.value = true

  try {
    const res = await uploadApi.uploadChatPhoto(file)
    const content = JSON.stringify({
      thumbnailUrl: res.thumbnailUrl,
      originalUrl: res.originalUrl,
      fileName: res.fileName
    })
    chatWs.sendMessage(targetUserId.value, content, 'image', res.photoId)

    const localMsg: ChatMessage = {
      id: Date.now(),
      conversationId: conversationId.value,
      senderId,
      receiverId: targetUserId.value,
      content,
      messageType: 'image',
      photoId: res.photoId,
      isRead: false,
      createdAt: new Date().toISOString()
    }
    messages.value.push(localMsg)
    saveToCache(conversationId.value, messages.value)
    scrollToBottom()
  } catch (err: any) {
    showError(err?.message || '图片发送失败')
  } finally {
    uploading.value = false
    if (photoInput.value) photoInput.value.value = ''
  }
}

async function handleFileUpload(e: Event) {
  const file = (e.target as HTMLInputElement).files?.[0]
  if (!file || uploading.value) return
  const senderId = getRequiredCurrentUserId()
  if (senderId === null) return

  if (file.size > 50 * 1024 * 1024) {
    showError('文件过大，无法发送！请选择小于 50MB 的文件')
    return
  }

  uploading.value = true

  try {
    const res = await uploadApi.uploadChatFile(file)
    const content = JSON.stringify({
      fileUrl: res.fileUrl,
      fileName: res.fileName,
      fileSize: res.fileSize
    })
    chatWs.sendMessage(targetUserId.value, content, 'file', undefined, res.fileId)

    const localMsg: ChatMessage = {
      id: Date.now(),
      conversationId: conversationId.value,
      senderId,
      receiverId: targetUserId.value,
      content,
      messageType: 'file',
      fileId: res.fileId,
      isRead: false,
      createdAt: new Date().toISOString()
    }
    messages.value.push(localMsg)
    saveToCache(conversationId.value, messages.value)
    scrollToBottom()
  } catch (err: any) {
    showError(err?.message || '文件发送失败')
  } finally {
    uploading.value = false
    if (fileInput.value) fileInput.value.value = ''
  }
}

function parseMediaUrl(content: string | object, field: string): string {
  try {
    if (!content) return ''
    const data = typeof content === 'string' ? JSON.parse(content) : content
    return data?.[field] || ''
  } catch {
    return ''
  }
}

function previewImage(msg: ChatMessage) {
  try {
    const content = msg.content
    const data = typeof content === 'string' ? JSON.parse(content) : content
    showImagePreview([data?.originalUrl || data?.thumbnailUrl])
  } catch { /* ignore */ }
}

function downloadFile(msg: ChatMessage) {
  try {
    const content = msg.content
    const data = typeof content === 'string' ? JSON.parse(content) : content
    const link = document.createElement('a')
    link.href = data?.fileUrl || data?.originalUrl || ''
    link.download = data?.fileName || 'file'
    link.click()
  } catch { /* ignore */ }
}

function formatFileSize(size: string | number): string {
  const bytes = Number(size) || 0
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

// ==================== 消息操作 ====================

function handleMessageClick(msg: ChatMessage) {
  if (msg.messageType === 'image') {
    previewImage(msg)
    return
  }
  if (activeMessageId.value === msg.id) {
    activeMessageId.value = null
  } else {
    activeMessageId.value = msg.id
  }
}

function handleCopyContent(msg: ChatMessage) {
  navigator.clipboard.writeText(msg.content).then(() => {
    showSuccess('已复制')
    activeMessageId.value = null
  }).catch(() => {
    showError('复制失败')
  })
}

async function handleDeleteMessage(msg: ChatMessage) {
  activeMessageId.value = null
  if (!msg.id) return
  try {
    await showConfirmDialog({ title: '删除消息', message: '确定删除这条消息吗？' })
    await chatApi.deleteMessage(msg.id)
    showSuccess('已删除')
    messages.value = messages.value.filter(m => m.id !== msg.id)
    saveToCache(conversationId.value, messages.value)
  } catch (err: any) {
    if (err === 'cancel' || err?.name === 'Cancel') return
    showError(err?.message || '删除失败')
  }
}

function handleDownloadFile(msg: any) {
  activeMessageId.value = null
  downloadFile(msg)
}

async function handleDeleteFile(msg: ChatMessage) {
  activeMessageId.value = null
  if (!msg.fileId) return
  try {
    await showConfirmDialog({ title: '删除文件', message: '确定删除这个文件吗？删除后将无法恢复。' })
    await chatApi.deleteChatFile(msg.fileId)
    showSuccess('已删除')
    messages.value = messages.value.filter(m => m.id !== msg.id)
    saveToCache(conversationId.value, messages.value)
  } catch (err: any) {
    if (err === 'cancel' || err?.name === 'Cancel') return
    showError(err?.message || '删除失败')
  }
}

async function handleDeletePhoto(msg: ChatMessage) {
  if (!msg.photoId) return
  try {
    await showConfirmDialog({ title: '删除图片', message: '确定删除这张图片吗？删除后将无法恢复。' })
    await chatApi.deleteChatPhoto(msg.photoId)
    showSuccess('已删除')
    messages.value = messages.value.filter(m => m.id !== msg.id)
    saveToCache(conversationId.value, messages.value)
  } catch (err: any) {
    if (err === 'cancel' || err?.name === 'Cancel') return
    showError(err?.message || '删除失败')
  }
}

// ==================== 清除聊天 ====================

async function handleClearChat() {
  const convId = conversationId.value
  if (!convId || isNaN(convId)) return

  try {
    await showConfirmDialog({ title: '清除聊天', message: '确定清除所有聊天记录吗？清除后将无法恢复。' })
    await chatApi.clearConversation(convId)
    showSuccess('聊天记录已清除')

    // 清除本地缓存并重新加载（服务端会根据 clearedAt 过滤，返回空或新消息）
    localStorage.removeItem(getCacheKey(convId))
    messages.value = []
    currentPage.value = 0
    hasMore.value = true
    fetchMessages(false)
  } catch {
    // 用户取消
  }
}

// ==================== WebSocket 消息 ====================

function handleNewMessage(data: any) {
  const convId = conversationId.value
  if (!convId || isNaN(convId)) return
  if (data.conversationId === convId && data.senderId !== currentUserId.value) {
    messages.value.push(data)
    scrollToBottom()
    saveToCache(convId, messages.value)
    chatWs.markRead(convId)
  }
}

function handleError(data: any) {
  showError(data.message || '发送失败')
  // 权限被拒绝时重新检查
  if (data.message?.includes('消息') || data.message?.includes('发送')) {
    checkPermission()
  }
}

function handleSentMessage(data: any) {
  if (!data.conversationId) return

  // 首条消息：conversationId 从 NaN 变为服务端真实 ID
  if (isNaN(conversationId.value)) {
    conversationId.value = data.conversationId
    router.replace({
      query: {
        ...route.query,
        conversationId: data.conversationId
      }
    })
    // 更新本地消息的 conversationId
    messages.value.forEach(m => { m.conversationId = data.conversationId })
    saveToCache(data.conversationId, messages.value)
    return
  }

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

onMounted(async () => {
  // 检查发送权限
  checkPermission()

  // 如果没有 conversationId，先从服务端获取/创建
  if (isNaN(conversationId.value) && targetUserId.value) {
    try {
      const res = await chatApi.getOrCreateConversationWith(targetUserId.value)
      if (res?.conversationId) {
        conversationId.value = res.conversationId
        router.replace({
          query: {
            ...route.query,
            conversationId: res.conversationId
          }
        })
      }
    } catch (e) {
      console.error('Failed to get conversation', e)
    }
  }

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
  chatWs.on('error', handleError)

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
  chatWs.off('error', handleError)
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

.permission-hint {
  padding: 8px 16px;
  text-align: center;
  font-size: 12px;
  color: #ef4444;
  background: rgba(239, 68, 68, 0.06);
  flex-shrink: 0;
}

.permission-hint-info {
  color: var(--color-on-surface-variant);
  background: var(--color-surface-container-low);
}

.input-container {
  display: flex;
  gap: 8px;
  padding: 12px;
  background: var(--color-surface-container-lowest);
  border-top: 1px solid var(--color-surface-container-low);
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
  &:active { background: var(--color-surface-container-low); }
  &:disabled { opacity: 0.3; cursor: not-allowed; }
  &.active { color: var(--color-primary); background: rgba(0, 89, 182, 0.08); }
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
  &:active { background: var(--color-surface-container-low); }
  &:disabled { opacity: 0.3; cursor: not-allowed; }
  &.active {
    color: var(--color-primary);
    background: rgba(0, 89, 182, 0.08);
    svg { transform: rotate(45deg); }
  }
}

.clear-btn {
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
  &:active { background: var(--color-surface-container-low); }
}

.message-image {
  cursor: pointer;
  border-radius: 12px;
  overflow: hidden;
  max-width: 200px;
  img {
    width: 100%;
    display: block;
    border-radius: 12px;
  }
}

.message-file {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  padding: 4px 0;
}

.file-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  background: var(--color-surface-container-low);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-on-surface-variant);
  flex-shrink: 0;
}

.file-info {
  min-width: 0;
}

.file-name {
  font-size: 13px;
  font-weight: 500;
  color: var(--color-on-surface);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 160px;
}

.file-size {
  font-size: 11px;
  color: var(--color-on-surface-variant);
  opacity: 0.7;
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

/* Attachment panel */
.attachment-panel {
  background: rgba(255, 255, 255, 0.6);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-radius: 1.5rem 1.5rem 0 0;
  border: 1px solid rgba(255, 255, 255, 0.8);
  box-shadow: 0 -4px 24px rgba(0, 0, 0, 0.06);
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
  &:active { background: rgba(0, 0, 0, 0.05); }
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
    background: rgba(0, 89, 182, 0.08);
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
    background: linear-gradient(135deg, rgba(0, 89, 182, 0.12), rgba(0, 89, 182, 0.06));
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

/* 消息操作面板 */
.delete-btn-wrapper {
  display: flex;
  align-items: center;
  flex-shrink: 0;
}

.delete-btn {
  width: 24px;
  height: 24px;
  border: none;
  border-radius: 50%;
  background: rgba(239, 68, 68, 0.1);
  color: #ef4444;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: background 0.2s, transform 0.15s;
  flex-shrink: 0;

  &:active {
    background: rgba(239, 68, 68, 0.2);
    transform: scale(0.9);
  }

  &.delete-btn-image {
    position: absolute;
    top: -8px;
    left: -4px;
    width: 22px;
    height: 22px;
    background: rgba(0, 0, 0, 0.5);
    color: white;
    opacity: 0;
    transition: opacity 0.2s;
  }
}

.message-item {
  position: relative;

  &:hover .delete-btn-image {
    opacity: 1;
  }
}

.message-bubble {
  position: relative;

  &.bubble-clickable {
    cursor: pointer;
  }
}

.message-actions-panel {
  position: absolute;
  bottom: calc(100% + 8px);
  left: 0;
  display: flex;
  gap: 6px;
  background: var(--color-surface-container-lowest);
  border-radius: 8px;
  padding: 6px 8px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
  z-index: 100;
  white-space: nowrap;

  &.panel-self {
    left: auto;
    right: 0;
  }
}

.action-btn {
  padding: 6px 12px;
  border: none;
  border-radius: 6px;
  background: var(--color-surface-container-low);
  color: var(--color-on-surface);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.2s;

  &:active {
    background: var(--color-surface-container-high);
  }

  &.action-btn-delete {
    background: rgba(239, 68, 68, 0.1);
    color: #ef4444;

    &:active {
      background: rgba(239, 68, 68, 0.2);
    }
  }
}

/* Panel slide transition */
.panel-slide-enter-active {
  transition: transform 0.25s cubic-bezier(0.4, 0, 0.2, 1), opacity 0.2s;
}
.panel-slide-leave-active {
  transition: transform 0.2s cubic-bezier(0.4, 0, 0.2, 1), opacity 0.15s;
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

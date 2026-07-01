<template>
  <div class="chat-room-container">
    <!-- Header -->
    <ChatRoomHeader :nickname="nickname" @back="router.back()" @clear="handleClearChat" />

    <!-- Messages -->
    <ChatMessageList
      ref="messageListRef"
      :messages="messages"
      :current-user-id="currentUserId"
      :partner-avatar="partnerAvatar"
      :partner-nickname="nickname"
      :my-avatar="myAvatar"
      :my-nickname="authStore.userInfo?.nickname || ''"
      :loading-more="loadingMore"
      :has-more="hasMore"
      @load-more="onLoadMore"
      @close-panels="closePanels"
      @message-action="onMessageAction"
    />

    <!-- Input -->
    <ChatMessageInput
      ref="messageInputRef"
      :send-permission="sendPermission"
      :remaining="remaining"
      :uploading="uploading"
      :close-panels-signal="closePanelsSignal"
      @send-text="onSendText"
      @send-image="onSendImage"
      @send-file="onSendFile"
      @input-focus="closePanels"
      @clear-photo-input="messageInputRef?.resetPhotoInput()"
      @clear-file-input="messageInputRef?.resetFileInput()"
    />
  </div>
</template>

<script setup lang="ts">
// ChatRoom 父组件：保留所有 state + WebSocket + API + lifecycle。
// 三个子组件负责纯 UI（Header / MessageList / MessageInput），业务事件全部在本组件处理。
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { chatApi, uploadApi } from '@/api'
import { showError, showSuccess } from '@/utils/toast'
import { getCachedUser } from '@/utils/userCache'
import { showImagePreview, showConfirmDialog } from 'vant'
import chatWs from '@/websocket/chat'
import type { ChatMessage } from '@/types/chat'
import ChatRoomHeader from './components/ChatRoom/ChatRoomHeader.vue'
import ChatMessageList from './components/ChatRoom/ChatMessageList.vue'
import ChatMessageInput from './components/ChatRoom/ChatMessageInput.vue'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const messageListRef = ref<InstanceType<typeof ChatMessageList> | null>(null)
const messageInputRef = ref<InstanceType<typeof ChatMessageInput> | null>(null)

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
const currentPage = ref(0)
const hasMore = ref(true)
const loadingMore = ref(false)
const isLoading = ref(false)
const uploading = ref(false)

// 发送权限
const sendPermission = ref<'ok' | 'max_reached' | 'not_allowed'>('ok')
const remaining = ref(-1) // -1 = 无限制

/** 关闭所有面板的广播信号（递增），由子组件监听并各自收起 emoji/attach/操作面板 */
const closePanelsSignal = ref(0)
function closePanels() {
  closePanelsSignal.value++
  messageListRef.value?.clearActiveMessage()
}

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

  // 追加更早消息前先快照当前 scrollHeight，用于追加后补偿滚动位置。
  // 必须在 await 之前读取，否则 DOM 已渲染新消息导致高度变化。
  let prevScrollHeight = 0
  if (append) {
    const el = document.querySelector('.messages-container') as HTMLElement | null
    prevScrollHeight = el?.scrollHeight ?? 0
  }

  try {
    const res = await chatApi.getMessages(convId, { page: currentPage.value, size: PAGE_SIZE })
    const newItems = (res?.items || []).reverse()

    if (append) {
      messages.value = [...newItems, ...messages.value]
      messageListRef.value?.restoreScrollPosition(prevScrollHeight)
    } else {
      messages.value = newItems
      messageListRef.value?.scrollToBottom()
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

/**
 * 子组件（消息列表）通知：滚到顶部，请求加载更早的消息。
 */
function onLoadMore() {
  currentPage.value++
  fetchMessages(true)
}

// ==================== 发送消息 ====================

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

function onSendText(rawText: string) {
  const content = rawText.trim()
  if (!content || sendPermission.value !== 'ok') return
  const senderId = getRequiredCurrentUserId()
  if (senderId === null) return

  const sent = chatWs.sendMessage(targetUserId.value, content)
  if (!sent) {
    showError('连接未就绪，消息未发送')
    return
  }

  const localMsg: ChatMessage = {
    id: Date.now(),
    conversationId: conversationId.value,
    senderId,
    receiverId: targetUserId.value,
    content,
    messageType: 'text',
    isRead: false,
    createdAt: new Date().toISOString(),
    deliveryStatus: 'pending'
  }
  messages.value.push(localMsg)
  messageInputRef.value?.clearInput()
  messageListRef.value?.scrollToBottom()

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

async function onSendImage(file: File) {
  if (uploading.value) return
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
    const sent = chatWs.sendMessage(targetUserId.value, content, 'image', res.photoId)
    if (!sent) {
      showError('连接未就绪，图片未发送')
      return
    }

    const localMsg: ChatMessage = {
      id: Date.now(),
      conversationId: conversationId.value,
      senderId,
      receiverId: targetUserId.value,
      content,
      messageType: 'image',
      photoId: res.photoId,
      isRead: false,
      createdAt: new Date().toISOString(),
      deliveryStatus: 'pending'
    }
    messages.value.push(localMsg)
    saveToCache(conversationId.value, messages.value)
    messageListRef.value?.scrollToBottom()
  } catch (err: any) {
    showError(err?.message || '图片发送失败')
  } finally {
    uploading.value = false
    messageInputRef.value?.resetPhotoInput()
  }
}

async function onSendFile(file: File) {
  if (uploading.value) return
  const senderId = getRequiredCurrentUserId()
  if (senderId === null) return

  if (file.size > 50 * 1024 * 1024) {
    showError('文件过大，无法发送！请选择小于 50MB 的文件')
    messageInputRef.value?.resetFileInput()
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
    const sent = chatWs.sendMessage(targetUserId.value, content, 'file', undefined, res.fileId)
    if (!sent) {
      showError('连接未就绪，文件未发送')
      return
    }

    const localMsg: ChatMessage = {
      id: Date.now(),
      conversationId: conversationId.value,
      senderId,
      receiverId: targetUserId.value,
      content,
      messageType: 'file',
      fileId: res.fileId,
      isRead: false,
      createdAt: new Date().toISOString(),
      deliveryStatus: 'pending'
    }
    messages.value.push(localMsg)
    saveToCache(conversationId.value, messages.value)
    messageListRef.value?.scrollToBottom()
  } catch (err: any) {
    showError(err?.message || '文件发送失败')
  } finally {
    uploading.value = false
    messageInputRef.value?.resetFileInput()
  }
}

/**
 * 将附件消息改写为“已删除”占位态，而不是直接从消息流中移除。
 */
function markAttachmentDeleted(messageId: number) {
  const target = messages.value.find(m => m.id === messageId)
  if (!target) return
  target.content = '__deleted_attachment__'
  target.photoId = undefined
  target.fileId = undefined
  saveToCache(conversationId.value, messages.value)
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

// ==================== 消息操作 ====================

/**
 * 子组件（消息列表）转发的消息操作事件，统一在本组件分发到对应的业务函数。
 */
async function onMessageAction(payload: { action: string; msg: ChatMessage }) {
  const { action, msg } = payload
  // 任何面板操作都先收起活动操作面板
  closePanels()
  switch (action) {
    case 'preview-image':
      previewImage(msg)
      break
    case 'copy':
      handleCopyContent(msg)
      break
    case 'delete-message':
      await handleDeleteMessage(msg)
      break
    case 'download-file':
      downloadFile(msg)
      break
    case 'delete-file':
      await handleDeleteFile(msg)
      break
    case 'delete-photo':
      await handleDeletePhoto(msg)
      break
  }
}

function handleCopyContent(msg: ChatMessage) {
  navigator.clipboard.writeText(msg.content).then(() => {
    showSuccess('已复制')
  }).catch(() => {
    showError('复制失败')
  })
}

async function handleDeleteMessage(msg: ChatMessage) {
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

async function handleDeleteFile(msg: ChatMessage) {
  if (!msg.fileId) return
  try {
    await showConfirmDialog({ title: '删除文件', message: '确定删除这个文件吗？删除后将无法恢复。' })
    await chatApi.deleteChatFile(msg.fileId)
    showSuccess('已删除')
    markAttachmentDeleted(msg.id)
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
    markAttachmentDeleted(msg.id)
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
    messageListRef.value?.scrollToBottom()
    saveToCache(convId, messages.value)
    chatWs.markRead(convId)
  }
}

function handleError(data: any) {
  showError(data.message || '发送失败')
  markLatestPendingMessageFailed()
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
      data.deliveryStatus = 'sent'
      messages.value[lastIndex] = data
      saveToCache(conversationId.value, messages.value)
    }
  }
}

/**
 * 将最近一条待确认的本地消息标记为失败，并回滚一次非互关额度的乐观扣减。
 */
function markLatestPendingMessageFailed() {
  for (let i = messages.value.length - 1; i >= 0; i--) {
    const msg = messages.value[i]
    if (msg.deliveryStatus === 'pending') {
      msg.deliveryStatus = 'failed'
      saveToCache(conversationId.value, messages.value)
      if (sendPermission.value === 'max_reached') {
        sendPermission.value = 'ok'
      }
      if (remaining.value >= 0) {
        remaining.value++
      }
      break
    }
  }
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
      messageListRef.value?.scrollToBottom()
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
</style>

<template>
  <div class="chat-list-container">
    <div class="bg-glow bg-glow-1"></div>

    <!-- Header -->
    <div class="page-header animate-fade-up">
      <h1 class="page-title"><em>消息</em></h1>
      <button class="contacts-btn" @click="router.push('/contacts')">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M22 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>
      </button>
    </div>

    <!-- Conversation List -->
    <div class="conversation-list">
      <div 
        v-for="(conv, index) in conversations" 
        :key="conv.conversationId" 
        class="conversation-item animate-fade-up"
        :style="{ animationDelay: (index % 10) * 0.05 + 's' }"
        @click="openChat(conv)"
      >
        <div class="conv-avatar">
          <img v-if="conv.avatar" :src="conv.avatar" alt="" />
          <span v-else>{{ conv.nickname?.charAt(0) || '?' }}</span>
          <span v-if="conv.unreadCount > 0" class="unread-badge">{{ conv.unreadCount > 99 ? '99+' : conv.unreadCount }}</span>
        </div>
        <div class="conv-content">
          <div class="conv-header">
            <span class="conv-name">{{ conv.nickname }}</span>
            <span class="conv-time">{{ formatTime(conv.lastMessageAt) }}</span>
          </div>
          <div class="conv-preview">{{ conv.lastMessage || '暂无消息' }}</div>
        </div>
      </div>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="loading-more">
      <div class="spinner"></div>
    </div>

    <!-- Empty -->
    <div v-if="!loading && conversations.length === 0" class="empty-state">
      <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" class="empty-icon"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>
      <div class="empty-text">暂无消息</div>
      <div class="empty-hint">去通讯录找好友聊天吧</div>
    </div>

    <div class="nav-spacer"></div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { chatApi } from '@/api'
import { cacheUsersFromList } from '@/utils/userCache'
import chatWs from '@/websocket/chat'

const router = useRouter()

const conversations = ref<any[]>([])
const loading = ref(false)

async function fetchConversations() {
  loading.value = true
  try {
    const res = await chatApi.getConversations()
    conversations.value = res || []
    // 缓存用户信息（头像、昵称）
    cacheUsersFromList(res || [])
  } catch { /* ignore */ }
  finally { loading.value = false }
}

function openChat(conv: any) {
  localStorage.setItem('lastChatPartner', JSON.stringify({
    userId: conv.userId,
    conversationId: conv.conversationId,
    nickname: conv.nickname,
    avatar: conv.avatar
  }))
  router.push({
    path: '/chat-room',
    query: {
      userId: conv.userId,
      conversationId: conv.conversationId,
      nickname: conv.nickname,
      avatar: conv.avatar || ''
    }
  })
}

function formatTime(dateStr: string): string {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)
  const days = Math.floor(diff / 86400000)

  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  if (hours < 24) return `${hours}小时前`
  if (days < 7) return `${days}天前`
  return date.toLocaleDateString()
}

function handleNewMessage(data: any) {
  // 更新对话列表
  const convIndex = conversations.value.findIndex(c => c.conversationId === data.conversationId)
  if (convIndex > -1) {
    conversations.value[convIndex].lastMessage = data.content
    conversations.value[convIndex].lastMessageAt = data.createdAt
    if (data.senderId !== conversations.value[convIndex].userId) {
      conversations.value[convIndex].unreadCount = (conversations.value[convIndex].unreadCount || 0) + 1
    }
    // 移到顶部
    const conv = conversations.value.splice(convIndex, 1)[0]
    conversations.value.unshift(conv)
  } else {
    // 新对话，重新加载列表
    fetchConversations()
  }
}

onMounted(() => {
  fetchConversations()

  // 监听新消息
  chatWs.on('message', handleNewMessage)
  chatWs.on('sent', handleNewMessage)

  // 连接 WebSocket
  if (!chatWs.connected) {
    chatWs.connect()
  }
})

onUnmounted(() => {
  chatWs.off('message', handleNewMessage)
  chatWs.off('sent', handleNewMessage)
})
</script>

<style lang="scss" scoped>
.chat-list-container {
  min-height: 100vh;
  background: var(--color-surface);
  padding: 40px 12px 100px;
  position: relative;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
}

.bg-glow {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  pointer-events: none;
}

.bg-glow-1 {
  top: -60px;
  right: -40px;
  width: 240px;
  height: 240px;
  background: var(--color-primary-container);
  opacity: 0.1;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  z-index: 1;
  position: relative;
}

.page-title {
  font-family: var(--font-serif);
  font-style: italic;
  font-size: 32px;
  font-weight: 400;
  color: var(--color-on-surface);
  em { font-style: italic; color: var(--color-primary); }
}

.contacts-btn {
  width: 40px;
  height: 40px;
  border: none;
  border-radius: 50%;
  background: var(--color-surface-container-lowest);
  color: var(--color-on-surface-variant);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  &:active { transform: scale(0.95); }
}

.conversation-list {
  z-index: 1;
  position: relative;
}

.conversation-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: var(--color-surface-container-lowest);
  border-radius: 1.25rem;
  border: 1px solid rgba(255, 255, 255, 0.8);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  margin-bottom: 8px;
  cursor: pointer;
  transition: transform 0.2s;
  &:active { transform: scale(0.98); }
}

.conv-avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--color-primary-container), var(--color-primary));
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-family: var(--font-serif);
  font-style: italic;
  font-size: 18px;
  font-weight: 400;
  flex-shrink: 0;
  overflow: hidden;
  position: relative;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}

.unread-badge {
  position: absolute;
  top: -2px;
  right: -2px;
  min-width: 18px;
  height: 18px;
  padding: 0 4px;
  border-radius: 9px;
  background: #ef4444;
  color: white;
  font-size: 10px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  line-height: 1;
  border: 2px solid var(--color-surface);
}

.conv-content {
  flex: 1;
  min-width: 0;
}

.conv-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.conv-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-on-surface);
}

.conv-time {
  font-size: 11px;
  color: var(--color-on-surface-variant);
}

.conv-preview {
  font-size: 13px;
  color: var(--color-on-surface-variant);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.loading-more {
  display: flex;
  justify-content: center;
  padding: 20px 0;
  z-index: 1;
  position: relative;
}

.spinner {
  width: 24px;
  height: 24px;
  border: 3px solid var(--color-surface-container-low);
  border-top-color: var(--color-primary);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin { to { transform: rotate(360deg); } }

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 60px 0;
  z-index: 1;
  position: relative;
}

.empty-icon { color: var(--color-on-surface-variant); opacity: 0.3; margin-bottom: 16px; }
.empty-text { font-size: 15px; font-weight: 600; color: var(--color-on-surface-variant); margin-bottom: 6px; }
.empty-hint { font-size: 12px; color: var(--color-on-surface-variant); opacity: 0.6; }

.nav-spacer {
  height: 80px;
}

@media (min-width: 1024px) {
  .chat-list-container {
    max-width: 60%;
    margin: 0 auto;
  }
}
</style>

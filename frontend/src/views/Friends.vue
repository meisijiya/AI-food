<template>
  <div class="friends-container">
    <div class="bg-glow bg-glow-1"></div>

    <!-- Header -->
    <div class="page-header animate-fade-up">
      <button class="back-btn" @click="router.back()">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m15 18-6-6 6-6"/></svg>
      </button>
      <h1 class="page-title"><em>好友</em></h1>
      <button class="search-btn" @click="router.push('/user-search')">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="11" cy="11" r="8"/><path d="m21 21-4.3-4.3"/></svg>
      </button>
    </div>

    <!-- Sub Tabs -->
    <div class="sub-tabs animate-fade-up delay-100">
      <button
        class="sub-tab"
        :class="{ active: activeTab === 'chat' }"
        @click="switchTab('chat')"
      >
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>
        聊天列表
        <span v-if="totalUnread > 0" class="tab-badge">{{ totalUnread > 99 ? '99+' : totalUnread }}</span>
      </button>
      <button
        class="sub-tab"
        :class="{ active: activeTab === 'direct' }"
        @click="switchTab('direct')"
      >
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M22 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>
        私聊
      </button>
      <button
        class="sub-tab"
        :class="{ active: activeTab === 'friends' }"
        @click="switchTab('friends')"
      >
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M22 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>
        好友列表
      </button>
    </div>

    <!-- Chat List Tab -->
    <div v-if="activeTab === 'chat'" class="tab-content">
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
            <span v-if="conv.unreadCount > 0" class="unread-dot"></span>
          </div>
          <div class="conv-content">
            <div class="conv-header">
              <span class="conv-name">{{ conv.nickname }}</span>
              <span class="conv-time">{{ formatTime(conv.lastMessageAt) }}</span>
            </div>
            <div class="conv-preview">{{ conv.lastMessage || '暂无消息' }}</div>
          </div>
          <span v-if="conv.unreadCount > 0" class="unread-badge">{{ conv.unreadCount > 99 ? '99+' : conv.unreadCount }}</span>
        </div>
      </div>
      <div v-if="!loadingChat && conversations.length === 0" class="empty-state">
        <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" class="empty-icon"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>
        <div class="empty-text">暂无消息</div>
        <div class="empty-hint">去好友列表找人聊天吧</div>
      </div>
    </div>

    <!-- Direct Chat Tab -->
    <div v-if="activeTab === 'direct'" class="tab-content">
      <div v-if="lastPartner" class="direct-chat-card animate-fade-up" @click="openDirectChat">
        <div class="direct-avatar">
          <img v-if="lastPartner.avatar" :src="lastPartner.avatar" alt="" />
          <span v-else>{{ lastPartner.nickname?.charAt(0) || '?' }}</span>
        </div>
        <div class="direct-info">
          <div class="direct-name">{{ lastPartner.nickname }}</div>
          <div class="direct-hint">上次聊天对象，点击继续聊天</div>
        </div>
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="var(--color-primary)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m9 18 6-6-6-6"/></svg>
      </div>
      <div v-else class="empty-state">
        <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" class="empty-icon"><path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M22 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>
        <div class="empty-text">还没有聊过天</div>
        <div class="empty-hint">去聊天列表开始吧</div>
      </div>
    </div>

    <!-- Friends List Tab -->
    <div v-if="activeTab === 'friends'" class="tab-content">
      <div class="friend-list">
        <div
          v-for="(friend, index) in friendList"
          :key="friend.userId"
          class="friend-item animate-fade-up"
          :style="{ animationDelay: (index % 10) * 0.05 + 's' }"
        >
          <div class="friend-avatar">
            <img v-if="friend.avatar" :src="friend.avatar" alt="" />
            <span v-else>{{ friend.nickname?.charAt(0) || '?' }}</span>
          </div>
          <div class="friend-info">
            <div class="friend-name">{{ friend.nickname }}</div>
          </div>
          <button class="friend-chat-btn" @click="openFriendChat(friend)">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>
          </button>
          <button class="friend-unfollow-btn" @click="handleUnfollow(friend)">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><line x1="23" y1="11" x2="17" y2="11"/></svg>
          </button>
        </div>
      </div>
      <div v-if="!loadingFriends && friendList.length === 0" class="empty-state">
        <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" class="empty-icon"><path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M22 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>
        <div class="empty-text">暂无好友</div>
        <div class="empty-hint">互相关注的好友会出现在这里</div>
      </div>
    </div>

    <!-- Loading -->
    <div v-if="loadingChat || loadingFriends" class="loading-more">
      <div class="spinner"></div>
    </div>

    <div class="nav-spacer"></div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { chatApi, followApi } from '@/api'
import chatWs from '@/websocket/chat'

const router = useRouter()

const activeTab = ref('chat')
const conversations = ref<any[]>([])
const friendList = ref<any[]>([])
const totalUnread = ref(0)
const loadingChat = ref(false)
const loadingFriends = ref(false)
const lastPartner = ref<any>(null)

function switchTab(tab: string) {
  activeTab.value = tab
  if (tab === 'chat') {
    fetchConversations()
  } else if (tab === 'direct') {
    loadLastPartner()
  } else if (tab === 'friends') {
    fetchFriends()
  }
}

// ==================== Chat List ====================

async function fetchConversations() {
  loadingChat.value = true
  try {
    const res = await chatApi.getConversations()
    conversations.value = res || []
    totalUnread.value = conversations.value.reduce((sum, c) => sum + (c.unreadCount || 0), 0)
  } catch { /* ignore */ }
  finally { loadingChat.value = false }
}

function openChat(conv: any) {
  // Save last chat partner to localStorage
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
      nickname: conv.nickname
    }
  })
}

function handleNewMessage(data: any) {
  const convIndex = conversations.value.findIndex(c => c.conversationId === data.conversationId)
  if (convIndex > -1) {
    conversations.value[convIndex].lastMessage = data.content
    conversations.value[convIndex].lastMessageAt = data.createdAt
    if (data.senderId !== conversations.value[convIndex].userId) {
      conversations.value[convIndex].unreadCount = (conversations.value[convIndex].unreadCount || 0) + 1
    }
    const conv = conversations.value.splice(convIndex, 1)[0]
    conversations.value.unshift(conv)
    totalUnread.value = conversations.value.reduce((sum, c) => sum + (c.unreadCount || 0), 0)
  } else {
    fetchConversations()
  }
}

// ==================== Direct Chat ====================

function loadLastPartner() {
  const saved = localStorage.getItem('lastChatPartner')
  if (saved) {
    try {
      lastPartner.value = JSON.parse(saved)
    } catch {
      lastPartner.value = null
    }
  } else {
    lastPartner.value = null
  }
}

function openDirectChat() {
  if (!lastPartner.value) return
  router.push({
    path: '/chat-room',
    query: {
      userId: lastPartner.value.userId,
      conversationId: lastPartner.value.conversationId,
      nickname: lastPartner.value.nickname
    }
  })
}

// ==================== Friends List ====================

async function fetchFriends() {
  loadingFriends.value = true
  try {
    const res = await chatApi.getContacts()
    friendList.value = res || []
  } catch {
    try {
      const res = await followApi.getMutualFriends({ page: 0, size: 100 })
      friendList.value = res?.items || []
    } catch { /* ignore */ }
  }
  finally { loadingFriends.value = false }
}

async function openFriendChat(friend: any) {
  try {
    const conversations = await chatApi.getConversations()
    const existing = conversations?.find((c: any) => c.userId === friend.userId)

    const partner = {
      userId: friend.userId,
      conversationId: existing?.conversationId,
      nickname: friend.nickname,
      avatar: friend.avatar
    }
    localStorage.setItem('lastChatPartner', JSON.stringify(partner))

    if (existing) {
      router.push({
        path: '/chat-room',
        query: { userId: friend.userId, conversationId: existing.conversationId, nickname: friend.nickname }
      })
    } else {
      router.push({
        path: '/chat-room',
        query: { userId: friend.userId, nickname: friend.nickname }
      })
    }
  } catch {
    router.push({
      path: '/chat-room',
      query: { userId: friend.userId, nickname: friend.nickname }
    })
  }
}

async function handleUnfollow(friend: any) {
  try {
    await followApi.toggleFollow(friend.userId)
    friendList.value = friendList.value.filter(f => f.userId !== friend.userId)
  } catch { /* ignore */ }
}

// ==================== Utils ====================

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

onMounted(() => {
  fetchConversations()

  chatWs.on('message', handleNewMessage)
  chatWs.on('sent', handleNewMessage)

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
.friends-container {
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
  background: var(--color-secondary-fixed);
  opacity: 0.1;
}

/* Header */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  z-index: 1;
  position: relative;
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

.page-title {
  font-family: var(--font-serif);
  font-style: italic;
  font-size: 24px;
  font-weight: 400;
  color: var(--color-on-surface);
  em { font-style: italic; color: var(--color-primary); }
}

.search-btn {
  width: 36px;
  height: 36px;
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

/* Sub Tabs */
.sub-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
  z-index: 1;
  position: relative;
}

.sub-tab {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  padding: 8px 4px;
  border: 1.5px solid var(--color-surface-container-low);
  border-radius: 100px;
  background: none;
  color: var(--color-on-surface-variant);
  font-family: var(--font-sans);
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  position: relative;
  white-space: nowrap;

  svg { opacity: 0.7; }

  &.active {
    background: linear-gradient(135deg, var(--color-primary-container), var(--color-primary));
    border-color: transparent;
    color: white;
    svg { opacity: 1; stroke: white; }
  }

  &:not(.active):active {
    background: var(--color-surface-container-low);
  }
}

.tab-badge {
  position: absolute;
  top: -4px;
  right: -4px;
  min-width: 16px;
  height: 16px;
  padding: 0 4px;
  border-radius: 8px;
  background: #ef4444;
  color: white;
  font-size: 9px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  line-height: 1;
}

/* Tab Content */
.tab-content {
  z-index: 1;
  position: relative;
}

/* Conversation List */
.conversation-list {}

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

.unread-dot {
  position: absolute;
  top: 0;
  right: 0;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #ef4444;
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

.unread-badge {
  min-width: 20px;
  height: 20px;
  padding: 0 6px;
  border-radius: 10px;
  background: #ef4444;
  color: white;
  font-size: 11px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  line-height: 1;
  flex-shrink: 0;
}

/* Direct Chat */
.direct-chat-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 20px 20px;
  background: var(--color-surface-container-lowest);
  border-radius: 1.5rem;
  border: 1px solid rgba(255, 255, 255, 0.8);
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  cursor: pointer;
  transition: transform 0.2s;
  &:active { transform: scale(0.98); }
}

.direct-avatar {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--color-primary-container), var(--color-primary));
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-family: var(--font-serif);
  font-style: italic;
  font-size: 22px;
  font-weight: 400;
  flex-shrink: 0;
  overflow: hidden;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}

.direct-info {
  flex: 1;
  min-width: 0;
}

.direct-name {
  font-size: 17px;
  font-weight: 600;
  color: var(--color-on-surface);
  margin-bottom: 4px;
}

.direct-hint {
  font-size: 13px;
  color: var(--color-on-surface-variant);
}

/* Friends List */
.friend-list {}

.friend-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: var(--color-surface-container-lowest);
  border-radius: 1.25rem;
  border: 1px solid rgba(255, 255, 255, 0.8);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  margin-bottom: 8px;
}

.friend-avatar {
  width: 44px;
  height: 44px;
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

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}

.friend-info {
  flex: 1;
  min-width: 0;
}

.friend-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-on-surface);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.friend-chat-btn {
  width: 36px;
  height: 36px;
  border: none;
  border-radius: 50%;
  background: var(--color-surface-container-low);
  color: var(--color-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  flex-shrink: 0;
  &:active { transform: scale(0.95); }
}

.friend-unfollow-btn {
  width: 36px;
  height: 36px;
  border: 1.5px solid var(--color-surface-container-low);
  border-radius: 50%;
  background: none;
  color: var(--color-on-surface-variant);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  flex-shrink: 0;
  &:active { transform: scale(0.95); background: var(--color-surface-container-low); }
}

/* Loading / Empty */
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

.nav-spacer { height: 80px; }
</style>

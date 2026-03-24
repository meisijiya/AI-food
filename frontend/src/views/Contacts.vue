<template>
  <div class="contacts-container">
    <div class="bg-glow bg-glow-1"></div>

    <!-- Header -->
    <div class="page-header animate-fade-up">
      <button class="back-btn" @click="router.back()">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m15 18-6-6 6-6"/></svg>
      </button>
      <h1 class="page-title"><em>通讯录</em></h1>
      <div class="header-placeholder"></div>
    </div>

    <!-- Contact List -->
    <div class="contact-list">
      <div 
        v-for="(contact, index) in contacts" 
        :key="contact.userId" 
        class="contact-item animate-fade-up"
        :style="{ animationDelay: (index % 10) * 0.05 + 's' }"
        @click="openChat(contact)"
      >
        <div class="contact-avatar">
          <img v-if="contact.avatar" :src="contact.avatar" alt="" />
          <span v-else>{{ contact.nickname?.charAt(0) || '?' }}</span>
          <span v-if="contact.isOnline" class="online-dot"></span>
        </div>
        <div class="contact-info">
          <div class="contact-name">{{ contact.nickname }}</div>
        </div>
        <button class="chat-btn" @click.stop="openChat(contact)">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>
        </button>
      </div>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="loading-more">
      <div class="spinner"></div>
    </div>

    <!-- Empty -->
    <div v-if="!loading && contacts.length === 0" class="empty-state">
      <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" class="empty-icon"><path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M22 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>
      <div class="empty-text">暂无好友</div>
      <div class="empty-hint">互相关注的好友会出现在这里</div>
    </div>

    <div class="nav-spacer"></div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { chatApi, followApi } from '@/api'

const router = useRouter()

const contacts = ref<any[]>([])
const loading = ref(false)

async function fetchContacts() {
  loading.value = true
  try {
    // 先尝试从聊天通讯录获取
    const res = await chatApi.getContacts()
    contacts.value = res || []
  } catch {
    // 如果失败，从互关好友获取
    try {
      const res = await followApi.getMutualFriends({ page: 0, size: 100 })
      contacts.value = res?.items || []
    } catch { /* ignore */ }
  }
  finally { loading.value = false }
}

async function openChat(contact: any) {
  // 获取或创建对话
  try {
    const conversations = await chatApi.getConversations()
    const existing = conversations?.find((c: any) => c.userId === contact.userId)
    
    if (existing) {
      router.push({
        path: '/chat-room',
        query: {
          userId: contact.userId,
          conversationId: existing.conversationId,
          nickname: contact.nickname
        }
      })
    } else {
      // 新对话，conversationId 会在发送消息时创建
      router.push({
        path: '/chat-room',
        query: {
          userId: contact.userId,
          nickname: contact.nickname
        }
      })
    }
  } catch {
    router.push({
      path: '/chat-room',
      query: {
        userId: contact.userId,
        nickname: contact.nickname
      }
    })
  }
}

onMounted(() => {
  fetchContacts()
})
</script>

<style lang="scss" scoped>
.contacts-container {
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

.header-placeholder {
  width: 36px;
}

.contact-list {
  z-index: 1;
  position: relative;
}

.contact-item {
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

.contact-avatar {
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

.online-dot {
  position: absolute;
  bottom: 2px;
  right: 2px;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #22c55e;
  border: 2px solid var(--color-surface);
}

.contact-info {
  flex: 1;
  min-width: 0;
}

.contact-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-on-surface);
}

.chat-btn {
  width: 40px;
  height: 40px;
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

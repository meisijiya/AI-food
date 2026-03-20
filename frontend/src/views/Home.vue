<template>
  <div class="home-container">
    <div class="header">
      <h1 class="title">AI美食推荐</h1>
      <p class="subtitle">告诉我你的需求，为你推荐最合适的美食</p>
    </div>
    
    <div class="content">
      <div class="feature-list">
        <div class="feature-item">
          <van-icon name="chat" size="24" color="#1989fa" />
          <span>智能对话</span>
        </div>
        <div class="feature-item">
          <van-icon name="location" size="24" color="#1989fa" />
          <span>精准定位</span>
        </div>
        <div class="feature-item">
          <van-icon name="like" size="24" color="#1989fa" />
          <span>个性化推荐</span>
        </div>
      </div>
      
      <van-button 
        type="primary" 
        size="large" 
        block 
        round
        @click="startChat"
        :loading="loading"
      >
        开始美食之旅
      </van-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { conversationApi } from '@/api'
import { useChatStore } from '@/stores/chat'
import { showToast } from 'vant'

const router = useRouter()
const chatStore = useChatStore()
const loading = ref(false)

const startChat = async () => {
  loading.value = true
  
  try {
    const response = await conversationApi.start()
    chatStore.setSessionId(response.sessionId)
    chatStore.clearChat()
    router.push('/chat')
  } catch (error) {
    showToast('启动对话失败，请重试')
    console.error('Failed to start conversation:', error)
  } finally {
    loading.value = false
  }
}
</script>

<style lang="scss" scoped>
.home-container {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
}

.header {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  text-align: center;
  color: white;
}

.title {
  font-size: 32px;
  font-weight: bold;
  margin-bottom: 10px;
}

.subtitle {
  font-size: 16px;
  opacity: 0.9;
}

.content {
  padding: 30px 0;
}

.feature-list {
  display: flex;
  justify-content: space-around;
  margin-bottom: 40px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 12px;
  padding: 20px;
}

.feature-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  color: white;
  font-size: 14px;
}

:deep(.van-button--primary) {
  background: linear-gradient(90deg, #1989fa 0%, #07c160 100%);
  border: none;
  font-size: 16px;
  font-weight: bold;
}
</style>
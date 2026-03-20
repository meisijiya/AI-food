<template>
  <div class="chat-container">
    <van-nav-bar
      title="AI美食助手"
      left-arrow
      @click-left="onClickLeft"
      fixed
      placeholder
    >
      <template #right>
        <van-icon name="ellipsis" size="20" @click="showActions = true" />
      </template>
    </van-nav-bar>

    <div class="progress-bar">
      <van-progress
        :percentage="progressPercentage"
        :show-pivot="false"
        color="#1989fa"
      />
      <span class="progress-text">{{ chatStore.progress.current }}/{{ chatStore.progress.total }}</span>
    </div>

    <div class="message-list" ref="messageListRef">
      <div class="welcome-message" v-if="chatStore.messages.length === 0">
        <p>正在连接AI助手...</p>
      </div>
      
      <div
        v-for="message in chatStore.messages"
        :key="message.id"
        :class="['message-item', message.type === 'user' ? 'user' : 'ai']"
      >
        <div class="avatar">
          <van-icon 
            v-if="message.type === 'user'" 
            name="user" 
            size="20" 
            color="#fff" 
          />
          <van-icon 
            v-else 
            name="smile" 
            size="20" 
            color="#fff" 
          />
        </div>
        <div class="message-content">
          <div class="message-text">{{ message.content }}</div>
          <div class="message-time">{{ formatTime(message.timestamp) }}</div>
        </div>
      </div>
      
      <div v-if="chatStore.isLoading" class="loading-indicator">
        <van-loading size="20px">AI正在思考...</van-loading>
      </div>
    </div>

    <div class="input-area">
      <van-field
        v-model="inputValue"
        placeholder="请输入你的回答..."
        :disabled="!chatStore.isConnected || chatStore.isLoading"
        @keyup.enter="sendMessage"
      />
      <van-button
        type="primary"
        size="small"
        :disabled="!inputValue.trim() || !chatStore.isConnected"
        @click="sendMessage"
      >
        发送
      </van-button>
    </div>

    <van-action-sheet
      v-model:show="showActions"
      :actions="actions"
      cancel-text="取消"
      @select="onActionSelect"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { useChatStore } from '@/stores/chat'
import { WebSocketClient, type WebSocketMessage } from '@/websocket'
import { showToast, showConfirmDialog } from 'vant'

const router = useRouter()
const chatStore = useChatStore()
const messageListRef = ref<HTMLElement | null>(null)
const inputValue = ref('')
const showActions = ref(false)

let wsClient: WebSocketClient | null = null

const progressPercentage = computed(() => {
  if (chatStore.progress.total === 0) return 0
  return Math.round((chatStore.progress.current / chatStore.progress.total) * 100)
})

const actions = [
  { name: '查看历史', value: 'history' },
  { name: '结束对话', value: 'complete' },
  { name: '重新开始', value: 'restart' }
]

const formatTime = (timestamp: number) => {
  const date = new Date(timestamp)
  return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

const scrollToBottom = () => {
  nextTick(() => {
    if (messageListRef.value) {
      messageListRef.value.scrollTop = messageListRef.value.scrollHeight
    }
  })
}

const handleWebSocketMessage = (message: WebSocketMessage) => {
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
  }
  
  chatStore.setLoading(false)
  scrollToBottom()
}

const handleWebSocketError = (error: Event) => {
  showToast('连接出错，请重试')
  chatStore.setConnected(false)
}

const handleWebSocketClose = () => {
  chatStore.setConnected(false)
}

const sendMessage = () => {
  if (!inputValue.value.trim() || !chatStore.isConnected) return
  
  chatStore.addMessage({
    type: 'user',
    content: inputValue.value
  })
  
  wsClient?.answer(inputValue.value)
  inputValue.value = ''
  chatStore.setLoading(true)
  scrollToBottom()
}

const onClickLeft = () => {
  showConfirmDialog({
    title: '提示',
    message: '确定要离开当前对话吗？'
  }).then(() => {
    wsClient?.disconnect()
    router.push('/')
  }).catch(() => {})
}

const onActionSelect = (action: { value: string }) => {
  showActions.value = false
  
  switch (action.value) {
    case 'history':
      showToast('查看历史功能开发中')
      break
    case 'complete':
      showConfirmDialog({
        title: '提示',
        message: '确定要结束对话并获取推荐吗？'
      }).then(() => {
        wsClient?.complete()
      }).catch(() => {})
      break
    case 'restart':
      showConfirmDialog({
        title: '提示',
        message: '确定要重新开始对话吗？'
      }).then(() => {
        wsClient?.disconnect()
        chatStore.clearChat()
        router.push('/')
      }).catch(() => {})
      break
  }
}

onMounted(() => {
  if (!chatStore.sessionId) {
    router.push('/')
    return
  }
  
  wsClient = new WebSocketClient({
    sessionId: chatStore.sessionId,
    onMessage: handleWebSocketMessage,
    onError: handleWebSocketError,
    onClose: handleWebSocketClose
  })
  
  wsClient.connect()
  chatStore.setConnected(true)
})

onUnmounted(() => {
  wsClient?.disconnect()
})
</script>

<style lang="scss" scoped>
.chat-container {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: #f7f8fa;
}

.progress-bar {
  padding: 10px 15px;
  background: white;
  display: flex;
  align-items: center;
  gap: 10px;
  border-bottom: 1px solid #ebedf0;
}

.progress-text {
  font-size: 12px;
  color: #666;
  min-width: 30px;
}

.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 15px;
}

.welcome-message {
  text-align: center;
  color: #999;
  padding: 50px 0;
}

.message-item {
  display: flex;
  margin-bottom: 15px;
  
  &.user {
    flex-direction: row-reverse;
    
    .message-content {
      margin-right: 10px;
      margin-left: 0;
    }
    
    .message-text {
      background: #1989fa;
      color: white;
    }
  }
  
  &.ai {
    .message-content {
      margin-left: 10px;
    }
    
    .message-text {
      background: white;
      color: #333;
    }
  }
}

.avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: #1989fa;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  
  .message-item.user & {
    background: #07c160;
  }
}

.message-content {
  max-width: 70%;
}

.message-text {
  padding: 10px 15px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.5;
  word-break: break-word;
}

.message-time {
  font-size: 10px;
  color: #999;
  margin-top: 4px;
  text-align: right;
}

.loading-indicator {
  display: flex;
  justify-content: center;
  padding: 15px;
}

.input-area {
  display: flex;
  padding: 10px 15px;
  background: white;
  border-top: 1px solid #ebedf0;
  gap: 10px;
  
  .van-field {
    flex: 1;
    background: #f7f8fa;
    border-radius: 20px;
    padding: 8px 15px;
  }
}
</style>
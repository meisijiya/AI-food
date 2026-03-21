import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export interface ChatMessage {
  id: string
  type: 'question' | '2question' | 'chat' | 'interrupt' | 'recommend' | 'system' | 'user'
  param?: string
  content: string
  timestamp: number
}

export interface Progress {
  current: number
  total: number
  collected: string[]
}

export const useChatStore = defineStore('chat', () => {
  const sessionId = ref<string>('')
  const messages = ref<ChatMessage[]>([])
  const progress = ref<Progress>({
    current: 0,
    total: 7,
    collected: []
  })
  const isConnected = ref(false)
  const isLoading = ref(false)
  const currentPhase = ref<'chat' | 'recommend'>('chat')
  const recommendationResult = ref<any>(null)
  // 前端自行追踪收集到的参数值（从 WebSocket 消息的 param + content 中提取）
  const collectedParamValues = ref<Record<string, string>>({})

  const messageCount = computed(() => messages.value.length)

  function setSessionId(id: string) {
    sessionId.value = id
  }

  function addMessage(message: Omit<ChatMessage, 'id' | 'timestamp'>) {
    messages.value.push({
      ...message,
      id: Date.now().toString(),
      timestamp: Date.now()
    })
    // 从 chat/confirm 类型消息中提取参数值
    if ((message.type === 'chat' || message.type === 'question') && message.param) {
      // 尝试从确认消息中提取值，如 "好的，时间是晚上，我记下了！"
      const match = message.content.match(/是(.+?)，我记下了/)
      if (match) {
        collectedParamValues.value[message.param] = match[1]
      }
    }
    // 用户消息也尝试关联到当前参数
    if (message.type === 'user' && message.content) {
      // 这里不做自动关联，由后端决定
    }
  }

  function setCollectedParamValue(param: string, value: string) {
    collectedParamValues.value[param] = value
  }

  function updateProgress(newProgress: Progress) {
    progress.value = newProgress
  }

  function setConnected(status: boolean) {
    isConnected.value = status
  }

  function setLoading(status: boolean) {
    isLoading.value = status
  }

  function setPhase(phase: 'chat' | 'recommend') {
    currentPhase.value = phase
  }

  function setRecommendationResult(result: any) {
    recommendationResult.value = result
  }

  function clearChat() {
    sessionId.value = ''
    messages.value = []
    progress.value = { current: 0, total: 7, collected: [] }
    currentPhase.value = 'chat'
    recommendationResult.value = null
    isConnected.value = false
    isLoading.value = false
    collectedParamValues.value = {}
  }

  return {
    sessionId,
    messages,
    progress,
    isConnected,
    isLoading,
    currentPhase,
    recommendationResult,
    collectedParamValues,
    messageCount,
    setSessionId,
    addMessage,
    setCollectedParamValue,
    updateProgress,
    setConnected,
    setLoading,
    setPhase,
    setRecommendationResult,
    clearChat
  }
})

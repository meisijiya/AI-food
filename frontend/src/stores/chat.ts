import { defineStore } from 'pinia'
import { ref } from 'vue'

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
  const progress = ref<Progress>({ current: 0, total: 7, collected: [] })
  const isConnected = ref(false)
  const isLoading = ref(false)
  const currentPhase = ref<'chat' | 'recommend'>('chat')
  const recommendationResult = ref<any>(null)

  function setSessionId(id: string) { sessionId.value = id }

  function addMessage(message: Omit<ChatMessage, 'id' | 'timestamp'>) {
    messages.value.push({ ...message, id: Date.now().toString(), timestamp: Date.now() })
  }

  function updateProgress(newProgress: Progress) { progress.value = newProgress }
  function setConnected(status: boolean) { isConnected.value = status }
  function setLoading(status: boolean) { isLoading.value = status }
  function setPhase(phase: 'chat' | 'recommend') { currentPhase.value = phase }

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
  }

  return {
    sessionId, messages, progress, isConnected, isLoading,
    currentPhase, recommendationResult,
    setSessionId, addMessage,
    updateProgress, setConnected, setLoading, setPhase,
    setRecommendationResult, clearChat
  }
})

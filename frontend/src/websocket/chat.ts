import { useAuthStore } from '@/stores/auth'

type MessageHandler = (data: any) => void

class ChatWebSocketClient {
  private ws: WebSocket | null = null
  private handlers: Map<string, MessageHandler[]> = new Map()
  private reconnectAttempts = 0
  private maxReconnectAttempts = 5
  private reconnectDelay = 3000
  private pingInterval: number | null = null
  private isConnected = false
  private isAuthenticated = false

  connect() {
    const authStore = useAuthStore()
    if (!authStore.token) {
      console.error('No token available for chat WebSocket')
      return
    }

    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
    const host = import.meta.env.VITE_WS_HOST || window.location.host
    const url = `${protocol}//${host}/ws/chat?token=${encodeURIComponent(authStore.token)}`

    this.ws = new WebSocket(url)

    this.ws.onopen = () => {
      console.log('Chat WebSocket connected')
      this.isConnected = true
      this.reconnectAttempts = 0

      // 发送认证消息
      this.send({
        action: 'auth',
        token: authStore.token
      })
    }

    this.ws.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data)
        this.handleMessage(data)
      } catch (e) {
        console.error('Failed to parse chat message', e)
      }
    }

    this.ws.onclose = () => {
      console.log('Chat WebSocket closed')
      this.isConnected = false
      this.isAuthenticated = false
      this.stopPing()
      this.attemptReconnect()
    }

    this.ws.onerror = (error) => {
      console.error('Chat WebSocket error', error)
    }
  }

  private handleMessage(data: any) {
    const { type } = data

    // 处理认证成功
    if (type === 'auth' && data.success) {
      this.isAuthenticated = true
      this.startPing()
      console.log('Chat authenticated')
    }

    // 触发注册的处理器
    const handlers = this.handlers.get(type)
    if (handlers) {
      handlers.forEach(handler => handler(data))
    }

    // 触发通配符处理器
    const allHandlers = this.handlers.get('*')
    if (allHandlers) {
      allHandlers.forEach(handler => handler(data))
    }
  }

  on(type: string, handler: MessageHandler) {
    if (!this.handlers.has(type)) {
      this.handlers.set(type, [])
    }
    this.handlers.get(type)!.push(handler)
  }

  off(type: string, handler: MessageHandler) {
    const handlers = this.handlers.get(type)
    if (handlers) {
      const index = handlers.indexOf(handler)
      if (index > -1) {
        handlers.splice(index, 1)
      }
    }
  }

  send(data: any) {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      this.ws.send(JSON.stringify(data))
    } else {
      console.warn('Chat WebSocket not connected')
    }
  }

  sendMessage(receiverId: number, content: string, messageType: string = 'text', photoId?: number, fileId?: number) {
    if (!this.ws || this.ws.readyState !== WebSocket.OPEN || !this.isAuthenticated) {
      console.warn('Chat WebSocket is not ready for sending')
      return false
    }

    this.send({
      action: 'send',
      receiverId,
      content,
      messageType,
      photoId,
      fileId
    })
    return true
  }

  markRead(conversationId: number) {
    this.send({
      action: 'read',
      conversationId
    })
  }

  private startPing() {
    this.pingInterval = window.setInterval(() => {
      this.send({ action: 'ping' })
    }, 30000) // 每30秒发送心跳
  }

  private stopPing() {
    if (this.pingInterval) {
      clearInterval(this.pingInterval)
      this.pingInterval = null
    }
  }

  private attemptReconnect() {
    if (this.reconnectAttempts >= this.maxReconnectAttempts) {
      console.error('Max reconnect attempts reached')
      return
    }

    this.reconnectAttempts++
    console.log(`Attempting to reconnect (${this.reconnectAttempts}/${this.maxReconnectAttempts})...`)

    setTimeout(() => {
      this.connect()
    }, this.reconnectDelay)
  }

  disconnect() {
    this.stopPing()
    if (this.ws) {
      this.ws.close()
      this.ws = null
    }
    this.isConnected = false
    this.isAuthenticated = false
    this.handlers.clear()
  }

  get connected() {
    return this.isConnected
  }

  get authenticated() {
    return this.isAuthenticated
  }
}

// 单例模式
export const chatWs = new ChatWebSocketClient()
export default chatWs

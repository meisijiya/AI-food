import { useAuthStore } from '@/stores/auth'

export interface WebSocketMessage {
  type: 'question' | '2question' | 'chat' | 'interrupt' | 'recommend' | 'system' | 'error'
  param?: string
  content: string
  progress?: {
    current: number
    total: number
    collected: string[]
  }
}

export interface WebSocketClientOptions {
  sessionId: string
  onOpen: () => void
  onMessage: (message: WebSocketMessage) => void
  onError: (error: Event) => void
  onClose: () => void
}

export class WebSocketClient {
  private ws: WebSocket | null = null
  private sessionId: string
  private onOpen: () => void
  private onMessage: (message: WebSocketMessage) => void
  private onError: (error: Event) => void
  private onClose: () => void
  private reconnectAttempts = 0
  private maxReconnectAttempts = 5
  private reconnectDelay = 3000
  private isFirstConnection = true
  private manuallyDisconnected = false

  constructor(options: WebSocketClientOptions) {
    this.sessionId = options.sessionId
    this.onOpen = options.onOpen
    this.onMessage = options.onMessage
    this.onError = options.onError
    this.onClose = options.onClose
  }

  connect(): void {
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
    const wsHost = import.meta.env.VITE_WS_HOST || window.location.host
    // 安全修复（M1）：token 通过 Sec-WebSocket-Protocol 子协议传递，不再写入 URL query
    // ——避免 token 出现在 nginx access log / 浏览器 history / 网络抓包
    const authStore = useAuthStore()
    const token = authStore.token || ''

    let wsUrl: string
    if (wsHost.startsWith('/')) {
      // 相对路径配置，直接拼接
      wsUrl = `${protocol}//${window.location.host}${wsHost}/conversation/${this.sessionId}`
    } else {
      // 完整主机名配置
      wsUrl = `${protocol}//${wsHost}/ws/conversation/${this.sessionId}`
    }

    try {
      // 子协议格式："jwt." + token（如 "jwt.eyJhbGciOiJIUzI1NiJ9..."）
      // 后端 JwtHandshakeInterceptor 从 Sec-WebSocket-Protocol 请求头读取
      this.ws = token
        ? new WebSocket(wsUrl, ['jwt.' + token])
        : new WebSocket(wsUrl)

      this.ws.onopen = () => {
        this.reconnectAttempts = 0

        // 仅首次连接发送 start；重连不重复发送（避免覆盖后端状态）
        if (this.isFirstConnection) {
          this.isFirstConnection = false
          this.send({ action: 'start', sessionId: this.sessionId, content: '' })
        }

        this.onOpen()
      }

      this.ws.onmessage = (event) => {
        try {
          const message: WebSocketMessage = JSON.parse(event.data)
          this.onMessage(message)
        } catch (error) {
          console.error('[WS] Failed to parse message')
        }
      }

      this.ws.onerror = (error) => {
        console.error('[WS] Error:', error)
        this.onError(error)
      }

      this.ws.onclose = () => {
        this.onClose()
        if (!this.manuallyDisconnected) {
          this.tryReconnect()
        }
      }
    } catch (error) {
      console.error('[WS] Failed to create connection:', error)
      this.onError(error as Event)
    }
  }

  private tryReconnect(): void {
    if (this.reconnectAttempts < this.maxReconnectAttempts) {
      this.reconnectAttempts++

      setTimeout(() => {
        this.connect()
      }, this.reconnectDelay)
    } else {
      console.error('[WS] Max reconnect attempts reached')
    }
  }

  send(message: { action: string; sessionId: string; content: string }): void {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      this.ws.send(JSON.stringify(message))
    } else {
      console.error('[WS] Cannot send, readyState:', this.ws?.readyState)
    }
  }

  answer(content: string): void {
    this.send({ action: 'answer', sessionId: this.sessionId, content })
  }

  complete(): void {
    this.send({ action: 'complete', sessionId: this.sessionId, content: '' })
  }

  cancel(): void {
    this.send({ action: 'cancel', sessionId: this.sessionId, content: '' })
  }

  disconnect(): void {
    this.manuallyDisconnected = true
    if (this.ws) {
      this.ws.close()
      this.ws = null
    }
  }

  isConnected(): boolean {
    return this.ws !== null && this.ws.readyState === WebSocket.OPEN
  }
}

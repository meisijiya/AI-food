export interface WebSocketMessage {
  type: 'question' | '2question' | 'chat' | 'interrupt' | 'recommend' | 'system'
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
  onMessage: (message: WebSocketMessage) => void
  onError: (error: Event) => void
  onClose: () => void
}

export class WebSocketClient {
  private ws: WebSocket | null = null
  private sessionId: string
  private onMessage: (message: WebSocketMessage) => void
  private onError: (error: Event) => void
  private onClose: () => void
  private reconnectAttempts = 0
  private maxReconnectAttempts = 5
  private reconnectDelay = 3000

  constructor(options: WebSocketClientOptions) {
    this.sessionId = options.sessionId
    this.onMessage = options.onMessage
    this.onError = options.onError
    this.onClose = options.onClose
  }

  connect(): void {
    const wsUrl = `ws://${window.location.host}/ws/conversation/${this.sessionId}`
    
    try {
      this.ws = new WebSocket(wsUrl)
      
      this.ws.onopen = () => {
        console.log('WebSocket connected')
        this.reconnectAttempts = 0
        this.send({ action: 'start', sessionId: this.sessionId, content: '' })
      }
      
      this.ws.onmessage = (event) => {
        try {
          const message: WebSocketMessage = JSON.parse(event.data)
          this.onMessage(message)
        } catch (error) {
          console.error('Failed to parse WebSocket message:', error)
        }
      }
      
      this.ws.onerror = (error) => {
        console.error('WebSocket error:', error)
        this.onError(error)
      }
      
      this.ws.onclose = () => {
        console.log('WebSocket closed')
        this.onClose()
        this.tryReconnect()
      }
    } catch (error) {
      console.error('Failed to create WebSocket connection:', error)
      this.onError(error as Event)
    }
  }

  private tryReconnect(): void {
    if (this.reconnectAttempts < this.maxReconnectAttempts) {
      this.reconnectAttempts++
      console.log(`Attempting to reconnect (${this.reconnectAttempts}/${this.maxReconnectAttempts})...`)
      
      setTimeout(() => {
        this.connect()
      }, this.reconnectDelay)
    } else {
      console.error('Max reconnect attempts reached')
    }
  }

  send(message: { action: string; sessionId: string; content: string }): void {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      this.ws.send(JSON.stringify(message))
    } else {
      console.error('WebSocket is not connected')
    }
  }

  answer(content: string): void {
    this.send({ action: 'answer', sessionId: this.sessionId, content })
  }

  complete(): void {
    this.send({ action: 'complete', sessionId: this.sessionId, content: '' })
  }

  disconnect(): void {
    if (this.ws) {
      this.ws.close()
      this.ws = null
    }
  }
}
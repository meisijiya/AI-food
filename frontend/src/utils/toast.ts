import { createApp, h, ref, onMounted } from 'vue'

type ToastType = 'success' | 'error' | 'info'

interface ToastOptions {
  message: string
  type?: ToastType
  duration?: number
}

const icons: Record<ToastType, string> = {
  success: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M20 6 9 17l-5-5"/></svg>',
  error: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><path d="m15 9-6 6"/><path d="m9 9 6 6"/></svg>',
  info: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><path d="M12 16v-4"/><path d="M12 8h.01"/></svg>'
}

function showToast(options: ToastOptions | string) {
  const opts: ToastOptions = typeof options === 'string'
    ? { message: options, type: 'info' }
    : { type: 'info', duration: 2200, ...options }

  const container = document.createElement('div')
  container.className = 'sanctuary-toast-container'
  document.body.appendChild(container)

  const visible = ref(true)

  const app = createApp({
    setup() {
      onMounted(() => {
        setTimeout(() => {
          visible.value = false
          setTimeout(() => {
            app.unmount()
            document.body.removeChild(container)
          }, 300)
        }, opts.duration || 2200)
      })
      return { visible, opts }
    },
    render() {
      if (!this.visible) return null
      return h('div', {
        class: 'sanctuary-toast-overlay'
      }, [
        h('div', {
          class: `sanctuary-toast toast-${this.opts.type || 'info'}`,
          innerHTML: `
            <span class="toast-icon">${icons[this.opts.type || 'info']}</span>
            <span class="toast-msg">${this.opts.message}</span>
          `
        })
      ])
    }
  })

  app.mount(container)
}

export function showSuccess(message: string) {
  showToast({ message, type: 'success' })
}

export function showError(message: string) {
  showToast({ message, type: 'error', duration: 3000 })
}

export function showInfo(message: string) {
  showToast({ message, type: 'info' })
}

// 兼容 Vant 的 showToast 调用
export { showToast }

// 注入全局样式
function injectStyles() {
  if (document.getElementById('sanctuary-toast-styles')) return
  const style = document.createElement('style')
  style.id = 'sanctuary-toast-styles'
  style.textContent = `
    .sanctuary-toast-container {
      position: fixed;
      inset: 0;
      z-index: 9999;
      pointer-events: none;
      display: flex;
      align-items: flex-start;
      justify-content: center;
      padding-top: 100px;
    }

    .sanctuary-toast-overlay {
      pointer-events: none;
      animation: toast-fade-in 0.3s cubic-bezier(0.22, 1, 0.36, 1) forwards;
    }

    .sanctuary-toast-overlay[style*="display: none"],
    .sanctuary-toast-overlay:has(.toast-exit) {
      animation: toast-fade-out 0.25s ease forwards;
    }

    .sanctuary-toast {
      display: inline-flex;
      align-items: center;
      gap: 10px;
      padding: 14px 24px;
      border-radius: 100px;
      backdrop-filter: blur(24px);
      -webkit-backdrop-filter: blur(24px);
      box-shadow: 0 8px 32px rgba(0, 0, 0, 0.15);
      pointer-events: auto;
      font-family: "Plus Jakarta Sans", -apple-system, sans-serif;
      font-size: 14px;
      font-weight: 500;
      line-height: 1;
      white-space: nowrap;
    }

    .toast-success {
      background: rgba(34, 197, 94, 0.9);
      color: white;
    }

    .toast-error {
      background: rgba(239, 68, 68, 0.9);
      color: white;
    }

    .toast-info {
      background: rgba(11, 15, 16, 0.88);
      color: rgba(255, 255, 255, 0.9);
    }

    .toast-icon {
      display: flex;
      align-items: center;
      line-height: 0;
    }

    .toast-msg {
      letter-spacing: 0.02em;
    }

    @keyframes toast-fade-in {
      from {
        opacity: 0;
        transform: translateY(-16px) scale(0.95);
      }
      to {
        opacity: 1;
        transform: translateY(0) scale(1);
      }
    }

    @keyframes toast-fade-out {
      from {
        opacity: 1;
        transform: translateY(0) scale(1);
      }
      to {
        opacity: 0;
        transform: translateY(-10px) scale(0.96);
      }
    }
  `
  document.head.appendChild(style)
}

injectStyles()

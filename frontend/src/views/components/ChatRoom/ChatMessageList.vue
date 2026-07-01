<template>
  <!-- 消息列表：滚动 / 加载更多 / 气泡渲染 / 附件操作面板 -->
  <div class="messages-container" ref="messagesContainer" @scroll="onScroll" @click="emit('close-panels')">
    <!-- Load more indicator -->
    <div v-if="loadingMore" class="load-more">
      <div class="spinner"></div>
    </div>
    <div v-if="!hasMore && messages.length > 0" class="no-more">没有更多消息了</div>

    <div
      v-for="msg in messages"
      :key="msg.id"
      class="message-item"
      :class="{ 'message-self': msg.senderId === currentUserId }"
    >
      <div v-if="msg.senderId !== currentUserId" class="msg-avatar">
        <img v-if="partnerAvatar" :src="partnerAvatar" alt="" />
        <span v-else>{{ partnerNickname?.charAt(0) || '?' }}</span>
      </div>

      <!-- 图片消息左侧删除按钮 -->
      <div v-if="msg.messageType === 'image' && !isDeletedAttachment(msg)" class="delete-btn-wrapper">
        <button class="delete-btn delete-btn-image" @click.stop="emitAction('delete-photo', msg)">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
        </button>
      </div>

      <div class="message-bubble" :class="{ 'bubble-clickable': msg.messageType === 'text' || msg.messageType === 'file' }" @click.stop="handleBubbleClick(msg)">
        <!-- 文本消息 -->
        <div v-if="msg.messageType === 'text'" class="message-content">{{ msg.content }}</div>
        <!-- 图片消息 -->
        <div v-else-if="msg.messageType === 'image' && !isDeletedAttachment(msg)" class="message-image" @click.stop="emitAction('preview-image', msg)">
          <CachedImage :src="parseMediaUrl(msg.content, 'thumbnailUrl')" alt="图片" :lazy="true" />
        </div>
        <div v-else-if="msg.messageType === 'image'" class="deleted-attachment">
          <span>图片已删除</span>
        </div>
        <!-- 文件消息 -->
        <div v-else-if="msg.messageType === 'file' && !isDeletedAttachment(msg)" class="message-file">
          <div class="file-icon">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>
          </div>
          <div class="file-info">
            <div class="file-name">{{ parseMediaUrl(msg.content, 'fileName') }}</div>
            <div class="file-size">{{ formatFileSize(parseMediaUrl(msg.content, 'fileSize')) }}</div>
          </div>
        </div>
        <div v-else-if="msg.messageType === 'file'" class="deleted-attachment">
          <span>文件已删除</span>
        </div>
        <!-- 兜底 -->
        <div v-else class="message-content">{{ msg.content }}</div>
        <div class="message-time">{{ formatTime(msg.createdAt) }}</div>
      </div>

      <!-- 消息操作面板 -->
      <div v-if="activeMessageId === msg.id" class="message-actions-panel" :class="{ 'panel-self': msg.senderId === currentUserId }">
        <template v-if="msg.messageType === 'text'">
          <button class="action-btn" @click.stop="emitAction('copy', msg)">复制内容</button>
          <button class="action-btn action-btn-delete" @click.stop="emitAction('delete-message', msg)">删除</button>
        </template>
        <template v-else-if="msg.messageType === 'file' && !isDeletedAttachment(msg)">
          <button class="action-btn" @click.stop="emitAction('download-file', msg)">下载</button>
          <button class="action-btn action-btn-delete" @click.stop="emitAction('delete-file', msg)">删除</button>
        </template>
      </div>

      <div v-if="msg.senderId === currentUserId" class="msg-avatar msg-avatar-self">
        <img v-if="myAvatar" :src="myAvatar" alt="" />
        <span v-else>{{ myNickname?.charAt(0) || '?' }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
// 消息列表子组件：纯渲染 + 滚动 / 操作面板 UI 状态。
// 所有业务事件（删除 / 复制 / 下载 / 预览）通过 emit 交给父组件处理。
import { ref, nextTick } from 'vue'
import CachedImage from '@/components/CachedImage.vue'
import type { ChatMessage } from '@/types/chat'

const props = defineProps<{
  messages: ChatMessage[]
  currentUserId: number | undefined
  partnerAvatar: string
  partnerNickname: string
  myAvatar: string
  myNickname: string
  loadingMore: boolean
  hasMore: boolean
}>()

const emit = defineEmits<{
  (e: 'load-more'): void
  (e: 'close-panels'): void
  (e: 'message-action', payload: { action: string; msg: ChatMessage }): void
}>()

const messagesContainer = ref<HTMLElement>()
const activeMessageId = ref<number | null>(null)

/** 转发消息操作事件 */
function emitAction(action: string, msg: ChatMessage) {
  emit('message-action', { action, msg })
}

/**
 * 处理消息气泡点击：
 * - 图片类型：直接 emit 预览（父组件负责打开 vant preview）
 * - 文本 / 文件：在当前消息上切换操作面板
 */
function handleBubbleClick(msg: ChatMessage) {
  if (msg.messageType === 'image') {
    emitAction('preview-image', msg)
    return
  }
  activeMessageId.value = activeMessageId.value === msg.id ? null : msg.id
}

/**
 * 滚动监听：滚到顶部时通知父组件加载更早的消息，父组件负责实际翻页与位置补偿。
 */
function onScroll() {
  const el = messagesContainer.value
  if (!el || props.loadingMore || !props.hasMore) return
  if (el.scrollTop < 60) {
    emit('load-more')
  }
}

/**
 * 解析消息内容 JSON 字段（thumbnailUrl / originalUrl / fileName / fileSize）。
 */
function parseMediaUrl(content: string | object, field: string): string {
  try {
    if (!content) return ''
    const data = typeof content === 'string' ? JSON.parse(content) : content
    return data?.[field] || ''
  } catch {
    return ''
  }
}

/**
 * 判断附件消息是否已在当前视图中被删除（用占位内容表示）。
 */
function isDeletedAttachment(msg: ChatMessage): boolean {
  return msg.content === '__deleted_attachment__'
}

function formatFileSize(size: string | number): string {
  const bytes = Number(size) || 0
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

function formatTime(dateStr: string): string {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
}

// 暴露给父级：发送 / 接收消息后滚到底部 + 加载更多后保持滚动位置 + 重置操作面板
defineExpose({
  scrollToBottom() {
    nextTick(() => {
      const el = messagesContainer.value
      if (el) el.scrollTop = el.scrollHeight
    })
  },
  /** 加载更多消息后保持滚动位置（父组件在 fetchMessages 后调用） */
  restoreScrollPosition(prevScrollHeight: number) {
    nextTick(() => {
      const el = messagesContainer.value
      if (!el) return
      const newScrollHeight = el.scrollHeight
      el.scrollTop = newScrollHeight - prevScrollHeight
    })
  },
  /** 重置活动操作面板 */
  clearActiveMessage() {
    activeMessageId.value = null
  }
})
</script>

<style lang="scss" scoped>
.messages-container {
  flex: 1;
  overflow-y: auto;
  padding: 16px 12px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  -webkit-overflow-scrolling: touch;
}

.load-more {
  display: flex;
  justify-content: center;
  padding: 12px 0;
}

.spinner {
  width: 20px;
  height: 20px;
  border: 2px solid var(--color-surface-container-low);
  border-top-color: var(--color-primary);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin { to { transform: rotate(360deg); } }

.no-more {
  text-align: center;
  font-size: 12px;
  color: var(--color-on-surface-variant);
  opacity: 0.5;
  padding: 8px 0;
}

.message-item {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  position: relative;

  &.message-self {
    justify-content: flex-end;
    .message-bubble {
      background: linear-gradient(135deg, var(--color-primary-container), var(--color-primary));
      color: white;
      border-radius: 1.25rem 1.25rem 4px 1.25rem;
    }
    .message-time {
      color: rgba(255, 255, 255, 0.7);
    }
  }

  &:hover .delete-btn-image {
    opacity: 1;
  }
}

.msg-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--color-primary-container), var(--color-primary));
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-family: var(--font-serif);
  font-style: italic;
  font-size: 13px;
  font-weight: 400;
  flex-shrink: 0;
  overflow: hidden;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}

.message-bubble {
  max-width: 70%;
  padding: 10px 14px;
  background: var(--color-surface-container-lowest);
  border-radius: 1.25rem 1.25rem 1.25rem 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  position: relative;

  &.bubble-clickable {
    cursor: pointer;
  }
}

.message-content {
  font-size: 14px;
  line-height: 1.5;
  word-break: break-word;
}

.message-time {
  font-size: 10px;
  color: var(--color-on-surface-variant);
  margin-top: 4px;
  text-align: right;
}

.message-image {
  cursor: pointer;
  border-radius: 12px;
  overflow: hidden;
  max-width: 200px;
  img {
    width: 100%;
    display: block;
    border-radius: 12px;
  }
}

.message-file {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  padding: 4px 0;
}

.deleted-attachment {
  min-width: 140px;
  padding: 8px 10px;
  border-radius: 10px;
  background: var(--color-surface-container-low);
  color: var(--color-on-surface-variant);
  font-size: 13px;
  text-align: center;
}

.file-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  background: var(--color-surface-container-low);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-on-surface-variant);
  flex-shrink: 0;
}

.file-info {
  min-width: 0;
}

.file-name {
  font-size: 13px;
  font-weight: 500;
  color: var(--color-on-surface);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 160px;
}

.file-size {
  font-size: 11px;
  color: var(--color-on-surface-variant);
  opacity: 0.7;
}

.delete-btn-wrapper {
  display: flex;
  align-items: center;
  flex-shrink: 0;
}

.delete-btn {
  width: 24px;
  height: 24px;
  border: none;
  border-radius: 50%;
  background: rgba(239, 68, 68, 0.1);
  color: #ef4444;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: background 0.2s, transform 0.15s;
  flex-shrink: 0;

  &:active {
    background: rgba(239, 68, 68, 0.2);
    transform: scale(0.9);
  }

  &.delete-btn-image {
    position: absolute;
    top: -8px;
    left: -4px;
    width: 22px;
    height: 22px;
    background: rgba(0, 0, 0, 0.5);
    color: white;
    opacity: 0;
    transition: opacity 0.2s;
  }
}

.message-actions-panel {
  position: absolute;
  bottom: calc(100% + 8px);
  left: 0;
  display: flex;
  gap: 6px;
  background: var(--color-surface-container-lowest);
  border-radius: 8px;
  padding: 6px 8px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
  z-index: 100;
  white-space: nowrap;

  &.panel-self {
    left: auto;
    right: 0;
  }
}

.action-btn {
  padding: 6px 12px;
  border: none;
  border-radius: 6px;
  background: var(--color-surface-container-low);
  color: var(--color-on-surface);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.2s;

  &:active {
    background: var(--color-surface-container-high);
  }

  &.action-btn-delete {
    background: rgba(239, 68, 68, 0.1);
    color: #ef4444;

    &:active {
      background: rgba(239, 68, 68, 0.2);
    }
  }
}
</style>

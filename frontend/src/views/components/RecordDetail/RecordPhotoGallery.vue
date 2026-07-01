<template>
  <div v-if="photo && !showUpload" class="photo-card card-enter card-delay-2">
    <div class="section-title">
      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14.5 4h-5L7 7H4a2 2 0 0 0-2 2v9a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2V9a2 2 0 0 0-2-2h-3l-2.5-3z"/><circle cx="12" cy="13" r="3"/></svg>
      <span>美食照片</span>
    </div>
    <div class="photo-body">
      <div class="photo-glow"></div>
      <img
        :src="photo.thumbnailPath"
        class="photo-image"
        alt="美食照片"
        @click="openPhotoModal(photo.originalPath)"
      />
      <div class="photo-actions">
        <button class="photo-action-btn" @click="showUpload = true" title="更换照片">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" x2="12" y1="3" y2="15"/></svg>
        </button>
        <button class="photo-action-btn delete" @click="emit('delete')" title="删除照片">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M3 6h18"/><path d="M19 6v14c0 1-1 2-2 2H7c-1 0-2-1-2-2V6"/><path d="M8 6V4c0-1 1-2 2-2h4c1 0 2 1 2 2v2"/></svg>
        </button>
      </div>
    </div>
  </div>

  <UploadPhoto
    v-if="!photo || showUpload"
    :session-id="sessionId"
    @uploaded="handleUploaded"
  />

  <Transition name="fade">
    <div v-if="photoModalUrl" class="photo-modal" @click.self="photoModalUrl = null">
      <div class="photo-modal-content">
        <img :src="photoModalUrl" class="full-photo" alt="原始照片" />
        <button class="photo-modal-close" @click="photoModalUrl = null">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M18 6 6 18"/><path d="m6 6 12 12"/></svg>
        </button>
      </div>
    </div>
  </Transition>
</template>

<script setup lang="ts">
// 记录详情 - 照片展示子组件
// 负责: 照片墙渲染、点击放大、替换按钮、删除按钮、photo upload modal
import { ref } from 'vue'
import UploadPhoto from '@/components/UploadPhoto.vue'

// 父组件传入的 props
defineProps<{
  photo: { thumbnailPath: string; originalPath: string } | null
  sessionId: string
}>()

// 事件回传父组件(API 调用由父组件负责)
const emit = defineEmits<{
  (e: 'uploaded', data: { thumbnailUrl: string; originalUrl: string }): void
  (e: 'delete'): void
}>()

// 子组件内部 UI 状态
const showUpload = ref(false)
const photoModalUrl = ref<string | null>(null)

// 打开全屏照片预览
function openPhotoModal(url: string) {
  photoModalUrl.value = url
}

// UploadPhoto 上传完成后: 关闭 upload UI,通知父组件
function handleUploaded(data: { thumbnailUrl: string; originalUrl: string }) {
  showUpload.value = false
  emit('uploaded', data)
}
</script>

<style lang="scss" scoped>
/* ===== Card Entrance Animation (self-contained, no parent dependency) ===== */
.card-enter {
  animation: card-slide-up 0.5s cubic-bezier(0.22, 1, 0.36, 1) both;
}
.card-delay-2 { animation-delay: 0.1s; }

@keyframes card-slide-up {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}

/* ===== Fade Transition (for photo modal) ===== */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* ===== Section Title (shared shape) ===== */
.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-family: var(--font-serif);
  font-style: italic;
  font-size: 18px;
  font-weight: 500;
  color: var(--color-on-surface);
  margin-bottom: 14px;

  svg {
    color: var(--color-primary);
  }
}

/* ===== Photo Card ===== */
.photo-card {
  margin-bottom: 24px;
}

.photo-body {
  position: relative;
  overflow: hidden;
  border-radius: 2rem;
  background: var(--color-surface-container-lowest);
  border: 1px solid rgba(255, 255, 255, 0.8);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.04);
}

.photo-glow {
  position: absolute;
  top: -40px;
  left: -30px;
  width: 140px;
  height: 140px;
  background: var(--color-primary-container);
  opacity: 0.06;
  border-radius: 50%;
  filter: blur(40px);
  pointer-events: none;
  z-index: 0;
}

.photo-image {
  width: 100%;
  display: block;
  max-height: 360px;
  object-fit: cover;
  cursor: pointer;
  transition: transform 0.5s cubic-bezier(0.22, 1, 0.36, 1);
  position: relative;
  z-index: 1;

  &:hover {
    transform: scale(1.02);
  }
}

.photo-actions {
  display: flex;
  gap: 6px;
  position: absolute;
  top: 12px;
  right: 12px;
  z-index: 2;
}

.photo-action-btn {
  width: 36px;
  height: 36px;
  border: none;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.25s;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  color: var(--color-on-surface);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);

  &:hover {
    background: rgba(255, 255, 255, 0.95);
    transform: scale(1.08);
  }

  &:active {
    transform: scale(0.95);
  }

  &.delete {
    color: #ef4444;
    &:hover { background: rgba(255, 240, 240, 0.95); }
  }
}

/* ===== Photo Modal ===== */
.photo-modal {
  position: fixed;
  inset: 0;
  z-index: 200;
  background: rgba(11, 15, 16, 0.7);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}

.photo-modal-content {
  position: relative;
  max-width: 100%;
  max-height: 100%;
}

.full-photo {
  max-width: 100%;
  max-height: 85vh;
  border-radius: 1.5rem;
  object-fit: contain;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
}

.photo-modal-close {
  position: absolute;
  top: -12px;
  right: -12px;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  border: none;
  background: rgba(255, 255, 255, 0.15);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    background: rgba(255, 255, 255, 0.3);
    transform: scale(1.1);
  }
}
</style>

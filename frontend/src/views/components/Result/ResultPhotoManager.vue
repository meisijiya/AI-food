<template>
  <!--
    ResultPhotoManager - 照片管理(展示 / 上传 / 删除 / 全屏预览)
    内部维护 showUpload + showFullPhoto 状态
    所有 API 调用(recordApi.updatePhoto / deletePhoto)由父组件在 emit 处理函数中完成
  -->
  <div v-if="sessionId">
    <!-- 已上传照片 -->
    <div v-if="photo && !showUpload" class="uploaded-photo-card animate-fade-up delay-450 animate-start-hidden">
      <div class="photo-header">
        <div class="upload-label">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14.5 4h-5L7 7H4a2 2 0 0 0-2 2v9a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2V9a2 2 0 0 0-2-2h-3l-2.5-3z"/><circle cx="12" cy="13" r="3"/></svg>
          <span>美食照片</span>
        </div>
        <div class="photo-actions">
          <button class="photo-action-btn replace" @click="showUpload = true" title="更换照片">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" x2="12" y1="3" y2="15"/></svg>
          </button>
          <button class="photo-action-btn delete" @click="emit('photo-deleted')" title="删除照片">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M3 6h18"/><path d="M19 6v14c0 1-1 2-2 2H7c-1 0-2-1-2-2V6"/><path d="M8 6V4c0-1 1-2 2-2h4c1 0 2 1 2 2v2"/></svg>
          </button>
        </div>
      </div>
      <img :src="photo.thumbnailUrl" class="photo-thumbnail" alt="美食照片" @click="showFullPhoto = true" />
    </div>

    <!-- 上传 / 重新上传 -->
    <UploadPhoto
      v-if="!photo || showUpload"
      :session-id="sessionId"
      @uploaded="onUploaded"
    />

    <!-- 全屏照片预览 -->
    <Transition name="fade">
      <div v-if="showFullPhoto && photo" class="photo-modal" @click.self="showFullPhoto = false">
        <div class="photo-modal-content">
          <img :src="photo.originalUrl" class="full-photo" alt="原始照片" />
          <button class="photo-modal-close" @click="showFullPhoto = false">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M18 6 6 18"/><path d="m6 6 12 12"/></svg>
          </button>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup lang="ts">
// ResultPhotoManager - 照片管理(子组件)
// 父组件传入 sessionId + 当前 photo,子组件负责 UI 状态(显示/上传/全屏)
// API 调用全部由父组件在 photo-uploaded / photo-deleted 处理函数中完成
import { ref } from 'vue'
import UploadPhoto from '@/components/UploadPhoto.vue'
import type { Photo } from '@/types/result'

defineProps<{
  sessionId: string
  photo: Photo | null
}>()

const emit = defineEmits<{
  (e: 'photo-uploaded', photo: Photo): void
  (e: 'photo-deleted'): void
}>()

// 内部 UI 状态
const showUpload = ref(false)
const showFullPhoto = ref(false)

// 包装 emit(原 Result.vue onPhotoUploaded 的 reset 行为由父组件负责)
function onUploaded(data: Photo) {
  showUpload.value = false
  emit('photo-uploaded', data)
}
</script>

<style lang="scss" scoped>
/* Uploaded photo */
.uploaded-photo-card {
  margin-bottom: 20px;
}

.photo-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.photo-actions {
  display: flex;
  gap: 8px;
}

.photo-action-btn {
  width: 32px;
  height: 32px;
  border: none;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;

  &.replace {
    background: var(--focus-ring-color);
    color: var(--color-primary);
    &:active { background: /* ponytail: rgba(74, 141, 213, 0.15) */ rgba(74, 141, 213, 0.15); }
  }

  &.delete {
    background: var(--color-danger-bright);
    color: var(--color-danger-bright);
    &:active { background: var(--color-danger-bright); }
  }
}

.upload-label {
  display: flex;
  align-items: center;
  gap: 8px;
  font-family: var(--font-serif);
  font-style: italic;
  font-size: 16px;
  font-weight: 500;
  color: var(--color-on-surface);
  margin-bottom: 12px;

  svg {
    color: var(--color-primary);
  }
}

.photo-thumbnail {
  width: 100%;
  border-radius: 1.5rem;
  display: block;
  max-height: 300px;
  object-fit: cover;
  box-shadow: var(--shadow-flat-md);
  cursor: pointer;
  transition: transform 0.3s var(--ease-out-soft);

  &:hover {
    transform: scale(1.01);
  }
}

/* Photo modal */
.photo-modal {
  /* overlay 样式已抽到 Result.vue 父组件 :deep(.photo-modal),避免 100 行重复 */
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
}

.photo-modal-close {
  position: absolute;
  top: -12px;
  right: -12px;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  border: none;
  background: var(--color-on-inverse-overlay-md);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: background 0.2s;

  &:hover {
    background: var(--color-on-inverse-overlay-md);
  }
}

/* Transition */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.25s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>

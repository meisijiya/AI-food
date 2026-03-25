<template>
  <div class="upload-photo">
    <div class="upload-label">
      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14.5 4h-5L7 7H4a2 2 0 0 0-2 2v9a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2V9a2 2 0 0 0-2-2h-3l-2.5-3z"/><circle cx="12" cy="13" r="3"/></svg>
      <span>上传美食照片</span>
    </div>

    <!-- Uploaded state -->
    <div v-if="uploadedThumbnailUrl" class="uploaded-card" @click="triggerFileInput">
      <img :src="uploadedThumbnailUrl" class="uploaded-image" alt="已上传" />
      <div class="uploaded-check">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M20 6 9 17l-5-5"/></svg>
      </div>
    </div>

    <!-- Upload area -->
    <template v-else>
      <div
        class="upload-area"
        :class="{ 'has-preview': !!previewUrl }"
        @click="triggerFileInput"
      >
        <div v-if="previewUrl" class="preview-wrapper">
          <img :src="previewUrl" class="preview-image" alt="预览" />
        </div>
        <div v-else class="upload-placeholder">
          <div class="upload-icon">
            <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M14.5 4h-5L7 7H4a2 2 0 0 0-2 2v9a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2V9a2 2 0 0 0-2-2h-3l-2.5-3z"/><circle cx="12" cy="13" r="3"/></svg>
          </div>
          <div class="upload-text">点击选择照片</div>
          <div class="upload-hint">支持拍照或从相册选择</div>
        </div>
      </div>

      <input
        ref="fileInput"
        type="file"
        accept="image/*"
        capture="environment"
        class="file-input"
        @change="onFileSelected"
      />

      <!-- Progress -->
      <div v-if="uploading" class="upload-progress">
        <div class="progress-bar">
          <div class="progress-fill"></div>
        </div>
        <span class="progress-text">{{ compressing ? '压缩中...' : '上传中...' }}</span>
      </div>

      <!-- Upload button -->
      <button
        v-if="selectedFile && !uploading"
        class="upload-btn"
        @click="upload"
      >
        上传照片
      </button>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { uploadApi } from '@/api'
import { showSuccess, showError } from '@/utils/toast'

const props = defineProps<{
  sessionId: string
}>()

const emit = defineEmits<{
  uploaded: [data: { thumbnailUrl: string; originalUrl: string }]
}>()

const fileInput = ref<HTMLInputElement | null>(null)
const selectedFile = ref<File | null>(null)
const previewUrl = ref<string | null>(null)
const uploading = ref(false)
const compressing = ref(false)
const uploadedThumbnailUrl = ref<string | null>(null)

function triggerFileInput() {
  fileInput.value?.click()
}

function onFileSelected(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return

  selectedFile.value = file
  previewUrl.value = URL.createObjectURL(file)
}

async function compressImage(file: File): Promise<File> {
  compressing.value = true
  try {
    const bitmap = await createImageBitmap(file)
    const maxWidth = 1920
    let { width, height } = bitmap

    if (width > maxWidth) {
      const ratio = maxWidth / width
      width = maxWidth
      height = Math.round(height * ratio)
    }

    const canvas = new OffscreenCanvas(width, height)
    const ctx = canvas.getContext('2d')!
    ctx.drawImage(bitmap, 0, 0, width, height)
    bitmap.close()

    let quality = 0.9
    let blob: Blob | null = null

    while (quality >= 0.1) {
      blob = await canvas.convertToBlob({ type: 'image/jpeg', quality })
      if (blob.size < 1024 * 1024) break
      quality -= 0.1
    }

    if (!blob) blob = await canvas.convertToBlob({ type: 'image/jpeg', quality: 0.1 })

    return new File([blob], file.name.replace(/\.[^.]+$/, '.jpg'), { type: 'image/jpeg' })
  } finally {
    compressing.value = false
  }
}

async function upload() {
  if (!selectedFile.value) return

  uploading.value = true
  try {
    const compressed = await compressImage(selectedFile.value)
    const oldPhotoUrl = uploadedThumbnailUrl.value || undefined
    const uploadRes = await uploadApi.uploadPhoto(compressed, props.sessionId, oldPhotoUrl)

    const thumbnailUrl = uploadRes?.thumbnailUrl || uploadRes?.url || uploadRes?.photoUrl
    const originalUrl = uploadRes?.originalUrl || uploadRes?.url || uploadRes?.photoUrl

    if (!thumbnailUrl) throw new Error('上传返回数据异常')

    uploadedThumbnailUrl.value = thumbnailUrl
    emit('uploaded', { thumbnailUrl, originalUrl })
    showSuccess('照片上传成功')
  } catch (err: any) {
    showError(err.message || '照片上传失败')
  } finally {
    uploading.value = false
  }
}
</script>

<style lang="scss" scoped>
.upload-photo {
  margin-top: 20px;
  z-index: 1;
  position: relative;
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

.upload-area {
  border: 2px dashed var(--color-surface-container-low);
  border-radius: 1.5rem;
  padding: 32px 20px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.22, 1, 0.36, 1);
  background: var(--color-surface-container-lowest);
  position: relative;
  overflow: hidden;

  &:hover {
    border-color: var(--color-primary);
    background: rgba(0, 89, 182, 0.03);
  }

  &:active {
    transform: scale(0.99);
  }

  &.has-preview {
    padding: 8px;
    border-style: solid;
    border-color: var(--color-surface-container-low);
  }
}

.upload-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
}

.upload-icon {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: var(--color-surface-container-low);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-on-surface-variant);
}

.upload-text {
  font-family: var(--font-sans);
  font-size: 14px;
  font-weight: 600;
  color: var(--color-on-surface);
}

.upload-hint {
  font-size: 12px;
  color: var(--color-on-surface-variant);
}

.preview-wrapper {
  border-radius: 1.25rem;
  overflow: hidden;
}

.preview-image {
  width: 100%;
  display: block;
  max-height: 300px;
  object-fit: cover;
}

.file-input {
  display: none;
}

.upload-progress {
  margin-top: 12px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.progress-bar {
  flex: 1;
  height: 4px;
  background: var(--color-surface-container-low);
  border-radius: 100px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: var(--color-primary);
  border-radius: 100px;
  animation: progress-indeterminate 1.5s ease-in-out infinite;
}

@keyframes progress-indeterminate {
  0% { width: 0; margin-left: 0; }
  50% { width: 60%; margin-left: 20%; }
  100% { width: 0; margin-left: 100%; }
}

.progress-text {
  font-size: 12px;
  color: var(--color-on-surface-variant);
  white-space: nowrap;
}

.upload-btn {
  width: 100%;
  margin-top: 12px;
  padding: 14px;
  border: none;
  border-radius: 2rem;
  background: linear-gradient(135deg, var(--color-primary-container), var(--color-primary));
  color: white;
  font-family: var(--font-sans);
  font-size: 14px;
  font-weight: 700;
  letter-spacing: 0.05em;
  cursor: pointer;
  box-shadow: 0 8px 24px -6px rgba(0, 89, 182, 0.3);
  transition: all 0.3s cubic-bezier(0.22, 1, 0.36, 1);

  &:hover {
    transform: translateY(-1px);
    box-shadow: 0 12px 32px -6px rgba(0, 89, 182, 0.4);
  }

  &:active {
    transform: translateY(0);
  }
}

.uploaded-card {
  position: relative;
  border-radius: 1.5rem;
  overflow: hidden;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  cursor: pointer;
  transition: transform 0.2s;

  &:active {
    transform: scale(0.98);
  }
}

.uploaded-image {
  width: 100%;
  display: block;
  max-height: 300px;
  object-fit: cover;
}

.uploaded-check {
  position: absolute;
  top: 12px;
  right: 12px;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: rgba(34, 197, 94, 0.9);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 12px rgba(34, 197, 94, 0.3);
}
</style>

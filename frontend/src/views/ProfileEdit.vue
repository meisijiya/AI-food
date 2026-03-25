<template>
  <div class="profile-edit-container">
    <div class="bg-glow bg-glow-1"></div>
    <div class="bg-glow bg-glow-2"></div>

    <!-- Header -->
    <div class="page-header animate-fade-up">
      <button class="back-btn" @click="router.back()">
        <van-icon name="arrow-left" size="20" />
      </button>
      <h1 class="page-title"><em>编辑资料</em></h1>
    </div>

    <!-- Avatar section -->
    <div class="section-card animate-fade-up delay-100 animate-start-hidden">
      <div class="section-title">头像</div>
      <div class="avatar-edit" @click="handleAvatarClick">
        <div class="avatar-circle-large">
          <span v-if="!currentAvatar">{{ avatarInitial }}</span>
          <img v-else :src="currentAvatar" alt="avatar" />
        </div>
        <div class="avatar-hint">点击修改</div>
      </div>
      <input ref="avatarInput" type="file" accept="image/*" style="display: none" @change="handleAvatarUpload" />
    </div>

    <!-- Nickname section -->
    <div class="section-card animate-fade-up delay-200 animate-start-hidden">
      <div class="section-title">昵称</div>
      <div class="field-row">
        <input
          ref="nicknameInput"
          v-model="nickname"
          class="field-input"
          placeholder="请输入昵称"
          maxlength="20"
          :disabled="!editingNickname"
        />
        <button v-if="!editingNickname" class="edit-btn" @click="startEditNickname">修改</button>
        <div v-else class="edit-actions">
          <button class="save-btn" @click="saveNickname" :disabled="savingNickname">{{ savingNickname ? '保存中' : '保存' }}</button>
          <button class="cancel-btn" @click="cancelEditNickname">取消</button>
        </div>
      </div>
    </div>

    <!-- Password section -->
    <div class="section-card animate-fade-up delay-300 animate-start-hidden">
      <div class="section-title">修改密码</div>
      <div class="password-form">
        <div class="field-group">
          <input
            v-model="passwordForm.oldPassword"
            type="password"
            class="field-input"
            placeholder="当前密码"
          />
        </div>
        <div class="field-group">
          <input
            v-model="passwordForm.newPassword"
            type="password"
            class="field-input"
            placeholder="新密码（至少6位）"
          />
        </div>
        <div class="field-group">
          <input
            v-model="passwordForm.confirmPassword"
            type="password"
            class="field-input"
            placeholder="确认新密码"
          />
        </div>
        <button
          class="submit-btn"
          :disabled="savingPassword || !canSubmitPassword"
          @click="savePassword"
        >
          {{ savingPassword ? '修改中...' : '确认修改' }}
        </button>
      </div>
    </div>

    <div class="nav-spacer"></div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { userApi } from '@/api'
import { showSuccess, showError } from '@/utils/toast'

const router = useRouter()
const authStore = useAuthStore()

const userInfo = computed(() => authStore.userInfo)
const avatarInitial = computed(() => {
  const name = userInfo.value?.nickname || userInfo.value?.username || '?'
  return name.charAt(0).toUpperCase()
})

const currentAvatar = ref(userInfo.value?.avatar || '')
const avatarInput = ref<HTMLInputElement | null>(null)

// Nickname
const nickname = ref(userInfo.value?.nickname || '')
const editingNickname = ref(false)
const savingNickname = ref(false)
const originalNickname = ref('')

// Password
const passwordForm = ref({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})
const savingPassword = ref(false)

const canSubmitPassword = computed(() => {
  return passwordForm.value.oldPassword &&
    passwordForm.value.newPassword.length >= 6 &&
    passwordForm.value.newPassword === passwordForm.value.confirmPassword
})

function handleAvatarClick() {
  avatarInput.value?.click()
}

async function compressImage(file: File): Promise<File> {
  const bitmap = await createImageBitmap(file)
  const maxWidth = 1200
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
}

async function handleAvatarUpload(e: Event) {
  const file = (e.target as HTMLInputElement).files?.[0]
  if (!file) return

  // 立即本地回显
  const localUrl = URL.createObjectURL(file)
  const oldAvatar = currentAvatar.value
  currentAvatar.value = localUrl

  try {
    const compressed = await compressImage(file)
    const url = await userApi.uploadAvatar(compressed)
    currentAvatar.value = url
    authStore.setUserInfo({
      ...userInfo.value!,
      avatar: url
    })
    showSuccess('头像更新成功')
  } catch (err: any) {
    currentAvatar.value = oldAvatar
    showError(err?.message || '头像上传失败')
  } finally {
    URL.revokeObjectURL(localUrl)
    if (avatarInput.value) avatarInput.value.value = ''
  }
}

function startEditNickname() {
  originalNickname.value = nickname.value
  editingNickname.value = true
  nextTick(() => {
    (document.querySelector('.field-input:not([type=password])') as HTMLInputElement)?.focus()
  })
}

async function saveNickname() {
  const trimmed = nickname.value.trim()
  if (!trimmed) {
    showError('昵称不能为空')
    return
  }
  if (trimmed === originalNickname.value) {
    editingNickname.value = false
    return
  }

  savingNickname.value = true
  try {
    await userApi.updateNickname(trimmed)
    authStore.setUserInfo({
      ...userInfo.value!,
      nickname: trimmed
    })
    editingNickname.value = false
    showSuccess('昵称更新成功')
  } catch (err: any) {
    showError(err?.message || '昵称更新失败')
  } finally {
    savingNickname.value = false
  }
}

function cancelEditNickname() {
  nickname.value = originalNickname.value
  editingNickname.value = false
}

async function savePassword() {
  if (!canSubmitPassword.value) return

  savingPassword.value = true
  try {
    await userApi.updatePassword({
      oldPassword: passwordForm.value.oldPassword,
      newPassword: passwordForm.value.newPassword
    })
    passwordForm.value = { oldPassword: '', newPassword: '', confirmPassword: '' }
    showSuccess('密码修改成功，请重新登录')
    authStore.logout()
    router.push('/login')
  } catch (err: any) {
    showError(err?.message || '密码修改失败')
  } finally {
    savingPassword.value = false
  }
}
</script>

<style lang="scss" scoped>
.profile-edit-container {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: var(--color-surface);
  padding: 0 20px 100px;
  position: relative;
  overflow: hidden;
}

.bg-glow {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  pointer-events: none;
  animation: sanctuary-glow-pulse 6s ease-in-out infinite;
}

.bg-glow-1 {
  top: -60px;
  right: -40px;
  width: 240px;
  height: 240px;
  background: var(--color-primary-container);
  opacity: 0.12;
}

.bg-glow-2 {
  bottom: 160px;
  left: -60px;
  width: 200px;
  height: 200px;
  background: var(--color-secondary-fixed);
  opacity: 0.08;
  animation-delay: 3s;
}

.page-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 0 24px;
  z-index: 1;
}

.back-btn {
  background: none;
  border: none;
  color: var(--color-on-surface);
  cursor: pointer;
  padding: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  transition: background 0.2s;

  &:active {
    background: var(--color-surface-container-lowest);
  }
}

.page-title {
  font-family: var(--font-serif);
  font-style: italic;
  font-size: 24px;
  font-weight: 400;
  color: var(--color-on-surface);

  em {
    font-style: italic;
    color: var(--color-primary);
  }
}

.section-card {
  background: var(--color-surface-container-lowest);
  border-radius: 2rem;
  border: 1px solid rgba(255, 255, 255, 0.8);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  padding: 24px;
  margin-bottom: 16px;
  z-index: 1;
}

.section-title {
  font-size: 13px;
  font-weight: 700;
  color: var(--color-on-surface-variant);
  margin-bottom: 16px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.avatar-edit {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  cursor: pointer;
}

.avatar-circle-large {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--color-primary-container), var(--color-primary));
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-family: var(--font-serif);
  font-style: italic;
  font-size: 32px;
  font-weight: 400;
  overflow: hidden;
  transition: transform 0.2s;

  &:active {
    transform: scale(0.95);
  }

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}

.avatar-hint {
  font-size: 12px;
  color: var(--color-on-surface-variant);
}

.field-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.field-input {
  flex: 1;
  padding: 10px 14px;
  border: 1.5px solid var(--color-surface-container-low);
  border-radius: 12px;
  background: transparent;
  color: var(--color-on-surface);
  font-size: 15px;
  font-family: var(--font-sans);
  outline: none;
  transition: border-color 0.2s;

  &:focus {
    border-color: var(--color-primary);
  }

  &:disabled {
    opacity: 0.7;
    cursor: default;
  }
}

.field-group {
  margin-bottom: 12px;

  &:last-of-type {
    margin-bottom: 16px;
  }
}

.edit-btn {
  flex-shrink: 0;
  padding: 8px 16px;
  border: 1.5px solid var(--color-primary);
  border-radius: 100px;
  background: transparent;
  color: var(--color-primary);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;

  &:active {
    background: var(--color-primary);
    color: white;
  }
}

.edit-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

.save-btn {
  padding: 8px 16px;
  border: none;
  border-radius: 100px;
  background: var(--color-primary);
  color: white;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: opacity 0.2s;

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }

  &:active:not(:disabled) {
    opacity: 0.9;
  }
}

.cancel-btn {
  padding: 8px 16px;
  border: 1.5px solid var(--color-surface-container-low);
  border-radius: 100px;
  background: transparent;
  color: var(--color-on-surface-variant);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;

  &:active {
    border-color: var(--color-on-surface-variant);
  }
}

.password-form {
  display: flex;
  flex-direction: column;
}

.submit-btn {
  padding: 12px;
  border: none;
  border-radius: 100px;
  background: var(--color-primary);
  color: white;
  font-family: var(--font-sans);
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s;

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }

  &:active:not(:disabled) {
    transform: scale(0.98);
  }
}

.nav-spacer {
  height: 80px;
}
</style>

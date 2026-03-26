<template>
  <div class="login-container">
    <!-- Decorative background glows -->
    <div class="bg-glow bg-glow-1"></div>
    <div class="bg-glow bg-glow-2"></div>
    <div class="bg-glow bg-glow-3"></div>

    <!-- Decorative badge -->
    <div class="hero-badge animate-fade-up animate-start-hidden">
      <span>AI 美食</span>
    </div>

    <!-- Hero title -->
    <h1 class="hero-title animate-fade-up delay-100 animate-start-hidden">
      <em>{{ isLogin ? '欢迎回来' : '创建账号' }}</em>
    </h1>
    <p class="hero-subtitle animate-fade-up delay-200 animate-start-hidden">
      {{ isLogin ? '登录后开启你的美食之旅' : '注册一个新账号，开始探索美食' }}
    </p>

    <!-- Tab switch -->
    <div class="tab-switch animate-fade-up delay-300 animate-start-hidden">
      <button
        :class="['tab-btn', { active: isLogin }]"
        @click="switchTab(true)"
      >
        登录
      </button>
      <button
        :class="['tab-btn', { active: !isLogin }]"
        @click="switchTab(false)"
      >
        注册
      </button>
      <div class="tab-indicator" :style="{ transform: isLogin ? 'translateX(0)' : 'translateX(100%)' }"></div>
    </div>

    <!-- Login form -->
    <form v-if="isLogin" class="form-area animate-fade-up delay-400 animate-start-hidden" @submit.prevent="handleLogin">
      <div class="input-group">
        <div class="input-pill">
          <svg class="input-icon" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect width="20" height="16" x="2" y="4" rx="2"/><path d="m22 7-8.97 5.7a1.94 1.94 0 0 1-2.06 0L2 7"/></svg>
          <input
            v-model="loginForm.email"
            type="email"
            placeholder="邮箱地址"
            autocomplete="email"
            required
          />
        </div>
      </div>
      <div class="input-group">
        <div class="input-pill">
          <svg class="input-icon" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect width="18" height="11" x="3" y="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg>
          <input
            v-model="loginForm.password"
            type="password"
            placeholder="密码"
            autocomplete="current-password"
            required
          />
        </div>
      </div>
      <button type="submit" class="cta-button" :disabled="loading">
        <span v-if="loading" class="cta-loading"></span>
        <span v-else>登 录</span>
      </button>
    </form>

    <!-- Register form -->
    <form v-else class="form-area animate-fade-up delay-400 animate-start-hidden" @submit.prevent="handleRegister">
      <div class="input-group">
        <div class="input-pill">
          <svg class="input-icon" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect width="20" height="16" x="2" y="4" rx="2"/><path d="m22 7-8.97 5.7a1.94 1.94 0 0 1-2.06 0L2 7"/></svg>
          <input
            v-model="registerForm.email"
            type="email"
            placeholder="邮箱地址"
            autocomplete="email"
            required
          />
        </div>
      </div>
      <div class="input-group">
        <div class="input-pill input-pill-code">
          <svg class="input-icon" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>
          <input
            v-model="registerForm.code"
            type="text"
            placeholder="验证码"
            maxlength="6"
            required
          />
          <button
            type="button"
            class="code-btn"
            :disabled="countdown > 0 || !registerForm.email || codeSending"
            @click="handleSendCode"
          >
            {{ countdown > 0 ? `${countdown}s` : '获取验证码' }}
          </button>
        </div>
      </div>
      <div class="input-group">
        <div class="input-pill">
          <svg class="input-icon" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect width="18" height="11" x="3" y="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg>
          <input
            v-model="registerForm.password"
            type="password"
            placeholder="设置密码"
            autocomplete="new-password"
            required
          />
        </div>
      </div>
      <button type="submit" class="cta-button" :disabled="loading">
        <span v-if="loading" class="cta-loading"></span>
        <span v-else>注 册</span>
      </button>
    </form>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { authApi } from '@/api'
import { useAuthStore } from '@/stores/auth'
import { showSuccess, showError } from '@/utils/toast'

const router = useRouter()
const authStore = useAuthStore()

// 页面加载时清理可能的脏数据
onMounted(() => {
  authStore.clearStale()
})

const isLogin = ref(true)
const loading = ref(false)
const codeSending = ref(false)
const countdown = ref(0)
let countdownTimer: ReturnType<typeof setInterval> | null = null

const loginForm = reactive({
  email: '',
  password: ''
})

const registerForm = reactive({
  email: '',
  code: '',
  password: ''
})

const switchTab = (login: boolean) => {
  isLogin.value = login
}

const startCountdown = () => {
  countdown.value = 60
  countdownTimer = setInterval(() => {
    countdown.value--
    if (countdown.value <= 0 && countdownTimer) {
      clearInterval(countdownTimer)
      countdownTimer = null
    }
  }, 1000)
}

const handleSendCode = async () => {
  if (!registerForm.email) return
  codeSending.value = true
  try {
    await authApi.sendCode(registerForm.email)
    showSuccess('验证码已发送')
    startCountdown()
  } catch (error: any) {
    showError(error?.response?.data?.message || '发送失败，请重试')
  } finally {
    codeSending.value = false
  }
}

const handleLogin = async () => {
  if (!loginForm.email || !loginForm.password) return
  loading.value = true
  try {
    const res = await authApi.login({
      email: loginForm.email,
      password: loginForm.password
    })
    // res 已被 api 拦截器解包为 LoginResponse: { token, userId, username, nickname, email, avatar }
    authStore.setToken(res.token)
    authStore.setUserInfo({
      userId: res.userId,
      username: res.username,
      nickname: res.nickname || res.username,
      email: res.email,
      avatar: res.avatar
    })
    showSuccess('登录成功')
    router.push('/')
  } catch (error: any) {
    showError(error?.response?.data?.message || '登录失败，请检查邮箱和密码')
  } finally {
    loading.value = false
  }
}

const handleRegister = async () => {
  if (!registerForm.email || !registerForm.code || !registerForm.password) return
  loading.value = true
  try {
    const res = await authApi.register({
      password: registerForm.password,
      email: registerForm.email,
      code: registerForm.code
    })
    // res 已被 api 拦截器解包为 LoginResponse
    authStore.setToken(res.token)
    authStore.setUserInfo({
      userId: res.userId,
      username: res.username,
      nickname: res.nickname || res.username,
      email: res.email,
      avatar: res.avatar
    })
    showSuccess('注册成功')
    router.push('/')
  } catch (error: any) {
    showError(error?.response?.data?.message || '注册失败，请重试')
  } finally {
    loading.value = false
  }
}
</script>

<style lang="scss" scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  background-color: var(--color-surface);
  padding: 48px 24px 60px;
  position: relative;
  overflow: hidden;
}

/* Background decorative glows */
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
  width: 260px;
  height: 260px;
  background: var(--color-primary-container);
  opacity: 0.15;
}

.bg-glow-2 {
  bottom: 120px;
  left: -70px;
  width: 220px;
  height: 220px;
  background: var(--color-primary);
  opacity: 0.08;
  animation-delay: 3s;
}

.bg-glow-3 {
  top: 40%;
  right: -50px;
  width: 180px;
  height: 180px;
  background: var(--color-primary-container);
  opacity: 0.1;
  animation-delay: 1.5s;
}

/* Badge */
.hero-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 16px;
  background: var(--color-surface-container-low);
  border-radius: 100px;
  margin-bottom: 24px;
  border: 1px solid var(--color-surface-container-lowest);
  z-index: 1;

  span {
    font-size: 10px;
    font-weight: 700;
    text-transform: uppercase;
    letter-spacing: 0.2em;
    color: var(--color-primary);
  }
}

/* Hero title */
.hero-title {
  font-family: var(--font-serif);
  font-style: italic;
  font-size: 36px;
  font-weight: 400;
  line-height: 1.2;
  color: var(--color-on-surface);
  margin-bottom: 10px;
  text-align: center;
  z-index: 1;

  em {
    font-style: italic;
    color: var(--color-primary);
  }
}

.hero-subtitle {
  font-size: 14px;
  color: var(--color-on-surface-variant);
  margin-bottom: 32px;
  text-align: center;
  z-index: 1;
}

/* Tab switch */
.tab-switch {
  position: relative;
  display: flex;
  background: var(--color-surface-container-lowest);
  border-radius: 2rem;
  padding: 4px;
  margin-bottom: 28px;
  width: 100%;
  max-width: 360px;
  border: 1px solid var(--color-surface-container-low);
  z-index: 1;
}

.tab-btn {
  flex: 1;
  position: relative;
  z-index: 1;
  padding: 12px 0;
  border: none;
  background: none;
  font-family: var(--font-sans);
  font-size: 14px;
  font-weight: 600;
  color: var(--color-on-surface-variant);
  cursor: pointer;
  transition: color 0.3s;

  &.active {
    color: var(--color-on-surface);
  }
}

.tab-indicator {
  position: absolute;
  top: 4px;
  left: 4px;
  width: calc(50% - 4px);
  height: calc(100% - 8px);
  background: var(--color-surface-container-low);
  border-radius: 1.75rem;
  transition: transform 0.35s cubic-bezier(0.22, 1, 0.36, 1);
  z-index: 0;
}

/* Form */
.form-area {
  width: 100%;
  max-width: 360px;
  display: flex;
  flex-direction: column;
  gap: 14px;
  z-index: 1;
}

.input-group {
  width: 100%;
}

.input-pill {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 0 20px;
  height: 52px;
  background: var(--color-surface-container-lowest);
  border: 1.5px solid var(--color-surface-container-low);
  border-radius: 2rem;
  transition: border-color 0.2s, box-shadow 0.2s;

  &:focus-within {
    border-color: var(--color-primary-container);
    box-shadow: 0 0 0 3px rgba(0, 89, 182, 0.08);
  }

  &.input-pill-code {
    padding-right: 6px;
  }
}

.input-icon {
  flex-shrink: 0;
  color: var(--color-on-surface-variant);
  opacity: 0.5;
}

.input-pill input {
  flex: 1;
  border: none;
  background: none;
  font-family: var(--font-sans);
  font-size: 14px;
  color: var(--color-on-surface);
  outline: none;
  min-width: 0;

  &::placeholder {
    color: var(--color-on-surface-variant);
    opacity: 0.5;
  }
}

.code-btn {
  flex-shrink: 0;
  padding: 8px 16px;
  border: none;
  border-radius: 1.5rem;
  background: var(--color-primary-container);
  color: var(--color-primary);
  font-family: var(--font-sans);
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;

  &:hover:not(:disabled) {
    opacity: 0.85;
  }

  &:active:not(:disabled) {
    transform: scale(0.96);
  }

  &:disabled {
    opacity: 0.4;
    cursor: not-allowed;
  }
}

/* CTA */
.cta-button {
  width: 100%;
  padding: 16px;
  margin-top: 8px;
  border: none;
  border-radius: 2rem;
  background: linear-gradient(135deg, var(--color-primary-container), var(--color-primary));
  color: white;
  font-family: var(--font-sans);
  font-size: 15px;
  font-weight: 700;
  letter-spacing: 0.15em;
  cursor: pointer;
  box-shadow: 0 12px 32px -8px rgba(0, 89, 182, 0.35);
  transition: all 0.3s cubic-bezier(0.22, 1, 0.36, 1);
  position: relative;
  overflow: hidden;

  &:hover:not(:disabled) {
    transform: translateY(-2px);
    box-shadow: 0 16px 40px -8px rgba(0, 89, 182, 0.45);
  }

  &:active:not(:disabled) {
    transform: translateY(0);
  }

  &:disabled {
    opacity: 0.7;
    cursor: not-allowed;
  }
}

.cta-loading {
  display: inline-block;
  width: 20px;
  height: 20px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: white;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

@media (min-width: 640px) {
  .login-container {
    padding: 80px 40px 80px;
    justify-content: center;
  }

  .hero-title {
    font-size: 44px;
  }

  .form-area {
    max-width: 400px;
  }

  .input-pill {
    height: 56px;
  }
}
</style>

<template>
  <div class="profile-container">
    <div class="bg-glow bg-glow-1"></div>
    <div class="bg-glow bg-glow-2"></div>

    <!-- Header -->
    <h1 class="page-title animate-fade-up">
      <em>个人中心</em>
    </h1>

    <!-- Guest Welcome Page -->
    <div v-if="isGuest" class="guest-welcome animate-fade-up delay-100">
      <div class="guest-icon">
        <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
          <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
          <circle cx="12" cy="7" r="4" />
        </svg>
      </div>
      <h2 class="guest-title">欢迎来到 AI Food</h2>
      <p class="guest-desc">登录后享受完整的个性化服务</p>
      <div class="guest-features">
        <div class="guest-feature-item">
          <span class="feature-check">✓</span>
          <span>AI智能美食推荐</span>
        </div>
        <div class="guest-feature-item">
          <span class="feature-check">✓</span>
          <span>拍照打卡记录</span>
        </div>
        <div class="guest-feature-item">
          <span class="feature-check">✓</span>
          <span>社区互动分享</span>
        </div>
      </div>
      <button class="guest-login-btn" @click="router.push('/login')">
        立即登录
      </button>
      <button class="guest-back-btn" @click="router.push('/')">
        返回首页
      </button>
    </div>

    <!-- Logged In User Content -->
    <div v-else class="bento-grid">
      <!-- User info card -->
      <div class="user-card bento-full animate-fade-up delay-100 animate-start-hidden" @click="router.push('/profile-edit')">
        <div class="avatar-circle">
          <span v-if="!userInfo?.avatar">{{ avatarInitial }}</span>
          <img v-else :src="userInfo.avatar" alt="avatar" />
        </div>
        <div class="user-info">
          <div class="nickname">{{ userInfo?.nickname || '未设置昵称' }}</div>
          <div class="username-id">ID: {{ userInfo?.username || '' }}</div>
        </div>
      </div>

      <!-- Sign-in card -->
      <div class="signin-card animate-fade-up delay-200 animate-start-hidden">
        <div class="signin-header">
          <span class="signin-title">本月签到</span>
          <span class="signin-count">{{ signStatus.totalDays }} 天</span>
        </div>

        <!-- Calendar grid -->
        <div class="calendar-grid">
          <div
            v-for="day in daysInMonth"
            :key="day"
            class="calendar-day"
            :class="{ signed: signedDaysSet.has(day), today: day === today }"
          >
            <div v-if="signedDaysSet.has(day)" class="signed-circle"></div>
            <span v-else class="day-num">{{ day }}</span>
          </div>
        </div>

        <div class="signin-footer">
          <span class="continuous">连续 {{ signStatus.continuousDays }} 天</span>
          <button
            class="signin-btn"
            :disabled="signStatus.todaySigned || signingIn"
            @click="handleSignIn"
          >
            {{ signStatus.todaySigned ? '已签到' : '签到' }}
          </button>
        </div>
      </div>

      <!-- Stats -->
      <div class="stats-card animate-fade-up delay-300 animate-start-hidden">
        <div class="stat-item" @click="router.push('/follow?type=following')">
          <div class="stat-value">{{ followStats.followingCount }}</div>
          <div class="stat-label">关注</div>
        </div>
        <div class="stat-item" @click="router.push('/follow?type=followers')">
          <div class="stat-value">{{ followStats.followerCount }}</div>
          <div class="stat-label">粉丝</div>
        </div>
        <div class="stat-item">
          <div class="stat-value">{{ conversationCount }}</div>
          <div class="stat-label">推荐记录</div>
        </div>
      </div>

      <!-- Logout -->
      <button class="logout-btn animate-fade-up delay-400 animate-start-hidden" @click="handleLogout">
        退出登录
      </button>
    </div>

    <div class="nav-spacer"></div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { userApi, recordApi, followApi } from '@/api'
import { showSuccess, showError } from '@/utils/toast'

const router = useRouter()
const authStore = useAuthStore()

const userInfo = computed(() => authStore.userInfo)
const isGuest = computed(() => authStore.isGuest)
const avatarInitial = computed(() => {
  const name = userInfo.value?.nickname || userInfo.value?.username || '?'
  return name.charAt(0).toUpperCase()
})

const signingIn = ref(false)
const conversationCount = ref(0)
const followStats = reactive({
  followingCount: 0,
  followerCount: 0
})
const signStatus = reactive({
  totalDays: 0,
  continuousDays: 0,
  todaySigned: false,
  signedDays: [] as number[]
})
const signedDaysSet = computed(() => new Set(signStatus.signedDays))

const now = new Date()
const daysInMonth = new Date(now.getFullYear(), now.getMonth() + 1, 0).getDate()
const today = now.getDate()

async function fetchData() {
  try {
    const [userRes, signRes] = await Promise.all([
      userApi.getUserInfo(),
      userApi.getSignStatus()
    ])
    // userRes 已被拦截器解包为 SysUser: { id, username, nickname, email, avatar }
    if (userRes) {
      authStore.setUserInfo({
        userId: userRes.id,
        username: userRes.username,
        nickname: userRes.nickname || userRes.username,
        email: userRes.email,
        avatar: userRes.avatar
      })
    }
    // signRes 已被解包为 { monthTotal, todaySigned, continuousDays, signedDays }
    if (signRes) {
      signStatus.totalDays = signRes.monthTotal || 0
      signStatus.continuousDays = signRes.continuousDays || 0
      signStatus.todaySigned = signRes.todaySigned || false
      signStatus.signedDays = signRes.signedDays || []
    }
  } catch {
    // ignore
  }

  try {
    const [listRes, statsRes] = await Promise.all([
      recordApi.getRecordList({ page: 0, size: 1 }),
      followApi.getMyFollowStats()
    ])
    conversationCount.value = listRes?.total || 0
    if (statsRes) {
      followStats.followingCount = statsRes.followingCount || 0
      followStats.followerCount = statsRes.followerCount || 0
    }
  } catch {
    // ignore
  }
}

async function handleSignIn() {
  signingIn.value = true
  try {
    const res = await userApi.signIn()
    if (res) {
      signStatus.todaySigned = true
      signStatus.totalDays = res.totalDays || signStatus.totalDays + 1
      signStatus.continuousDays = res.continuousDays || signStatus.continuousDays + 1
      if (!signStatus.signedDays.includes(today)) {
        signStatus.signedDays.push(today)
      }
      showSuccess('签到成功')
    }
  } catch (error: any) {
    showError(error?.response?.data?.message || '签到失败')
  } finally {
    signingIn.value = false
  }
}

function handleLogout() {
  authStore.logout()
  router.push('/login')
}

onMounted(() => {
  if (isGuest.value) return
  fetchData()
})
</script>

<style lang="scss" scoped>
.profile-container {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: var(--color-surface);
  padding: 40px 20px 100px;
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

.page-title {
  font-family: var(--font-serif);
  font-style: italic;
  font-size: 32px;
  font-weight: 400;
  color: var(--color-on-surface);
  margin-bottom: 24px;
  z-index: 1;

  em {
    font-style: italic;
    color: var(--color-primary);
  }
}

/* User card */
.user-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 24px;
  background: var(--color-surface-container-lowest);
  border-radius: 2rem;
  border: 1px solid rgba(255, 255, 255, 0.8);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  z-index: 1;
  cursor: pointer;
  transition: transform 0.2s;

  &:active {
    transform: scale(0.98);
  }
}

.avatar-circle {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--color-primary-container), var(--color-primary));
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-family: var(--font-serif);
  font-style: italic;
  font-size: 22px;
  font-weight: 400;
  flex-shrink: 0;
  overflow: hidden;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}

.user-info {
  flex: 1;
  min-width: 0;
}

.nickname {
  font-size: 16px;
  font-weight: 700;
  color: var(--color-on-surface);
  margin-bottom: 4px;
}

.email {
  font-size: 12px;
  color: var(--color-on-surface-variant);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.username-id {
  font-size: 12px;
  color: var(--color-on-surface-variant);
  opacity: 0.6;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* Sign-in card */
.signin-card {
  background: var(--color-inverse-surface);
  border-radius: 2rem;
  padding: 24px;
  z-index: 1;
}

.signin-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.signin-title {
  font-size: 14px;
  font-weight: 700;
  color: rgba(255, 255, 255, 0.7);
}

.signin-count {
  font-family: var(--font-serif);
  font-style: italic;
  font-size: 20px;
  color: #22d3ee;
}

.calendar-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 8px;
  margin-bottom: 16px;
}

.calendar-day {
  aspect-ratio: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  position: relative;

  .day-num {
    font-size: 11px;
    color: rgba(255, 255, 255, 0.2);
    font-weight: 500;
  }

  &.signed {
    background: rgba(255, 255, 255, 0.06);
    border-radius: 50%;

    .signed-circle {
      width: 22px;
      height: 22px;
      border-radius: 50%;
      background: var(--color-primary);
      box-shadow: 0 2px 8px rgba(0, 89, 182, 0.4);
    }
  }

  &.today {
    outline: 1.5px solid rgba(34, 211, 238, 0.5);
  }
}

.signin-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.continuous {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.5);
}

.signin-btn {
  padding: 8px 24px;
  border: none;
  border-radius: 100px;
  background: #22d3ee;
  color: #1e1e1e;
  font-family: var(--font-sans);
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s;

  &:hover:not(:disabled) {
    opacity: 0.9;
  }

  &:active:not(:disabled) {
    transform: scale(0.95);
  }

  &:disabled {
    background: rgba(255, 255, 255, 0.15);
    color: rgba(255, 255, 255, 0.4);
    cursor: not-allowed;
  }
}

/* Stats card */
.stats-card {
  display: flex;
  padding: 24px;
  background: var(--color-surface-container-lowest);
  border-radius: 2rem;
  border: 1px solid rgba(255, 255, 255, 0.8);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  z-index: 1;
}

.stat-item {
  text-align: center;
  flex: 1;
}

.stat-value {
  font-family: var(--font-serif);
  font-style: italic;
  font-size: 28px;
  color: var(--color-primary);
  margin-bottom: 4px;
}

.stat-label {
  font-size: 12px;
  color: var(--color-on-surface-variant);
}

/* Logout */
.logout-btn {
  padding: 14px;
  border: 1.5px solid var(--color-surface-container-low);
  border-radius: 2rem;
  background: none;
  color: var(--color-on-surface-variant);
  font-family: var(--font-sans);
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  z-index: 1;

  &:hover {
    border-color: var(--color-primary);
    color: var(--color-primary);
  }

  &:active {
    transform: scale(0.98);
  }
}

/* Guest Welcome */
.guest-welcome {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 40px 24px;
  background: var(--color-surface-container-lowest);
  border-radius: 2rem;
  border: 1px solid rgba(255, 255, 255, 0.8);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  z-index: 1;
}

.guest-icon {
  width: 96px;
  height: 96px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--color-primary-container), var(--color-primary));
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  margin-bottom: 24px;
}

.guest-title {
  font-family: var(--font-serif);
  font-style: italic;
  font-size: 24px;
  font-weight: 400;
  color: var(--color-on-surface);
  margin-bottom: 8px;
}

.guest-desc {
  font-size: 14px;
  color: var(--color-on-surface-variant);
  margin-bottom: 32px;
}

.guest-features {
  display: flex;
  flex-direction: column;
  gap: 12px;
  width: 100%;
  margin-bottom: 32px;
}

.guest-feature-item {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 14px;
  color: var(--color-on-surface);
}

.feature-check {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--color-primary-container), var(--color-primary));
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
}

.guest-login-btn {
  width: 100%;
  padding: 16px;
  border: none;
  border-radius: 2rem;
  background: linear-gradient(135deg, var(--color-primary-container), var(--color-primary));
  color: white;
  font-family: var(--font-sans);
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
  box-shadow: 0 12px 32px -8px rgba(0, 89, 182, 0.35);
  transition: all 0.3s cubic-bezier(0.22, 1, 0.36, 1);
  margin-bottom: 12px;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 16px 40px -8px rgba(0, 89, 182, 0.45);
  }

  &:active {
    transform: translateY(0);
  }
}

.guest-back-btn {
  width: 100%;
  padding: 16px;
  border: 1.5px solid var(--color-surface-container-low);
  border-radius: 2rem;
  background: none;
  color: var(--color-on-surface-variant);
  font-family: var(--font-sans);
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    background: var(--color-surface-container-low);
  }

  &:active {
    transform: scale(0.98);
  }
}

.nav-spacer {
  height: 80px;
}

/* Bento grid wrapper — mobile: flex column, desktop: CSS masonry columns */
.bento-grid {
  display: flex;
  flex-direction: column;
  gap: 16px;
  z-index: 1;
}

.bento-full {
  /* mobile: no effect, desktop: column-span all */
}

@media (min-width: 1024px) {
  .profile-container {
    max-width: 60%;
    margin: 0 auto;
  }
  .bento-grid {
    display: block;
    column-count: 2;
    column-gap: 16px;
  }
  .bento-full {
    column-span: all;
    margin-bottom: 16px;
  }
  .signin-card,
  .stats-card,
  .logout-btn {
    break-inside: avoid;
    margin-bottom: 16px;
  }
  /* Compact sign-in calendar on desktop */
  .signin-card {
    padding: 12px;
  }
  .signin-count {
    font-size: 14px;
  }
  .calendar-grid {
    gap: 3px;
    margin-bottom: 10px;
  }
  .calendar-day .day-num {
    font-size: 9px;
  }
  .calendar-day.signed .signed-circle {
    width: 14px;
    height: 14px;
  }
  .signin-btn {
    padding: 6px 16px;
    font-size: 12px;
  }
  .continuous {
    font-size: 11px;
  }
}
</style>

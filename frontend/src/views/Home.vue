<template>
  <div class="home-container">
    <!-- Decorative background glows -->
    <div class="bg-glow bg-glow-1"></div>
    <div class="bg-glow bg-glow-2"></div>

    <!-- Pending photo check-in notification -->
    <div
      v-if="
        pendingRecommendation &&
        pendingRecommendation.sessionId &&
        !pendingRecommendation.photoUploaded
      "
      class="pending-notification animate-fade-up animate-start-hidden"
    >
      <div class="pending-icon">
        <svg
          width="20"
          height="20"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <path
            d="M14.5 4h-5L7 7H4a2 2 0 0 0-2 2v9a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2V9a2 2 0 0 0-2-2h-3l-2.5-3z"
          />
          <circle cx="12" cy="13" r="3" />
        </svg>
      </div>
      <div class="pending-text">
        <div class="pending-title">还有未完成的拍照打卡</div>
      </div>
      <button class="pending-action" @click="router.push('/result')">
        查看推荐
      </button>
    </div>

    <!-- Hero Section -->
    <div class="hero animate-fade-up">
      <div class="hero-badge animate-fade-up delay-100 animate-start-hidden">
        <span>AI 驱动</span>
      </div>
      <h1 class="hero-title animate-fade-up delay-200 animate-start-hidden">
        <em>为你推荐</em><br />恰到好处的美味
      </h1>
      <p class="hero-subtitle animate-fade-up delay-300 animate-start-hidden">
        通过智能对话，了解你的时间、心情与偏好<br />
        在万千美食中找到最适合你的那一道
      </p>
    </div>

    <!-- Feature Cards -->
    <div class="features animate-fade-up delay-400 animate-start-hidden">
      <div class="feature-card">
        <div class="feature-icon">
          <svg
            width="20"
            height="20"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
            stroke-linecap="round"
            stroke-linejoin="round"
          >
            <path
              d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"
            ></path>
          </svg>
        </div>
        <div>
          <div class="feature-title">智能对话</div>
          <div class="feature-desc">自然语言交互，理解你的真实需求</div>
        </div>
      </div>
      <div class="feature-card">
        <div class="feature-icon icon-orange">
          <svg
            width="20"
            height="20"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
            stroke-linecap="round"
            stroke-linejoin="round"
          >
            <path d="M20 10c0 6-8 12-8 12s-8-6-8-12a8 8 0 0 1 16 0Z"></path>
            <circle cx="12" cy="10" r="3"></circle>
          </svg>
        </div>
        <div>
          <div class="feature-title">场景感知</div>
          <div class="feature-desc">结合时间、天气、地点多维度推荐</div>
        </div>
      </div>
      <div class="feature-card">
        <div class="feature-icon icon-green">
          <svg
            width="20"
            height="20"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
            stroke-linecap="round"
            stroke-linejoin="round"
          >
            <path
              d="M19 14c1.49-1.46 3-3.21 3-5.5A5.5 5.5 0 0 0 16.5 3c-1.76 0-3 .5-4.5 2-1.5-1.5-2.74-2-4.5-2A5.5 5.5 0 0 0 2 8.5c0 2.3 1.5 4.05 3 5.5l7 7Z"
            ></path>
          </svg>
        </div>
        <div>
          <div class="feature-title">个性匹配</div>
          <div class="feature-desc">根据口味偏好和饮食禁忌精准推荐</div>
        </div>
      </div>
    </div>

    <!-- CTA Button -->
    <div class="cta-area animate-fade-up delay-500 animate-start-hidden">
      <button class="cta-button" @click="startChat" :disabled="loading">
        <span v-if="loading" class="cta-loading"></span>
        <span v-else>开始美食之旅</span>
      </button>
    </div>
    <!-- Version Info -->
    <div class="version-info animate-fade-up">
      <span class="version-badge">v{{ version }}</span>
      <span class="version-date">{{ releaseDate }}</span>
    </div>
    <!-- Confirm dialog overlay -->
    <Transition name="fade">
      <div
        v-if="showConfirmDialog"
        class="dialog-overlay"
        @click.self="showConfirmDialog = false"
      >
        <div class="dialog-card">
          <div class="dialog-title">还有未完成的拍照打卡</div>
          <div class="dialog-desc">是否继续新的对话？</div>
          <div class="dialog-actions">
            <button class="dialog-btn ghost" @click="goToResult">
              去完成打卡
            </button>
            <button class="dialog-btn primary" @click="doStartChat">
              继续对话
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue";
import { useRouter } from "vue-router";
import { conversationApi, recordApi } from "@/api";
import { useChatStore } from "@/stores/chat";
import { showError } from "@/utils/toast";
const router = useRouter();
const chatStore = useChatStore();
const loading = ref(false);
const showConfirmDialog = ref(false);
const pendingRecommendation = ref<any>(null);
const version = import.meta.env.VITE_VERSION || "1.0.0";
const releaseDate = import.meta.env.VITE_TIME || "未知日期";

onMounted(async () => {
  try {
    const res = await recordApi.getPendingRecommendation();
    if (res) {
      pendingRecommendation.value = res;
    }
  } catch {
    // no pending recommendation
  }
});

const hasPendingPhoto = () => {
  return (
    pendingRecommendation.value?.sessionId &&
    !pendingRecommendation.value?.photoUploaded
  );
};

const startChat = async () => {
  if (hasPendingPhoto()) {
    showConfirmDialog.value = true;
    return;
  }
  await doStartChat();
};

const doStartChat = async () => {
  showConfirmDialog.value = false;
  loading.value = true;
  try {
    const response = await conversationApi.start();
    if (!response || !response.sessionId) {
      showError("服务器返回数据异常");
      return;
    }
    chatStore.clearChat();
    chatStore.setSessionId(response.sessionId);
    router.push("/chat");
  } catch (error: any) {
    console.error("[Home] Failed:", error?.message);
    showError("启动对话失败，请确认后端服务已启动");
  } finally {
    loading.value = false;
  }
};

const goToResult = () => {
  showConfirmDialog.value = false;
  router.push("/result");
};
</script>

<style lang="scss" scoped>
.home-container {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: var(--color-surface);
  padding: 40px 24px 60px;
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
  top: -80px;
  right: -60px;
  width: 280px;
  height: 280px;
  background: var(--color-secondary-fixed);
  opacity: 0.15;
}

.bg-glow-2 {
  bottom: 100px;
  left: -80px;
  width: 240px;
  height: 240px;
  background: var(--color-primary-container);
  opacity: 0.1;
  animation-delay: 3s;
}

/* Hero */
.hero {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  text-align: center;
  padding: 40px 0;
  z-index: 1;
}

.hero-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 16px;
  background: var(--color-surface-container-low);
  border-radius: 100px;
  margin-bottom: 28px;
  border: 1px solid var(--color-surface-container-lowest);

  span {
    font-size: 10px;
    font-weight: 700;
    text-transform: uppercase;
    letter-spacing: 0.2em;
    color: var(--color-primary);
  }
}

.hero-title {
  font-family: var(--font-serif);
  font-style: italic;
  font-size: 40px;
  font-weight: 400;
  line-height: 1.2;
  color: var(--color-on-surface);
  margin-bottom: 20px;

  em {
    font-style: italic;
    color: var(--color-primary);
  }
}

.hero-subtitle {
  font-size: 14px;
  line-height: 1.8;
  color: var(--color-on-surface-variant);
  max-width: 320px;
}

/* Feature cards */
.features {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 40px;
  z-index: 1;
}

.feature-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  background: var(--color-surface-container-lowest);
  border-radius: 1.5rem;
  border: 1px solid rgba(255, 255, 255, 0.8);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  transition:
    transform 0.3s cubic-bezier(0.22, 1, 0.36, 1),
    box-shadow 0.3s ease;

  &:active {
    transform: scale(0.98);
  }
}

.feature-icon {
  width: 44px;
  height: 44px;
  border-radius: 1rem;
  background: linear-gradient(
    135deg,
    rgba(0, 89, 182, 0.08),
    rgba(104, 160, 255, 0.12)
  );
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-primary);
  flex-shrink: 0;

  &.icon-orange {
    background: linear-gradient(
      135deg,
      rgba(255, 152, 0, 0.08),
      rgba(255, 183, 77, 0.12)
    );
    color: #f57c00;
  }

  &.icon-green {
    background: linear-gradient(
      135deg,
      rgba(76, 175, 80, 0.08),
      rgba(129, 199, 132, 0.12)
    );
    color: #388e3c;
  }
}

.feature-title {
  font-weight: 700;
  font-size: 14px;
  color: var(--color-on-surface);
  margin-bottom: 2px;
}

.feature-desc {
  font-size: 12px;
  color: var(--color-on-surface-variant);
}

/* CTA */
.cta-area {
  z-index: 1;
}

/* Pending photo check-in notification */
.pending-notification {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 20px;
  background: var(--color-surface-container-lowest);
  border-radius: 1.5rem;
  border: 1px solid var(--color-surface-container-low);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);
  margin-bottom: 24px;
  z-index: 1;
}

.pending-icon {
  width: 40px;
  height: 40px;
  border-radius: 1rem;
  background: linear-gradient(
    135deg,
    rgba(255, 152, 0, 0.08),
    rgba(255, 183, 77, 0.12)
  );
  display: flex;
  align-items: center;
  justify-content: center;
  color: #f57c00;
  flex-shrink: 0;
}

.pending-text {
  flex: 1;
  min-width: 0;
}

.pending-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-on-surface);
}

.pending-action {
  flex-shrink: 0;
  padding: 8px 16px;
  border: none;
  border-radius: 100px;
  background: var(--color-primary);
  color: white;
  font-family: var(--font-sans);
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    opacity: 0.9;
  }
  &:active {
    transform: scale(0.96);
  }
}

/* Confirm dialog */
.dialog-overlay {
  position: fixed;
  inset: 0;
  z-index: 100;
  background: rgba(0, 0, 0, 0.4);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}

.dialog-card {
  width: 100%;
  max-width: 340px;
  background: var(--color-surface);
  border-radius: 2rem;
  padding: 32px 28px;
  box-shadow: 0 24px 64px rgba(0, 0, 0, 0.15);
}

.dialog-title {
  font-family: var(--font-serif);
  font-style: italic;
  font-size: 20px;
  font-weight: 500;
  color: var(--color-on-surface);
  margin-bottom: 8px;
}

.dialog-desc {
  font-size: 14px;
  color: var(--color-on-surface-variant);
  margin-bottom: 28px;
}

.dialog-actions {
  display: flex;
  gap: 10px;
}

.dialog-btn {
  flex: 1;
  padding: 14px 16px;
  border: none;
  border-radius: 1.5rem;
  font-family: var(--font-sans);
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s;

  &.primary {
    background: linear-gradient(
      135deg,
      var(--color-primary-container),
      var(--color-primary)
    );
    color: white;
    box-shadow: 0 8px 24px -6px rgba(0, 89, 182, 0.3);

    &:hover {
      transform: translateY(-1px);
    }
  }

  &.ghost {
    background: transparent;
    border: 1.5px solid var(--color-surface-container-low);
    color: var(--color-on-surface-variant);

    &:hover {
      background: var(--color-surface-container-low);
    }
  }
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.25s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

.cta-button {
  width: 100%;
  padding: 18px;
  border: none;
  border-radius: 2rem;
  background: linear-gradient(
    135deg,
    var(--color-primary-container),
    var(--color-primary)
  );
  color: white;
  font-family: var(--font-sans);
  font-size: 15px;
  font-weight: 700;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  cursor: pointer;
  box-shadow: 0 12px 32px -8px rgba(0, 89, 182, 0.35);
  transition: all 0.3s cubic-bezier(0.22, 1, 0.36, 1);
  position: relative;
  overflow: hidden;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 16px 40px -8px rgba(0, 89, 182, 0.45);
  }

  &:active {
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
  to {
    transform: rotate(360deg);
  }
}

@media (min-width: 640px) {
  .home-container {
    padding: 60px 40px 80px;
  }

  .hero-title {
    font-size: 52px;
  }

  .features {
    flex-direction: row;
    gap: 16px;
  }

  .feature-card {
    flex: 1;
    flex-direction: column;
    text-align: center;
    padding: 28px 20px;
  }

  .cta-button {
    max-width: 400px;
    margin: 0 auto;
    display: block;
  }
}

/* Version Info */
.version-info {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 8px 16px;
  margin-bottom: 16px;
  z-index: 1;
}

.version-badge {
  padding: 4px 10px;
  background: var(--color-surface-container-low);
  border-radius: 100px;
  font-size: 11px;
  font-weight: 700;
  color: var(--color-primary);
  border: 1px solid var(--color-surface-container-lowest);
}

.version-date {
  font-size: 11px;
  color: var(--color-on-surface-variant);
}
</style>

<template>
  <div class="home-container bg-winter-sunrise">
    <!-- Pending photo check-in notification -->
    <div
      v-if="
        pendingRecommendation &&
        pendingRecommendation.hasPending &&
        pendingRecommendation.sessionId
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

    <!-- Hero Section · RecommendationCard -->
    <RecommendationCard
      caption="AI 驱动"
      title="为你推荐恰到好处的美味"
      subtitle="通过智能对话，了解你的时间、心情与偏好，在万千美食中找到最适合你的那一道。"
      class="hero-card animate-fade-up animate-start-hidden"
    />

    <!-- Mood Chips · 心情偏好快捷入口 -->
    <div class="mood-row animate-fade-up delay-100 animate-start-hidden">
      <MoodChip label="热汤面" :selected="mood === 'hot'" @toggle="setMood('hot')" />
      <MoodChip label="暖心菜" :selected="mood === 'warm'" @toggle="setMood('warm')" />
      <MoodChip label="夜宵" :selected="mood === 'late'" @toggle="setMood('late')" />
    </div>

    <!-- Feature Cards · product features (informational) -->
    <div class="features animate-fade-up delay-200 animate-start-hidden">
      <div class="feature-card">
        <div class="feature-icon icon-blue">
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
        <div class="feature-icon icon-cyan">
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
        <div class="feature-icon icon-blue">
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
    <div class="cta-area animate-fade-up delay-300 animate-start-hidden">
      <button
        v-if="isGuest"
        class="cta-button guest-cta"
        @click="goToLogin"
      >
        <span>登录后开始体验</span>
      </button>
      <button v-else class="cta-button btn-primary-gradient" @click="startChat" :disabled="loading">
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
import { ref, onMounted, computed } from "vue";
import { useRouter } from "vue-router";
import { conversationApi, recordApi } from "@/api";
import { useChatStore } from "@/stores/chat";
import { useAuthStore } from "@/stores/auth";
import { showError } from "@/utils/toast";
import RecommendationCard from "@/components/ui/RecommendationCard.vue";
import MoodChip from "@/components/ui/MoodChip.vue";

const router = useRouter();
const chatStore = useChatStore();
const authStore = useAuthStore();
const loading = ref(false);
const showConfirmDialog = ref(false);
const pendingRecommendation = ref<any>(null);
const mood = ref<"hot" | "warm" | "late" | null>(null);
const version = import.meta.env.VITE_VERSION || "1.0.0";
const releaseDate = import.meta.env.VITE_TIME || "未知日期";

const isGuest = computed(() => authStore.isGuest);

function setMood(value: "hot" | "warm" | "late") {
  mood.value = mood.value === value ? null : value;
}

function goToLogin() {
  router.push("/login");
}

onMounted(async () => {
  if (isGuest.value) return;

  try {
    const res = await recordApi.getPendingRecommendation();
    if (res?.hasPending) {
      pendingRecommendation.value = res;
    }
  } catch {
    // no pending recommendation
  }
});

const hasPendingPhoto = () => {
  return (
    pendingRecommendation.value?.hasPending &&
    pendingRecommendation.value?.sessionId
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
  padding: 40px 24px 60px;
  position: relative;
  overflow: hidden;
}

/* Hero card */
.hero-card {
  margin: 40px 0 var(--space-6);
  z-index: 1;
}

/* Mood chips row */
.mood-row {
  display: flex;
  gap: var(--space-2);
  margin-bottom: var(--space-8);
  z-index: 1;
  flex-wrap: wrap;
}

/* Feature cards */
.features {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  margin-bottom: var(--space-10);
  z-index: 1;
}

.feature-card {
  display: flex;
  align-items: center;
  gap: var(--space-4);
  padding: var(--space-5);
  background: var(--color-surface-lowest);
  border-radius: var(--radius-xl);
  border: 1px solid var(--color-surface-low);
  box-shadow: var(--shadow-xs);
  transition: transform var(--dur-base) var(--ease-out-soft), box-shadow var(--dur-base) var(--ease-out-soft);

  &:active {
    transform: scale(0.98);
  }
}

.feature-icon {
  width: 44px;
  height: 44px;
  border-radius: var(--radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;

  &.icon-blue {
    background: rgba(74, 141, 213, 0.10);
    color: var(--color-primary);
  }
  &.icon-cyan {
    background: rgba(140, 225, 243, 0.18);
    color: var(--color-cyan-deep);
  }
  /* icon-warm removed per Phase 3 P0 review (warm-color rule: max 3 per screen) */
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
  gap: var(--space-3);
  padding: var(--space-4) var(--space-5);
  background: var(--color-surface-lowest);
  border-radius: var(--radius-xl);
  border: 1px solid var(--color-surface-low);
  box-shadow: var(--shadow-sm);
  margin-bottom: var(--space-6);
  z-index: 1;
}

.pending-icon {
  width: 40px;
  height: 40px;
  border-radius: var(--radius-lg);
  background: rgba(232, 133, 90, 0.12);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-accent-warm);
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
  padding: var(--space-2) var(--space-4);
  border: none;
  border-radius: var(--radius-pill);
  background: var(--color-primary);
  color: var(--color-on-primary);
  font-family: var(--font-sans);
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
  transition: all var(--dur-fast) var(--ease-out-soft);

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
  z-index: var(--z-dialog);
  background: rgba(11, 15, 16, 0.4);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--space-6);
}

.dialog-card {
  width: 100%;
  max-width: 340px;
  background: var(--color-surface-lowest);
  border-radius: var(--radius-2xl);
  padding: var(--space-8) var(--space-7);
  box-shadow: var(--shadow-xl);
}

.dialog-title {
  font-family: var(--font-sans);
  font-weight: 700;
  font-size: 18px;
  color: var(--color-on-surface);
  margin-bottom: var(--space-2);
}

.dialog-desc {
  font-size: 14px;
  color: var(--color-on-surface-variant);
  margin-bottom: var(--space-7);
}

.dialog-actions {
  display: flex;
  gap: var(--space-2);
}

.dialog-btn {
  flex: 1;
  padding: var(--space-3) var(--space-4);
  border: none;
  border-radius: var(--radius-xl);
  font-family: var(--font-sans);
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  transition: all var(--dur-fast) var(--ease-out-soft);

  &.primary {
    background: linear-gradient(135deg, var(--color-primary-soft), var(--color-primary));
    color: var(--color-on-primary);
    box-shadow: var(--shadow-glow);

    &:hover {
      transform: translateY(-1px);
    }
  }

  &.ghost {
    background: transparent;
    border: 1.5px solid var(--color-surface-low);
    color: var(--color-on-surface-variant);

    &:hover {
      background: var(--color-surface-low);
    }
  }
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity var(--dur-base) ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

.cta-button {
  width: 100%;
  padding: var(--space-5);
  border: none;
  border-radius: var(--radius-2xl);
  color: var(--color-on-primary);
  font-family: var(--font-sans);
  font-size: 15px;
  font-weight: 700;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  cursor: pointer;
  box-shadow: var(--shadow-glow);
  transition: all var(--dur-base) var(--ease-out-soft);

  &:hover {
    transform: translateY(-2px);
    box-shadow: var(--shadow-lg);
  }

  &:active {
    transform: translateY(0);
  }

  &:disabled {
    opacity: 0.7;
    cursor: not-allowed;
  }

  &.guest-cta {
    background: linear-gradient(135deg, var(--color-cyan), var(--color-primary-soft));
    box-shadow: var(--shadow-md);
  }
}

.cta-loading {
  display: inline-block;
  width: 20px;
  height: 20px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: var(--color-on-primary);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

@media (min-width: 640px) {
  .home-container {
    padding: 60px 40px 80px;
  }

  .features {
    flex-direction: row;
    gap: var(--space-4);
  }

  .feature-card {
    flex: 1;
    flex-direction: column;
    text-align: center;
    padding: var(--space-7) var(--space-5);
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
  gap: var(--space-3);
  padding: var(--space-2) var(--space-4);
  margin-top: var(--space-4);
  margin-bottom: var(--space-4);
  z-index: 1;
}

.version-badge {
  padding: var(--space-1) var(--space-2);
  background: var(--color-surface-low);
  border-radius: var(--radius-pill);
  font-size: 11px;
  font-weight: 700;
  color: var(--color-primary);
  border: 1px solid var(--color-surface-lowest);
}

.version-date {
  font-size: 11px;
  color: var(--color-on-surface-variant);
}
</style>
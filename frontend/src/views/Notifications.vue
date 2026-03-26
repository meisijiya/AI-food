<template>
  <div class="notifications-container">
    <div class="bg-glow bg-glow-1"></div>
    <div class="bg-glow bg-glow-2"></div>

    <!-- Header -->
    <div class="page-header animate-fade-up">
      <button class="back-btn" @click="router.back()">
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
          <path d="m15 18-6-6 6-6" />
        </svg>
      </button>
      <h1 class="page-title"><em>消息通知</em></h1>
      <button
        v-if="notifications.length > 0"
        class="clear-btn"
        @click="handleClearAll"
      >
        清空
      </button>
    </div>

    <!-- Notification List -->
    <div class="notification-list" ref="scrollContainer" @scroll="onScroll">
      <div
        v-for="(item, index) in notifications"
        :key="item.id || item.conversationId || index"
        class="notification-item animate-fade-up"
        :style="{ animationDelay: (index % 10) * 0.05 + 's' }"
        @click="handleClick(item)"
      >
        <!-- Comment notification -->
        <template v-if="item.type === 'comment'">
          <div class="notif-avatar">
            <img v-if="item.avatar" :src="item.avatar" alt="" />
            <span v-else>{{ item.nickname?.charAt(0) || "?" }}</span>
          </div>
          <div class="notif-body">
            <div class="notif-header">
              <span class="notif-title"
                >{{ item.nickname }}
                <span class="notif-action">评论了你的动态</span></span
              >
              <button class="notif-delete" @click.stop="handleDelete(item.id)">
                <svg
                  width="14"
                  height="14"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                >
                  <line x1="18" y1="6" x2="6" y2="18" />
                  <line x1="6" y1="6" x2="18" y2="18" />
                </svg>
              </button>
            </div>
            <div class="notif-content">{{ item.content }}</div>
            <div class="notif-time">{{ formatTime(item.timestamp) }}</div>
          </div>
        </template>

        <!-- Chat notification (aggregated) -->
        <template v-else-if="item.type === 'chat'">
          <div class="notif-avatar">
            <img v-if="item.avatar" :src="item.avatar" alt="" />
            <span v-else>{{ item.nickname?.charAt(0) || "?" }}</span>
            <span v-if="item.unreadCount > 0" class="notif-badge">{{
              item.unreadCount > 99 ? "99+" : item.unreadCount
            }}</span>
          </div>
          <div class="notif-body">
            <div class="notif-header">
              <span class="notif-title"
                >{{ item.nickname }}
                <span class="notif-action">发来消息</span></span
              >
              <button
                class="notif-delete"
                @click.stop="handleDelete('chat_' + item.conversationId)"
              >
                <svg
                  width="14"
                  height="14"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                >
                  <line x1="18" y1="6" x2="6" y2="18" />
                  <line x1="6" y1="6" x2="18" y2="18" />
                </svg>
              </button>
            </div>
            <div class="notif-content">{{ item.lastMessage }}</div>
            <div class="notif-time">{{ formatTime(item.timestamp) }}</div>
          </div>
        </template>
      </div>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="loading-more"><div class="spinner"></div></div>

    <!-- Empty -->
    <div v-if="!loading && notifications.length === 0" class="empty-state">
      <svg
        width="48"
        height="48"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        stroke-width="1.5"
        stroke-linecap="round"
        stroke-linejoin="round"
        class="empty-icon"
      >
        <path d="M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9" />
        <path d="M10.3 21a1.94 1.94 0 0 0 3.4 0" />
      </svg>
      <div class="empty-text">暂无通知</div>
    </div>

    <div class="nav-spacer"></div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue";
import { useRouter } from "vue-router";
import { notificationApi } from "@/api";
import { showSuccess, showError } from "@/utils/toast";
import { showConfirmDialog } from "vant";

const router = useRouter();
const scrollContainer = ref<HTMLElement | null>(null);

const notifications = ref<any[]>([]);
const loading = ref(false);
const page = ref(0);
const hasMore = ref(true);
const commentTotal = ref(0);

async function fetchNotifications(reset = false) {
  if (loading.value) return;
  if (!reset && !hasMore.value) return;

  loading.value = true;
  const p = reset ? 0 : page.value;

  try {
    const res = await notificationApi.getList({ page: p, size: 10 });
    const items = res?.items || [];
    commentTotal.value = res?.commentTotal || 0;

    if (reset) {
      notifications.value = items;
    } else {
      notifications.value.push(...items);
    }

    hasMore.value =
      items.length >= 10 || p * 10 + items.length < commentTotal.value;
    page.value = p + 1;
  } catch {
    // ignore
  } finally {
    loading.value = false;
  }
}

function onScroll() {
  const el = scrollContainer.value;
  if (!el) return;
  if (el.scrollTop + el.clientHeight >= el.scrollHeight - 100) {
    fetchNotifications(false);
  }
}

function handleClick(item: any) {
  if (item.type === "comment" && item.postId) {
    router.push(`/feed/${item.postId}`);
  } else if (item.type === "chat") {
    router.push({
      path: "/chat-room",
      query: {
        userId: String(item.senderId),
        nickname: item.nickname,
        avatar: item.avatar || "",
      },
    });
  }
}

async function handleDelete(notificationId: string) {
  try {
    await notificationApi.deleteOne(notificationId);
    notifications.value = notifications.value.filter((n) => {
      if (n.type === "chat")
        return "chat_" + n.conversationId !== notificationId;
      return n.id !== notificationId;
    });
    showSuccess("已删除");
  } catch {
    showError("删除失败");
  }
}

async function handleClearAll() {
  try {
    await showConfirmDialog({ title: "确认", message: "确定清空所有通知吗？" });
    await notificationApi.clearAll();
    notifications.value = [];
    showSuccess("已清空");
  } catch {
    // user cancelled
  }
}

function formatTime(timestamp: number) {
  if (!timestamp) return "";
  const diff = Date.now() - timestamp;
  if (diff < 60000) return "刚刚";
  if (diff < 3600000) return Math.floor(diff / 60000) + "分钟前";
  if (diff < 86400000) return Math.floor(diff / 3600000) + "小时前";
  if (diff < 604800000) return Math.floor(diff / 86400000) + "天前";
  return new Date(timestamp).toLocaleDateString();
}

onMounted(() => {
  fetchNotifications(true);
});
</script>

<style lang="scss" scoped>
.notifications-container {
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
  position: relative;
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
  flex: 1;
  em {
    font-style: italic;
    color: var(--color-primary);
  }
}

.clear-btn {
  padding: 6px 16px;
  border: 1.5px solid var(--color-surface-container-low);
  border-radius: 100px;
  background: transparent;
  color: var(--color-on-surface-variant);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  &:active {
    border-color: var(--color-primary);
    color: var(--color-primary);
  }
}

.notification-list {
  flex: 1;
  overflow-y: auto;
  z-index: 1;
  position: relative;
  -webkit-overflow-scrolling: touch;
}

.notification-item {
  display: flex;
  gap: 12px;
  padding: 16px;
  background: var(--color-surface-container-lowest);
  border-radius: 1.5rem;
  border: 1px solid rgba(255, 255, 255, 0.8);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  margin-bottom: 10px;
  cursor: pointer;
  transition: transform 0.15s;
  &:active {
    transform: scale(0.98);
  }
}

.notif-avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: linear-gradient(
    135deg,
    var(--color-primary-container),
    var(--color-primary)
  );
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-family: var(--font-serif);
  font-style: italic;
  font-size: 18px;
  flex-shrink: 0;
  overflow: hidden;
  position: relative;
  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}

.notif-badge {
  position: absolute;
  top: 2px;
  right: 2px;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: 100px;
  background: #ef4444;
  color: white;
  font-size: 10px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2px solid var(--color-surface);
}

.notif-body {
  flex: 1;
  min-width: 0;
}

.notif-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 4px;
}

.notif-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-on-surface);
}

.notif-action {
  font-weight: 400;
  color: var(--color-on-surface-variant);
}

.notif-delete {
  background: none;
  border: none;
  color: var(--color-on-surface-variant);
  cursor: pointer;
  padding: 2px;
  opacity: 0.5;
  transition: opacity 0.2s;
  &:active {
    opacity: 1;
  }
}

.notif-content {
  font-size: 13px;
  color: var(--color-on-surface-variant);
  line-height: 1.5;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  margin-bottom: 4px;
}

.notif-time {
  font-size: 11px;
  color: var(--color-on-surface-variant);
  opacity: 0.6;
}

.loading-more {
  display: flex;
  justify-content: center;
  padding: 16px 0;
  z-index: 1;
  position: relative;
}

.spinner {
  width: 28px;
  height: 28px;
  border: 3px solid var(--color-surface-container-low);
  border-top-color: var(--color-primary);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 0;
  z-index: 1;
  position: relative;
}

.empty-icon {
  color: var(--color-on-surface-variant);
  opacity: 0.3;
  margin-bottom: 16px;
}
.empty-text {
  font-size: 14px;
  color: var(--color-on-surface-variant);
  opacity: 0.6;
}

.nav-spacer {
  height: 80px;
}

@media (min-width: 1024px) {
  .notifications-container {
    max-width: 60%;
    margin: 0 auto;
    padding: 0 40px 100px;
  }
}
</style>

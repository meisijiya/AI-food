<template>
  <!--
    FeedPostCard - 大厅 Tab 的单个帖子卡片
    仅展示,点击事件通过 emit('click') 回传父组件路由跳转
    子组件不调 API、不持有跨组件 state
  -->
  <div
    class="feed-card animate-scale-in"
    :style="{ animationDelay: (index % 6) * 0.05 + 's' }"
    @click="emit('click', post)"
  >
    <div v-if="post.thumbnailUrl" class="card-photo">
      <CachedImage
        :src="post.thumbnailUrl"
        :alt="post.foodName"
        :lazy="true"
      />
    </div>
    <div class="card-body">
      <div class="card-food">
        <span>{{ post.foodName }}</span>
        <span v-if="post.visibility === 'friends'" class="fans-only-badge">仅粉丝可见</span>
      </div>
      <div v-if="post.commentPreview" class="card-preview">
        {{ post.commentPreview }}
      </div>
    </div>
    <div class="card-footer">
      <div class="card-user">
        <img
          v-if="post.avatar"
          :src="post.avatar"
          class="card-avatar"
          alt=""
        />
        <div v-else class="card-avatar-placeholder">
          <svg
            width="12"
            height="12"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
            stroke-linecap="round"
            stroke-linejoin="round"
          >
            <path d="M19 21v-2a4 4 0 0 0-4-4H9a4 4 0 0 0-4 4v2" />
            <circle cx="12" cy="7" r="4" />
          </svg>
        </div>
        <span class="card-nickname">{{ post.nickname || '匿名' }}</span>
      </div>
      <div class="card-likes">
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
          <path
            d="M19 14c1.49-1.46 3-3.21 3-5.5A5.5 5.5 0 0 0 16.5 3c-1.76 0-3 .5-4.5 2-1.5-1.5-2.74-2-4.5-2A5.5 5.5 0 0 0 2 8.5c0 2.3 1.5 4.05 3 5.5l7 7Z"
          />
        </svg>
        {{ post.likeCount || 0 }}
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
// FeedPostCard - 大厅帖子卡片(子组件,纯展示)
import CachedImage from '@/components/CachedImage.vue'
import type { FeedPost } from '@/types/feed'

defineProps<{
  post: FeedPost
  index: number
}>()

const emit = defineEmits<{
  (e: 'click', post: FeedPost): void
}>()
</script>

<style lang="scss" scoped>
/* 卡片外层(原 Feed.vue .feed-card 样式) */
.feed-card {
  break-inside: avoid;
  margin-bottom: 10px;
  background: var(--color-surface-lowest);
  border-radius: 1.25rem;
  border: 1px solid rgba(255, 255, 255, 0.8);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  overflow: hidden;
  cursor: pointer;
  transition:
    transform 0.2s,
    box-shadow 0.2s;
  &:active {
    transform: scale(0.98);
  }
}

/* 卡片照片 */
.card-photo {
  img {
    width: 100%;
    display: block;
    border-radius: 1.25rem 1.25rem 0 0;
    max-height: 240px;
    object-fit: cover;
  }
}

/* 卡片正文 */
.card-body {
  padding: 10px 12px;
}

.card-food {
  font-family: var(--font-serif);
  font-style: italic;
  font-size: 15px;
  font-weight: 500;
  color: var(--color-on-surface);
  margin-bottom: 4px;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
  display: flex;
  align-items: center;
  gap: 6px;
}

.fans-only-badge {
  flex-shrink: 0;
  font-family: var(--font-sans);
  font-style: normal;
  font-size: 10px;
  font-weight: 700;
  padding: 2px 6px;
  border-radius: 4px;
  background: rgba(74, 141, 213, 0.08);
  color: var(--color-primary);
  white-space: nowrap;
}

.card-preview {
  font-size: 12px;
  line-height: 1.5;
  color: var(--color-on-surface-variant);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* 卡片底部(用户信息 + 点赞数) */
.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px 10px;
}

.card-user {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
}

.card-avatar {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
}

.card-avatar-placeholder {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: var(--color-surface-low);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-on-surface-variant);
  flex-shrink: 0;
}

.card-nickname {
  font-size: 11px;
  color: var(--color-on-surface-variant);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-likes {
  display: flex;
  align-items: center;
  gap: 3px;
  font-size: 11px;
  color: var(--color-on-surface-variant);
  flex-shrink: 0;
  svg {
    color: #ef4444;
  }
}

/* ponytail: 入场动画 keyframes 在 main.scss 全局定义,这里无需重复 */
</style>

<template>
  <!--
    FeedHotRankItem - 热榜 Tab 单个条目
    仅展示,点击事件通过 emit('click') 回传父组件路由跳转
    子组件不调 API、不持有跨组件 state
  -->
  <div
    class="hot-rank-item animate-fade-up"
    :style="{ animationDelay: (index % 10) * 0.05 + 's' }"
    @click="emit('click', item)"
  >
    <span class="rank-num" :class="{ 'top-3': index < 3 }">{{ index + 1 }}</span>
    <div v-if="item.thumbnailUrl" class="rank-photo">
      <CachedImage
        :src="item.thumbnailUrl"
        :alt="item.foodName"
        :lazy="true"
      />
    </div>
    <div class="rank-content">
      <div class="rank-food">
        <span>{{ item.foodName }}</span>
        <span v-if="item.visibility === 'friends'" class="fans-only-badge">仅粉丝可见</span>
      </div>
      <div class="rank-meta">
        <span class="rank-user">{{ item.nickname || '匿名' }}</span>
        <span class="rank-score">热度 {{ item.hotScore }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
// FeedHotRankItem - 热榜条目(子组件,纯展示)
import CachedImage from '@/components/CachedImage.vue'
import type { HotRankItem } from '@/types/feed'

defineProps<{
  item: HotRankItem
  index: number
}>()

const emit = defineEmits<{
  (e: 'click', item: HotRankItem): void
}>()
</script>

<style lang="scss" scoped>
.hot-rank-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: var(--color-surface-lowest);
  border-radius: 1.25rem;
  border: 1px solid rgba(255, 255, 255, 0.8);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  margin-bottom: 8px;
  cursor: pointer;
  transition: transform 0.2s;
  &:active {
    transform: scale(0.98);
  }
}

.rank-num {
  width: 24px;
  height: 24px;
  border-radius: 6px;
  background: var(--color-surface-low);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
  color: var(--color-on-surface-variant);
  flex-shrink: 0;

  &.top-3 {
    background: linear-gradient(135deg, #f59e0b, #ef4444);
    color: white;
  }
}

.rank-photo {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  overflow: hidden;
  flex-shrink: 0;
  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}

.rank-content {
  flex: 1;
  min-width: 0;
}

.rank-food {
  font-family: var(--font-serif);
  font-style: italic;
  font-size: 15px;
  font-weight: 500;
  color: var(--color-on-surface);
  margin-bottom: 4px;
  display: flex;
  align-items: center;
  gap: 6px;
  overflow: hidden;
  span:first-child {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.rank-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: var(--color-on-surface-variant);
}

.rank-user {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.rank-score {
  color: var(--color-primary);
  font-weight: 600;
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

/* ponytail: 入场动画 keyframes 在 main.scss 全局定义,这里无需重复 */
</style>

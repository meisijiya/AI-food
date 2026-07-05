<!--
  ResultShare - 分享按钮 / 分享链接卡(子组件)
  父组件 ResultActions 传入 shareUrl + sharing,
  本组件无内部状态,事件回传 share / copy-share-url 由父组件处理 API。
-->
<template>
  <div class="share-section animate-fade-up delay-490 animate-start-hidden">
    <div class="share-card" v-if="!shareUrl">
      <button class="share-btn" @click="emit('share')" :disabled="sharing">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="18" cy="5" r="3"/><circle cx="6" cy="12" r="3"/><circle cx="18" cy="19" r="3"/><line x1="8.59" x2="15.42" y1="13.51" y2="17.49"/><line x1="15.41" x2="8.59" y1="6.51" y2="17.49"/></svg>
        {{ sharing ? '创建中...' : '分享此美食' }}
      </button>
    </div>
    <div class="share-link-card" v-else>
      <div class="share-link-label">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="18" cy="5" r="3"/><circle cx="6" cy="12" r="3"/><circle cx="18" cy="19" r="3"/><line x1="8.59" x2="15.42" y1="13.51" y2="17.49"/><line x1="15.41" x2="8.59" y1="6.51" y2="17.49"/></svg>
        <span>分享链接</span>
      </div>
      <div class="share-link-row">
        <input class="share-link-input" :value="shareUrl" readonly />
        <button class="share-copy-btn" @click="emit('copy-share-url')">复制链接</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
// ResultShare - 纯展示 + 事件转发,无内部状态,无 API 调用
defineProps<{
  shareUrl: string
  sharing: boolean
}>()

const emit = defineEmits<{
  (e: 'share'): void
  (e: 'copy-share-url'): void
}>()
</script>

<style lang="scss" scoped>
.share-section {
  margin-bottom: 4px;
  z-index: 1;
}

.share-card {
  display: flex;
}

.share-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 12px 24px;
  border: 1.5px solid var(--color-primary);
  border-radius: 2rem;
  background: none;
  color: var(--color-primary);
  font-family: var(--font-sans);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    background: var(--color-primary-06);
  }

  &:active {
    transform: scale(0.97);
  }

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
}

.share-link-card {
  background: var(--color-surface-lowest);
  border-radius: 1.25rem;
  padding: 16px 20px;
  border: 1px solid var(--color-surface-low);
  width: 100%;
}

.share-link-label {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  font-weight: 600;
  color: var(--color-on-surface-variant);
  margin-bottom: 12px;

  svg {
    color: var(--color-primary);
  }
}

.share-link-row {
  display: flex;
  gap: 8px;
  align-items: center;
}

.share-link-input {
  flex: 1;
  padding: 10px 14px;
  border: 1px solid var(--color-surface-low);
  border-radius: 1rem;
  background: var(--color-surface);
  font-family: var(--font-sans);
  font-size: 12px;
  color: var(--color-on-surface);
  outline: none;
  min-width: 0;
}

.share-copy-btn {
  flex-shrink: 0;
  padding: 10px 16px;
  border: none;
  border-radius: 1rem;
  background: linear-gradient(135deg, var(--color-primary-soft), var(--color-primary));
  color: white;
  font-family: var(--font-sans);
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;

  &:hover {
    transform: translateY(-1px);
  }

  &:active {
    transform: scale(0.97);
  }
}
</style>

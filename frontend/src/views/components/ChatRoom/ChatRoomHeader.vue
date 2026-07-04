<template>
  <!-- 聊天会话顶部栏：返回 + 对方昵称 + 清空聊天按钮 -->
  <div class="room-header">
    <button class="back-btn" @click="emit('back')">
      <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m15 18-6-6 6-6"/></svg>
    </button>
    <div class="header-info">
      <div class="header-name">{{ nickname }}</div>
    </div>
    <button class="clear-btn" @click="emit('clear')">
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg>
    </button>
  </div>
</template>

<script setup lang="ts">
// 顶部栏子组件：纯展示 + 两个按钮事件，状态与路由跳转由父组件负责。
defineProps<{
  nickname: string
}>()

const emit = defineEmits<{
  (e: 'back'): void
  (e: 'clear'): void
}>()
</script>

<style lang="scss" scoped>
.room-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 40px 12px 12px;
  background: var(--color-surface-lowest);
  border-bottom: 1px solid var(--color-surface-low);
  flex-shrink: 0;
}

.back-btn {
  width: 36px;
  height: 36px;
  border: none;
  border-radius: 50%;
  background: none;
  color: var(--color-on-surface);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  &:active { background: var(--color-surface-low); }
}

.header-info {
  flex: 1;
  text-align: center;
}

.header-name {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-on-surface);
}

.clear-btn {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: none;
  border: none;
  color: var(--color-on-surface-variant);
  cursor: pointer;
  border-radius: 50%;
  flex-shrink: 0;
  transition: background 0.2s;
  &:active { background: var(--color-surface-low); }
}
</style>

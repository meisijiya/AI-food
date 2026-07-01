<template>
  <!--
    ResultRecommendation - 重新推荐 / 返回首页 操作按钮组
    纯事件回调,无内部状态
  -->
  <div class="actions animate-fade-up delay-500 animate-start-hidden">
    <button class="primary-btn" @click="emit('new-chat')">
      再来一次
    </button>
    <button class="ghost-btn" @click="emit('go-home')">
      返回首页
    </button>
  </div>
</template>

<script setup lang="ts">
// ResultRecommendation - 重新推荐按钮(子组件,纯事件)
// 新会话 / 返回首页 两个动作,emit 回调到父组件处理(路由跳转 + 清 chat store)
const emit = defineEmits<{
  (e: 'new-chat'): void
  (e: 'go-home'): void
}>()
</script>

<style lang="scss" scoped>
.actions {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 20px;
}

.primary-btn {
  width: 100%;
  padding: 18px;
  border: none;
  border-radius: 2rem;
  background: linear-gradient(135deg, var(--color-primary-container), var(--color-primary));
  color: white;
  font-family: var(--font-sans);
  font-size: 15px;
  font-weight: 700;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  cursor: pointer;
  box-shadow: 0 12px 32px -8px rgba(0, 89, 182, 0.35);
  transition: all 0.3s cubic-bezier(0.22, 1, 0.36, 1);
  &:hover { transform: translateY(-2px); box-shadow: 0 16px 40px -8px rgba(0, 89, 182, 0.45); }
  &:active { transform: translateY(0); }
}

.ghost-btn {
  width: 100%;
  padding: 16px;
  border: 1.5px solid var(--color-surface-container-low);
  border-radius: 2rem;
  background: transparent;
  color: var(--color-on-surface-variant);
  font-family: var(--font-sans);
  font-size: 14px;
  font-weight: 600;
  letter-spacing: 0.05em;
  cursor: pointer;
  transition: all 0.2s;
  &:hover { background: var(--color-surface-container-low); }
}
</style>
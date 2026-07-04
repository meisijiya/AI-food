<template>
  <div class="bubble-row" :class="[`bubble-row--${role}`, modifier ? `bubble-row--${modifier}` : '']">
    <div v-if="role === 'ai'" class="avatar avatar--ai" />
    <div class="bubble" :class="[`bubble--${role}`, modifier ? `bubble--${modifier}` : '']">
      <slot />
    </div>
  </div>
</template>

<script setup lang="ts">
interface Props {
  role: "user" | "ai";
  modifier?: "retry" | "interrupt";
}
defineProps<Props>();
</script>

<style lang="scss" scoped>
.bubble-row {
  display: flex;
  align-items: flex-start;
  gap: var(--space-2);
  margin-bottom: var(--space-2);
}
.bubble-row--user {
  justify-content: flex-end;
}
.avatar {
  width: 28px;
  height: 28px;
  border-radius: var(--radius-md);
  flex-shrink: 0;
}
.avatar--ai {
  background: linear-gradient(135deg, var(--color-accent-warm), var(--color-accent-warm-2));
}
.bubble {
  max-width: 70%;
  padding: var(--space-3);
  border-radius: var(--radius-md);
  font-family: var(--font-sans);
  font-size: 13px;
  line-height: 1.45;
}
.bubble--ai {
  background: linear-gradient(135deg, #fef0e8, #fff5ee);
  color: #7a3d22;
}
.bubble--user {
  background: var(--color-primary);
  color: var(--color-on-primary);
}
.bubble--retry {
  background: var(--color-surface-low);
  border: 1px dashed var(--color-primary-soft);
  color: var(--color-on-surface);
}
.bubble--interrupt {
  background: rgba(200, 52, 74, 0.08);
  border: 1px solid var(--color-danger);
  color: var(--color-danger);
}
</style>
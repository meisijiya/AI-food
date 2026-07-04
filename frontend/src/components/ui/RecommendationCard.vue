<template>
  <div class="recommendation-card">
    <div v-if="caption" class="caption">{{ caption }}</div>
    <div class="title">{{ title }}</div>
    <div v-if="subtitle" class="subtitle">{{ subtitle }}</div>
    <slot name="extra" />
  </div>
</template>

<script setup lang="ts">
interface Props {
  caption?: string;
  title: string;
  subtitle?: string;
}
defineProps<Props>();
</script>

<style lang="scss" scoped>
.recommendation-card {
  background: var(--glass-bg-strong);
  backdrop-filter: var(--glass-blur-strong);
  -webkit-backdrop-filter: var(--glass-blur-strong);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-xl);
  padding: var(--space-6);
  box-shadow: var(--shadow-glow);
}
.caption {
  font-family: var(--font-sans);
  font-weight: 600;
  font-size: 11px;
  letter-spacing: 0.04em;
  text-transform: uppercase;
  color: var(--color-accent-warm);
  margin-bottom: var(--space-2);
}
.title {
  font-family: var(--font-sans);
  font-weight: 700;
  font-size: 24px;
  line-height: 1.25;
  letter-spacing: -0.01em;
  color: var(--color-primary);
  margin-bottom: var(--space-2);
}
.subtitle {
  font-family: var(--font-sans);
  font-weight: 400;
  font-size: 14px;
  line-height: 1.45;
  color: var(--color-on-surface-variant);
}
</style>
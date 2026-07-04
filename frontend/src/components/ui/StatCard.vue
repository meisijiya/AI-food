<template>
  <div class="stat-card">
    <div class="stat-number">{{ number }}</div>
    <div class="stat-label">{{ label }}</div>
  </div>
</template>

<script setup lang="ts">
interface Props {
  number: number | string;
  label: string;
}
defineProps<Props>();
</script>

<style lang="scss" scoped>
.stat-card {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border-radius: var(--radius-lg);
  padding: var(--space-4) var(--space-3) var(--space-2);
  text-align: center;
  box-shadow: var(--shadow-md);
}
.stat-number {
  font-family: var(--font-serif);
  font-style: italic;
  font-weight: 600;
  font-size: 26px;
  line-height: 1;
  color: var(--color-primary);
}
.stat-label {
  font-family: var(--font-sans);
  font-weight: 600;
  font-size: 11px;
  letter-spacing: 0.04em;
  text-transform: uppercase;
  color: var(--color-on-surface-variant);
  margin-top: 2px;
}
</style>
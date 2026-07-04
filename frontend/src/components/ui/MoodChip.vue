<template>
  <button
    class="mood-chip"
    :class="{ 'mood-chip--selected': selected }"
    type="button"
    @click="$emit('toggle')"
  >
    <span class="label">{{ label }}</span>
  </button>
</template>

<script setup lang="ts">
interface Props {
  label: string;
  selected?: boolean;
}
defineProps<Props>();
defineEmits<{ (e: 'toggle'): void }>();
</script>

<style lang="scss" scoped>
.mood-chip {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-2) var(--space-4);
  border-radius: var(--radius-pill);
  background: var(--color-surface-lowest);
  border: 1px solid var(--color-surface-low);
  font-family: var(--font-sans);
  font-weight: 600;
  font-size: 12px;
  color: var(--color-on-surface);
  cursor: pointer;
  transition: all var(--dur-fast) var(--ease-out-soft);
}
.mood-chip:hover {
  border-color: var(--color-accent-warm);
  transform: translateY(-1px);
}
.mood-chip:active {
  transform: scale(0.95);
}
.mood-chip--selected {
  background: rgba(232, 133, 90, 0.12);
  border-color: var(--color-accent-warm);
  color: var(--color-accent-warm);
}
</style>
<template>
  <div class="theme-switcher">
    <button
      v-for="opt in options"
      :key="opt.value"
      class="theme-option"
      :class="{ 'theme-option--active': theme === opt.value }"
      type="button"
      @click="setTheme(opt.value)"
    >
      <svg
        class="theme-option__icon"
        width="14"
        height="14"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        stroke-width="2"
        stroke-linecap="round"
        stroke-linejoin="round"
        v-html="opt.iconPath"
      />
      <span class="theme-option__label">{{ opt.label }}</span>
    </button>
  </div>
</template>

<script setup lang="ts">
import { useThemeStore, type ThemeMode } from '@/stores/theme';

const { theme, setTheme } = useThemeStore();

const SUN_ICON = '<circle cx="12" cy="12" r="4"/><path d="M12 2v2"/><path d="M12 20v2"/><path d="m4.93 4.93 1.41 1.41"/><path d="m17.66 17.66 1.41 1.41"/><path d="M2 12h2"/><path d="M20 12h2"/><path d="m6.34 17.66-1.41 1.41"/><path d="m19.07 4.93-1.41 1.41"/>';

const MOON_ICON = '<path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/>';

const MONITOR_ICON = '<rect x="2" y="3" width="20" height="14" rx="2" ry="2"/><line x1="8" y1="21" x2="16" y2="21"/><line x1="12" y1="17" x2="12" y2="21"/>';

const options: Array<{ value: ThemeMode; label: string; iconPath: string }> = [
  { value: 'light', label: '浅色', iconPath: SUN_ICON },
  { value: 'dark', label: '深色', iconPath: MOON_ICON },
  { value: 'system', label: '跟随系统', iconPath: MONITOR_ICON },
];
</script>

<style lang="scss" scoped>
.theme-switcher {
  display: flex;
  gap: var(--space-2);
  padding: var(--space-2);
  background: var(--color-surface-low);
  border-radius: var(--radius-pill);
}
.theme-option {
  display: inline-flex;
  align-items: center;
  gap: var(--space-1);
  padding: var(--space-2) var(--space-4);
  border: none;
  border-radius: var(--radius-pill);
  background: transparent;
  color: var(--color-on-surface-variant);
  font-family: var(--font-sans);
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all var(--dur-fast) var(--ease-out-soft);
}
.theme-option:hover {
  color: var(--color-on-surface);
}
.theme-option--active {
  background: var(--color-primary);
  color: var(--color-on-primary);
  box-shadow: var(--shadow-sm);
}
.theme-option__icon {
  flex-shrink: 0;
}
</style>
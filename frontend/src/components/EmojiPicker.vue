<template>
  <Transition name="emoji-panel">
    <div v-if="show" class="emoji-panel" @click.stop>
      <div class="emoji-panel-header">
        <span class="emoji-panel-title">表情</span>
        <button class="emoji-close-btn" @click="$emit('close')">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
        </button>
      </div>
      <div class="emoji-grid">
        <button
          v-for="item in emojis"
          :key="item.name"
          class="emoji-item"
          @click="$emit('select', item.icon)"
        >
          {{ item.icon }}
        </button>
      </div>
    </div>
  </Transition>
</template>

<script setup lang="ts">
import emojiData from '@/emoji/emojiText.json'

defineProps<{
  show: boolean
}>()

defineEmits<{
  select: [icon: string]
  close: []
}>()

const emojis = emojiData.list
</script>

<style lang="scss" scoped>
.emoji-panel {
  background: var(--color-surface-container-lowest);
  border-radius: 1.5rem 1.5rem 0 0;
  border-top: 1px solid rgba(255, 255, 255, 0.8);
  box-shadow: 0 -4px 20px rgba(0, 0, 0, 0.08);
  max-height: 280px;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
}

.emoji-panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px 8px;
  border-bottom: 1px solid var(--color-surface-container-low);
}

.emoji-panel-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-on-surface-variant);
}

.emoji-close-btn {
  background: none;
  border: none;
  color: var(--color-on-surface-variant);
  cursor: pointer;
  padding: 4px;
  display: flex;
  align-items: center;
  border-radius: 50%;
  transition: background 0.2s;
  &:active { background: var(--color-surface-container-low); }
}

.emoji-grid {
  flex: 1;
  overflow-y: auto;
  padding: 10px 12px;
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 4px;
  -webkit-overflow-scrolling: touch;
}

.emoji-item {
  width: 100%;
  aspect-ratio: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  background: none;
  border: none;
  border-radius: 10px;
  cursor: pointer;
  transition: background 0.15s, transform 0.1s;
  &:active {
    background: var(--color-surface-container-low);
    transform: scale(0.9);
  }
}

/* Panel transitions */
.emoji-panel-enter-active {
  transition: transform 0.25s cubic-bezier(0.4, 0, 0.2, 1), opacity 0.2s;
}
.emoji-panel-leave-active {
  transition: transform 0.2s cubic-bezier(0.4, 0, 0.2, 1), opacity 0.15s;
}
.emoji-panel-enter-from {
  transform: translateY(100%);
  opacity: 0;
}
.emoji-panel-leave-to {
  transform: translateY(100%);
  opacity: 0;
}
</style>

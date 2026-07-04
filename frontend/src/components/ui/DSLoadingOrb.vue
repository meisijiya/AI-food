<template>
  <div class="loading-orb">
    <span
      v-for="i in 3"
      :key="i"
      class="dot"
      :style="{ animationDelay: `${(i - 1) * 150}ms` }"
    />
  </div>
</template>

<style lang="scss" scoped>
.loading-orb {
  display: inline-flex;
  gap: var(--space-1);
  padding: var(--space-3);
  border-radius: var(--radius-md);
  background: linear-gradient(135deg, #fef0e8, #fff5ee);
}
.dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: rgba(122, 61, 34, 0.4);
  animation: orb-bounce 1.2s ease-in-out infinite;
}
@keyframes orb-bounce {
  0%, 80%, 100% { transform: translateY(0); }
  40% { transform: translateY(-4px); }
}
</style>
<template>
  <router-view v-slot="{ Component }">
    <transition name="sanctuary-fade" mode="out-in">
      <component :is="Component" />
    </transition>
  </router-view>
</template>

<script setup lang="ts">
</script>

<style lang="scss">
#app {
  font-family: var(--font-sans);
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

/* Sanctuary page transition */
.sanctuary-fade-enter-active {
  animation: sanctuary-fade-up 0.4s cubic-bezier(0.22, 1, 0.36, 1) forwards;
}

.sanctuary-fade-leave-active {
  animation: sanctuary-fade-out 0.25s ease forwards;
}

@keyframes sanctuary-fade-out {
  from {
    opacity: 1;
    transform: translateY(0);
  }
  to {
    opacity: 0;
    transform: translateY(-10px);
  }
}
</style>

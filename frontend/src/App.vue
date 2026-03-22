<template>
  <router-view v-slot="{ Component }">
    <transition name="sanctuary-fade" mode="out-in">
      <component :is="Component" />
    </transition>
  </router-view>

  <!-- Floating pill bottom nav — scroll to hide/show -->
  <transition name="nav-slide">
    <nav v-show="showNav && !isNavHidden" class="bottom-nav">
      <router-link
        v-for="item in navItems"
        :key="item.path"
        :to="item.path"
        class="nav-item"
        :class="{ active: isActive(item.path) }"
      >
        <div class="nav-icon" v-html="item.icon"></div>
        <span class="nav-label">{{ item.label }}</span>
        <div class="nav-dot" v-show="isActive(item.path)"></div>
      </router-link>
    </nav>
  </transition>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()

const hiddenPaths = ['/login', '/chat', '/result']
const showNav = computed(() => !hiddenPaths.includes(route.path))

const navItems = [
  {
    path: '/',
    label: '首页',
    icon: '<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M15 21v-8a1 1 0 0 0-1-1h-4a1 1 0 0 0-1 1v8"/><path d="M3 10a2 2 0 0 1 .709-1.528l7-5.999a2 2 0 0 1 2.582 0l7 5.999A2 2 0 0 1 21 10v9a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/></svg>'
  },
  {
    path: '/records',
    label: '记录',
    icon: '<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 8v4l3 3"/><circle cx="12" cy="12" r="10"/></svg>'
  },
  {
    path: '/profile',
    label: '我的',
    icon: '<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M19 21v-2a4 4 0 0 0-4-4H9a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>'
  }
]

function isActive(path: string) {
  if (path === '/') return route.path === '/'
  return route.path.startsWith(path)
}

// ========== 收起/展开逻辑 ==========
const isNavHidden = ref(false)
let lastScrollY = 0
let hideTimer: ReturnType<typeof setTimeout> | null = null

function onScroll() {
  const currentY = window.scrollY
  const delta = currentY - lastScrollY

  if (delta > 10 && currentY > 80) {
    isNavHidden.value = true
  } else if (delta < -5) {
    isNavHidden.value = false
  }

  lastScrollY = currentY

  if (hideTimer) clearTimeout(hideTimer)
  hideTimer = setTimeout(() => {
    isNavHidden.value = false
  }, 3000)
}

onMounted(() => {
  window.addEventListener('scroll', onScroll, { passive: true })
})

onUnmounted(() => {
  window.removeEventListener('scroll', onScroll)
  if (hideTimer) clearTimeout(hideTimer)
})
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
  from { opacity: 1; transform: translateY(0); }
  to { opacity: 0; transform: translateY(-10px); }
}

/* Floating pill bottom nav */
.bottom-nav {
  position: fixed;
  bottom: 24px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 8px 16px;
  background: rgba(11, 15, 16, 0.92);
  backdrop-filter: blur(24px);
  -webkit-backdrop-filter: blur(24px);
  border-radius: 100px;
  z-index: 100;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
  border: 1px solid rgba(255, 255, 255, 0.08);
  transition: transform 0.35s cubic-bezier(0.22, 1, 0.36, 1);
}

/* Nav slide transition */
.nav-slide-enter-active {
  transition: transform 0.35s cubic-bezier(0.22, 1, 0.36, 1), opacity 0.25s ease;
}

.nav-slide-leave-active {
  transition: transform 0.3s ease-in, opacity 0.2s ease;
}

.nav-slide-enter-from {
  transform: translateX(-50%) translateY(120px);
  opacity: 0;
}

.nav-slide-leave-to {
  transform: translateX(-50%) translateY(120px);
  opacity: 0;
}

.nav-item {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  padding: 8px 20px;
  border-radius: 100px;
  text-decoration: none;
  transition: all 0.3s cubic-bezier(0.22, 1, 0.36, 1);

  &.active {
    .nav-icon { color: #22d3ee; }
    .nav-label { color: #22d3ee; }
  }

  &:not(.active) {
    .nav-icon { color: rgba(255, 255, 255, 0.4); }
    .nav-label { color: rgba(255, 255, 255, 0.4); }
  }

  &:active {
    transform: scale(0.92);
  }
}

.nav-icon {
  line-height: 0;
  transition: color 0.3s;
}

.nav-label {
  font-size: 10px;
  font-weight: 600;
  letter-spacing: 0.05em;
  transition: color 0.3s;
}

.nav-dot {
  position: absolute;
  bottom: 2px;
  left: 50%;
  transform: translateX(-50%);
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: #22d3ee;
}
</style>

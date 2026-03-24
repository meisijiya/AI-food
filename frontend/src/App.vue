<template>
  <router-view v-slot="{ Component }">
    <transition name="sanctuary-fade" mode="out-in">
      <component :is="Component" />
    </transition>
  </router-view>

  <!-- Wrapper: controls overall position -->
  <div v-show="showNav" class="nav-root" :class="{ collapsed: isNavHidden }">
    <!-- Floating pill nav -->
    <transition name="nav-pop">
      <div v-show="!isNavHidden" class="nav-bar">
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

        <!-- Toggle button inside nav, right edge -->
        <button class="nav-toggle-inner" @click.stop="isNavHidden = true">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="m6 9 6 6 6-6"/></svg>
        </button>
      </div>
    </transition>

    <!-- Collapsed toggle tab — peeks from bottom -->
    <button v-show="isNavHidden" class="nav-toggle-tab" @click="isNavHidden = false">
      <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="m18 15-6-6-6 6"/></svg>
    </button>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()

const hiddenPaths = ['/login', '/chat', '/result', '/share']
const showNav = computed(() => !hiddenPaths.some(p => route.path.startsWith(p)))

const navItems = [
  {
    path: '/',
    label: '首页',
    icon: '<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M15 21v-8a1 1 0 0 0-1-1h-4a1 1 0 0 0-1 1v8"/><path d="M3 10a2 2 0 0 1 .709-1.528l7-5.999a2 2 0 0 1 2.582 0l7 5.999A2 2 0 0 1 21 10v9a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/></svg>'
  },
  {
    path: '/feed',
    label: '大厅',
    icon: '<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect width="7" height="7" x="3" y="3" rx="1"/><rect width="7" height="7" x="14" y="3" rx="1"/><rect width="7" height="7" x="14" y="14" rx="1"/><rect width="7" height="7" x="3" y="14" rx="1"/></svg>'
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

const isNavHidden = ref(false)

// ========== 滚动到底部自动收起 ==========
function isAtBottom(el: Element): boolean {
  const threshold = 40
  if (el === document.documentElement || el === document.body) {
    const scrollH = document.documentElement.scrollHeight
    const scrollT = window.scrollY || document.documentElement.scrollTop
    const clientH = window.innerHeight
    return scrollT + clientH >= scrollH - threshold
  }
  const scrollH = el.scrollHeight
  const scrollT = (el as HTMLElement).scrollTop
  const clientH = el.clientHeight
  return scrollT + clientH >= scrollH - threshold
}

function onScroll(e: Event) {
  const target = e.target as HTMLElement
  if (!target) return
  if (isAtBottom(target)) {
    isNavHidden.value = true
  } else {
    isNavHidden.value = false
  }
}

onMounted(() => {
  // window 级别滚动
  window.addEventListener('scroll', onScroll, { passive: true })
  // capture 阶段捕获所有子元素的 scroll 事件
  document.addEventListener('scroll', onScroll, { passive: true, capture: true })
})

onUnmounted(() => {
  window.removeEventListener('scroll', onScroll)
  document.removeEventListener('scroll', onScroll, true)
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

/* ============================================
   Nav root — positions the whole system
   ============================================ */

.nav-root {
  position: fixed;
  bottom: 24px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 100;
  transition: transform 0.35s cubic-bezier(0.22, 1, 0.36, 1);

  &.collapsed {
    // Tab peeks just above bottom edge
    bottom: 0;
    transform: translateX(-50%);
  }
}

/* ============================================
   Nav bar — the floating pill
   ============================================ */

.nav-bar {
  position: relative;
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 8px 8px 8px 16px;
  background: rgba(11, 15, 16, 0.92);
  backdrop-filter: blur(24px);
  -webkit-backdrop-filter: blur(24px);
  border-radius: 100px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
  border: 1px solid rgba(255, 255, 255, 0.08);
}

.nav-pop-enter-active {
  transition: all 0.3s cubic-bezier(0.22, 1, 0.36, 1);
}

.nav-pop-leave-active {
  transition: all 0.25s ease-in;
}

.nav-pop-enter-from {
  opacity: 0;
  transform: translateY(40px) scale(0.95);
}

.nav-pop-leave-to {
  opacity: 0;
  transform: translateY(40px) scale(0.95);
}

/* ============================================
   Nav items
   ============================================ */

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

/* ============================================
   Toggle button — inside nav right edge
   ============================================ */

.nav-toggle-inner {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: none;
  margin-left: 6px;
  margin-right: 2px;
  background: rgba(255, 255, 255, 0.08);
  color: rgba(255, 255, 255, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  flex-shrink: 0;
  transition: all 0.2s;

  &:hover {
    background: rgba(255, 255, 255, 0.14);
    color: rgba(255, 255, 255, 0.7);
  }

  &:active {
    transform: scale(0.88);
  }
}

/* ============================================
   Collapsed toggle tab — peeks from bottom
   ============================================ */

.nav-toggle-tab {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 28px;
  margin: 0 auto;
  border: none;
  border-radius: 12px 12px 0 0;
  background: rgba(11, 15, 16, 0.85);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  color: rgba(255, 255, 255, 0.5);
  cursor: pointer;
  box-shadow: 0 -2px 12px rgba(0, 0, 0, 0.15);
  transition: all 0.2s;

  &:hover {
    color: rgba(255, 255, 255, 0.8);
    background: rgba(11, 15, 16, 0.95);
  }

  &:active {
    transform: scale(0.92);
  }
}
</style>

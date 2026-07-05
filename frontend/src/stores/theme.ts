import { defineStore } from 'pinia'
import { ref, watch } from 'vue'

export type ThemeMode = 'light' | 'dark' | 'system'

const STORAGE_KEY = 'aifood-theme'

function applyTheme(mode: ThemeMode) {
  const html = document.documentElement
  if (mode === 'system') {
    html.removeAttribute('data-theme')
  } else {
    html.setAttribute('data-theme', mode)
  }
}

function loadStoredTheme(): ThemeMode {
  try {
    const stored = localStorage.getItem(STORAGE_KEY)
    if (stored === 'light' || stored === 'dark' || stored === 'system') {
      return stored
    }
  } catch {
    /* localStorage unavailable */
  }
  return 'system'
}

export const useThemeStore = defineStore('theme', () => {
  const theme = ref<ThemeMode>(loadStoredTheme())

  function setTheme(next: ThemeMode) {
    theme.value = next
    try {
      localStorage.setItem(STORAGE_KEY, next)
    } catch {
      /* localStorage unavailable */
    }
  }

  function toggleTheme() {
    if (theme.value === 'light') setTheme('dark')
    else setTheme('light')
  }

  // 应用当前主题到 <html data-theme>
  watch(
    theme,
    (mode) => applyTheme(mode),
    { immediate: true }
  )

  // 系统主题切换时，如果当前是 system 模式，<html> 已经无 data-theme，
  // CSS media query 自动响应，无需额外处理。

  return { theme, setTheme, toggleTheme }
})
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import { useThemeStore } from './stores/theme'
import '@vant/touch-emulator'
import 'vant/es/dialog/style'
import 'vant/es/image-preview/style'
import './assets/styles/main.scss'

const app = createApp(App)

app.use(createPinia())
app.use(router)

// 主题 store: 立即初始化以设置 <html data-theme> 属性（避免 FOUC）
useThemeStore()

app.mount('#app')

import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import '@vant/touch-emulator'
import 'vant/es/dialog/style'
import 'vant/es/image-preview/style'
import './assets/styles/main.scss'

const app = createApp(App)

app.use(createPinia())
app.use(router)

app.mount('#app')
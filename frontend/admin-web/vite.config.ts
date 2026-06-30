import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

// ponytail: base='/admin/' 让 build 出的 index.html 里 <script src="/admin/assets/...">
// 否则 asset 用绝对路径 /assets/...,与公网 /admin/ 前缀冲突导致 502
export default defineConfig({
  base: '/admin/',
  plugins: [vue()],
  resolve: { alias: { '@': path.resolve(__dirname, 'src') } },
  server: {
    port: 5174,
    host: '0.0.0.0',
    proxy: {
      '/admin/api': { target: 'http://localhost:8081', changeOrigin: true }
    }
  }
})
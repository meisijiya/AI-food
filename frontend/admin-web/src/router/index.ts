import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'

// ponytail: stub router — pages will be added in subsequent tasks
const routes: RouteRecordRaw[] = [
  { path: '/', name: 'home', component: () => import('@/views/Home.vue') }
]

export default createRouter({
  history: createWebHistory(),
  routes
})
// 路由：登录页 + 后台布局（含 8 个业务子路由）；未登录强制跳转 /login
import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory('/'),
  routes: [
    { path: '/login', name: 'login', component: () => import('@/views/login/index.vue') },
    {
      path: '/',
      component: () => import('@/layouts/DefaultLayout.vue'),
      redirect: '/dashboard',
      children: [
        { path: 'dashboard', name: 'dashboard', component: () => import('@/views/dashboard/index.vue'), meta: { title: 'Dashboard' } },
        { path: 'user', name: 'user', component: () => import('@/views/user/index.vue'), meta: { title: '用户管理' } },
        { path: 'conversation', name: 'conversation', component: () => import('@/views/conversation/index.vue'), meta: { title: 'AI 对话' } },
        { path: 'token-usage', name: 'token-usage', component: () => import('@/views/token-usage/index.vue'), meta: { title: 'Token 用量' } },
        { path: 'token-quota', name: 'token-quota', component: () => import('@/views/token-quota/index.vue'), meta: { title: 'Token 限额' } },
        { path: 'model', name: 'model', component: () => import('@/views/model/index.vue'), meta: { title: '模型管理' } },
        { path: 'recommendation', name: 'recommendation', component: () => import('@/views/recommendation/index.vue'), meta: { title: '推荐记录' } },
        { path: 'monitor', name: 'monitor', component: () => import('@/views/monitor/index.vue'), meta: { title: '系统监控' } },
        { path: 'audit-log', name: 'audit-log', component: () => import('@/views/audit-log/index.vue'), meta: { title: '操作日志' } }
      ]
    }
  ]
})

// 路由守卫：未登录访问受保护页面 → /login
router.beforeEach((to, _from, next) => {
  const token = localStorage.getItem('admin_token')
  if (to.name !== 'login' && !token) next({ name: 'login' })
  else next()
})

export default router

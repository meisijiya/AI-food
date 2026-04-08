import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    name: 'Home',
    component: () => import('@/views/Home.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/chat',
    name: 'Chat',
    component: () => import('@/views/Chat.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/result',
    name: 'Result',
    component: () => import('@/views/Result.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/records',
    name: 'Records',
    component: () => import('@/views/Records.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/records/:sessionId',
    name: 'RecordDetail',
    component: () => import('@/views/RecordDetail.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/profile',
    name: 'Profile',
    component: () => import('@/views/Profile.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/feed',
    name: 'Feed',
    component: () => import('@/views/Feed.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/feed/:postId',
    name: 'FeedDetail',
    component: () => import('@/views/FeedDetail.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/follow',
    name: 'FollowList',
    component: () => import('@/views/FollowList.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/chat-list',
    name: 'ChatList',
    component: () => import('@/views/ChatList.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/chat-room',
    name: 'ChatRoom',
    component: () => import('@/views/ChatRoom.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/contacts',
    name: 'Contacts',
    component: () => import('@/views/Contacts.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/user-search',
    name: 'UserSearch',
    component: () => import('@/views/UserSearch.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/friends',
    name: 'Friends',
    component: () => import('@/views/Friends.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/match',
    name: 'Match',
    component: () => import('@/views/Match.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/profile-edit',
    name: 'ProfileEdit',
    component: () => import('@/views/ProfileEdit.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/notifications',
    name: 'Notifications',
    component: () => import('@/views/Notifications.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/share/:token',
    name: 'share',
    component: () => import('@/views/Share.vue'),
    meta: { requiresAuth: false }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  const token = localStorage.getItem('token')

  if (to.meta.requiresAuth !== false && !token) {
    return { path: '/login' }
  }

  if (to.path === '/login' && token) {
    return { path: '/' }
  }
})

export default router

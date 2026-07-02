<script setup lang="ts">
/**
 * 登录页 — 左侧品牌叙事(渐变 + SVG 装饰)/ 右侧表单
 * 功能沿旧:POST /admin/api/login → token + adminUser → /dashboard
 * ponytail: 响应式断点仅 1 条(@media max-width:768px 单列,品牌面板隐藏)
 */
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { login } from '@/api/auth'

const router = useRouter()
const userStore = useUserStore()
const form = reactive({ username: '', password: '' })
const loading = ref(false)

/** 提交登录;空白校验 → /admin/api/login → 写 store → 跳转 dashboard */
async function onSubmit(): Promise<void> {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  loading.value = true
  try {
    const res: any = await login(form)
    userStore.setLogin(res.data.token, res.data.adminUser)
    ElMessage.success('登录成功')
    router.push('/dashboard')
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <!-- 左:品牌面板 -->
    <section class="brand-panel">
      <div class="brand-inner">
        <div class="brand-logo">
          <span class="brand-icon">🍜</span>
          <span class="brand-name">AI-Food</span>
        </div>
        <h1 class="brand-tagline">智能美食<br />推荐管理后台</h1>
        <p class="brand-desc">统一管理用户、对话、Token 用量、推荐记录与系统监控</p>
        <svg class="brand-art" viewBox="0 0 400 200" preserveAspectRatio="none" aria-hidden="true">
          <path d="M0,160 C100,100 200,180 300,120 C350,90 380,80 400,70 L400,200 L0,200 Z"
                fill="rgba(255,255,255,0.08)" />
          <circle cx="80" cy="60" r="30" fill="rgba(255,255,255,0.12)" />
          <circle cx="320" cy="40" r="20" fill="rgba(255,255,255,0.16)" />
        </svg>
      </div>
    </section>

    <!-- 右:表单 -->
    <section class="form-panel">
      <div class="form-inner">
        <h2 class="form-title">欢迎登录</h2>
        <p class="form-sub">使用管理员账号继续</p>
        <el-form @submit.prevent="onSubmit">
          <el-form-item>
            <el-input v-model="form.username" size="large" placeholder="用户名" prefix-icon="User" />
          </el-form-item>
          <el-form-item>
            <el-input v-model="form.password" type="password" show-password
                     size="large" placeholder="密码" prefix-icon="Lock" />
          </el-form-item>
          <el-button type="primary" size="large" :loading="loading"
                     class="submit" @click="onSubmit">登录</el-button>
        </el-form>
        <p class="form-footer">© 2026 AI-Food · 管理后台</p>
      </div>
    </section>
  </div>
</template>

<style scoped>
.login-page {
  height: 100vh;
  display: grid;
  grid-template-columns: 1fr 1fr;
}
@media (max-width: 768px) {
  .login-page { grid-template-columns: 1fr; }
  .brand-panel { display: none; }
}

/* === 品牌面板 === */
.brand-panel {
  background: linear-gradient(135deg, #3b6cf0 0%, #2c4fb8 100%);
  color: white; position: relative; overflow: hidden;
}
.brand-inner {
  position: relative; z-index: 1; height: 100%; padding: 64px;
  display: flex; flex-direction: column; justify-content: center; gap: 24px;
}
.brand-logo { display: flex; align-items: center; gap: 12px; font-size: 20px; font-weight: 600; }
.brand-icon { font-size: 32px; }
.brand-tagline {
  font-size: 40px; line-height: 1.25; font-weight: 700;
  margin: 0; letter-spacing: -0.5px;
}
.brand-desc {
  font-size: 15px; opacity: 0.85; max-width: 360px; line-height: 1.6;
}
.brand-art { position: absolute; inset: 0; width: 100%; height: 100%; pointer-events: none; }

/* === 表单面板 === */
.form-panel {
  background: var(--color-bg-surface);
  display: flex; align-items: center; justify-content: center;
}
.form-inner { width: 100%; max-width: 380px; padding: 32px; }
.form-title {
  font-size: 28px; font-weight: 700;
  color: var(--color-text-primary); margin: 0 0 8px; letter-spacing: -0.5px;
}
.form-sub { color: var(--color-text-secondary); font-size: 14px; margin: 0 0 32px; }
.submit {
  width: 100%; height: 44px; border-radius: var(--radius-md);
  font-size: 15px; font-weight: 600; margin-top: 8px;
}
.form-footer {
  margin-top: 32px; text-align: center;
  color: var(--color-text-tertiary); font-size: 12px;
}
</style>
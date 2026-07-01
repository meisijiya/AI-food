// 认证相关接口 — ponytail: 用 bareApi 走裸实例,
// 即便主实例的拦截器有 bug,这些端点自身 401 也不会触发死循环
import { bareApi } from './http'

export const authApi = {
  sendCode: (email: string) => bareApi.post('/auth/send-code', { email }) as any,
  register: (data: { password: string; email: string; code: string }) =>
    bareApi.post('/auth/register', data) as any,
  login: (data: { email: string; password: string }) => bareApi.post('/auth/login', data) as any,
  logout: () => bareApi.post('/auth/logout') as any
}

# Task 20 Report — admin-web axios + Pinia + Router

## 1. 需求简述

实现 `admin-web` 后台前端的三件套：axios 请求封装（含 token 注入 / 401/403/5xx 统一处理）、Pinia user store（登录态持久化）、Vue Router（含 8 个业务子路由 + 登录页 + 未登录守卫）。配套创建 9 个占位 view 和 1 个简化版 `DefaultLayout`。

## 2. 实现总结

### 新增文件（11 个）

| 文件 | 行数 | 说明 |
|---|---|---|
| `src/api/request.ts` | 38 | axios 实例：baseURL `/admin/api`、Bearer token 注入、401 自动跳登录 |
| `src/stores/user.ts` | 28 | Pinia setup-style store，token + adminUser 持久化到 localStorage |
| `src/router/index.ts` | 31 | `createWebHistory('/')`、login + 8 子路由、`beforeEach` 守卫 |
| `src/layouts/DefaultLayout.vue` | 36 | 占位布局（顶部栏 + `<router-view>` + 退出登录按钮） |
| `src/views/{login,dashboard,user,conversation,token-usage,model,recommendation,monitor,audit-log}/index.vue` | 9 × 3 | 单行占位 `<template>` |

### 修改文件

- `src/router/index.ts`（替换原 Home.vue 单路由 stub → 完整路由表）

### Build 验证

`npx vite build` → `✓ built in 6.47s`，无 TypeScript / Rollup 错误。Warning 仅来自 Sass legacy API + 1.18 MB bundle 体积提示，与本次任务无关。

### Commit

```
f3306d8 feat(admin-web): add axios, pinia, router with placeholder views
```

### 已知遗留 / 注意事项

1. **旧 `views/Home.vue` 仍存在但已无引用** —— 后续 Task 21+ 可清理删除，本次未动以保持 diff 最小。
2. **`useUserStore.adminUser` 类型为 `any`**（`ponytail:` 注释已标注）—— 等后端 admin-user 模型稳定后收敛。
3. **`DefaultLayout` 是简化版占位**（仅顶部栏 + 退出按钮）—— 后续 Task 21+ 会接入侧边栏 + 菜单。
4. **守卫只校验 token 存在性，不校验角色 / 过期** —— 后端 `/admin/api/*` 接口返回 401 时由 `request.ts` 拦截器统一兜底。
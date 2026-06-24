# Knife4j / Swagger 接口文档使用指南

> AI-food 后端 API 文档已完整接入 [Knife4j 4.4.0](https://doc.xiaominfo.com/)，访问 `/doc.html` 即可。

---

## 一、访问入口

启动后端后（`mvn spring-boot:run` 或 Docker），访问：

| 入口 | URL | 说明 |
|------|-----|------|
| **Knife4j UI** | http://localhost:8080/doc.html | 推荐使用 — 美化版 Swagger |
| Swagger UI（原生）| http://localhost:8080/swagger-ui.html | 备用 |
| OpenAPI JSON | http://localhost:8080/v3/api-docs | 用于 Postman 导入 |
| 健康检查 | http://localhost:8080/actuator/health | 部署探针 |

---

## 二、接口分组（菜单）

| 分组 | 接口前缀 | 数量 | 说明 |
|------|---------|------|------|
| 0-所有接口 | `/api/**` | ~60 | 全部 |
| 1-认证与用户 | `/api/auth/**`, `/api/user/**`, `/api/guest/**` | ~12 | 注册/登录/验证码/用户信息 |
| 2-AI 服务 | `/api/ai/**`, `/api/recommendation/**` | ~6 | LLM 聊天/推荐/相似度 |
| 3-对话管理 | `/api/conversation/**` | ~8 | 会话状态/参数收集 |
| 4-Feed 推荐 | `/api/feed/**`, `/api/record/**` | ~6 | 推荐发布/Feed 流/热榜 |
| 5-社交 | `/api/like/**`, `/api/follow/**`, `/api/notification/**`, `/api/share/**` | ~12 | 点赞/关注/通知/分享 |
| 6-聊天与文件 | `/api/chat/**`, `/api/upload/**` | ~10 | WebSocket 聊天/文件上传 |
| 7-Bloom 过滤器 | `/api/bloom/**` | ~4 | 用户相似度/去重 |

---

## 三、5 分钟上手

### Step 1：注册账号

打开 `1-认证与用户 → /api/auth/register`：

```json
{
  "email": "your-email@example.com",
  "code": "123456",          // 先调 send-code 拿
  "password": "YourPass123"
}
```

成功返回 `LoginResponse`：

```json
{
  "code": 200,
  "message": "注册成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1...",  // ← JWT
    "userId": 10001
  }
}
```

### Step 2：把 token 填到 Knife4j

点击右上角 **🔓 Authorize** 按钮，弹窗输入：

```
Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6Ikp...
```

（"Bearer " 前缀必须有，**+ 空格 + token**）

之后所有需要鉴权的接口会自动带上 `Authorization` header。

### Step 3：调用任意接口

比如「对推荐点赞」—— `5-社交 → /api/like/{postId}`：

```
POST /api/like/10001
Authorization: Bearer eyJhbGc...  ← 自动带
```

返回：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "liked": true,
    "likeCount": 42,
    "queued": true
  }
}
```

---

## 四、调试技巧

### 1. 在线测试

每个接口右侧有「调试」按钮，**直接在线发请求**——不用 Postman。

### 2. 复制为 cURL

接口页右上角 `复制 cURL` —— 直接粘到终端跑。

### 3. 复制为 Postman

导出的 cURL 粘到 Postman：Import → Paste Raw Text → 自动生成请求。

### 4. 全局参数

Knife4j 支持「全局参数」—— 比如设 `Authorization`，所有请求自动带。

### 5. 离线导出

`doc.html` 右上角「下载」→ 选择 OpenAPI 2.0 / 3.0 → 导出 JSON / YAML。

---

## 五、接口规范

### 统一响应格式（ApiResponse）

```json
{
  "code": 200,           // 业务状态码（200=成功）
  "message": "success",  // 提示信息
  "data": { ... }        // 业务数据
}
```

### 错误码

| code | 含义 |
|------|------|
| 200 | 成功 |
| 400 | 业务错误（参数错误、权限不足）|
| 401 | 未登录 / token 失效 |
| 403 | 权限不足 |
| 500 | 服务器错误 |

### 时间格式

所有时间字段：`yyyy-MM-dd HH:mm:ss`（Asia/Shanghai 时区）

---

## 六、典型接口清单

### 1. 注册流程

```
POST /api/auth/send-code       发送邮箱验证码
POST /api/auth/register        注册 + 返回 token
POST /api/auth/login           登录 + 返回 token
POST /api/auth/logout          退出登录
```

### 2. AI 推荐流程

```
POST /api/conversation/start        开始会话
POST /api/conversation/{id}/answer  用户回答（LLM 验证）
POST /api/recommendation/generate   7 个参数收齐后生成推荐
POST /api/feed/publish              发布到 Feed
POST /api/like/{postId}             点赞
GET  /api/like/hot-posts            看热榜
```

### 3. 社交流程

```
POST /api/follow/{userId}     关注
GET  /api/follow/list         关注列表
POST /api/share/feed          分享
GET  /api/notification/list   通知列表
```

---

## 七、生产环境

⚠️ 生产环境 Knife4j **建议关闭**——接口文档是给开发者看的，不是给用户的。

修改 `application-prod.yml`：

```yaml
knife4j:
  enable: false
springdoc:
  api-docs:
    enabled: false
  swagger-ui:
    enabled: false
```

或者加 basic auth 保护：

```yaml
knife4j:
  basic:
    enable: true
    username: admin
    password: ${KNIFE4J_PASSWORD}
```

---

## 八、相关文件

```
backend/src/main/
├── java/com/ai/food/config/
│   └── Knife4jConfig.java            ← 7 个分组 + 鉴权配置
└── resources/
    ├── application.yml               ← Springdoc + Knife4j 通用配置
    ├── application-dev.yml           ← 开发环境
    └── application-prod.yml          ← 生产环境
```

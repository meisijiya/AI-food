# SessionId 安全加固计划

## Goal
修复项目中所有 10 个 sessionId 相关安全漏洞，全面加固项目安全。

## 审计发现

| # | 严重度 | 问题 | 文件位置 |
|---|--------|------|----------|
| 1 | CRITICAL | IDOR — 多数 `{sessionId}` 端点无所有权验证 | RecordController, ConversationController, FeedController, ShareController |
| 2 | CRITICAL | WebSocket `/ws/**` 完全未认证 (permitAll) | SecurityConfig.java:35 |
| 3 | HIGH | sessionId 暴露在 URL 查询参数 → 浏览器历史/Referer | Feed.vue:522, Result.vue:241 |
| 4 | HIGH | sessionId 暴露在公开 Feed API 响应 | FeedService.java:572 |
| 5 | HIGH | CORS 配置通配符，过于宽松 | SecurityConfig.java:49-53 |
| 6 | MEDIUM | sessionId 出现在 INFO 级别后端日志 | 多个 Controller/Service 文件 |
| 7 | MEDIUM | WebSocket 前端 console.log 暴露完整 URL | websocket/index.ts:54 |
| 8 | MEDIUM | 无任何限速 | 全项目 |
| 9 | MEDIUM | 无会话过期机制 — 已完成 session 永远可读 | ConversationService, DB schema |
| 10 | MEDIUM | Swagger 在生产环境 permitAll | SecurityConfig.java:37-38 |

---

## Phase 1: 修复 IDOR 漏洞 — 端点所有权验证

**目标：** 为所有 `{sessionId}` 端点添加用户所有权验证，确保用户只能访问自己的 session。

**方法：** 在 Service 层添加通用的 `validateSessionOwnership(sessionId, userId)` 方法，Controller 层在调用 service 方法前获取 `getCurrentUserId()` 并验证。

### 需要修改的文件：

**backend/src/main/java/com/ai/food/controller/RecordController.java**
- `GET /detail/{sessionId}` (line 53-57): 添加 `recordService.validateSessionOwnership(sessionId, userId)` 校验
- `DELETE /delete/{sessionId}` (line 59-68): 已有 `getCurrentUserId()` 但未用于校验，添加校验
- `DELETE /batch-delete` (line 70-79): 添加逐个校验
- `PUT /photo/{sessionId}` (line 81-97): 已有 userId 但未校验，添加校验
- `DELETE /photo/{sessionId}` (line 99-111): 已有 userId 但未校验，添加校验
- `PUT /comment/{sessionId}` (line 113-119): 添加 `getCurrentUserId()` + 校验

**backend/src/main/java/com/ai/food/controller/ConversationController.java**
- `GET /status/{sessionId}` (line 65-90): 添加 `getCurrentUserId()` + `conversationService.validateOwnership(sessionId, userId)` 校验
- `POST /complete/{sessionId}` (line 92-111): 同上
- `DELETE /cancel/{sessionId}` (line 113-127): 同上
- `GET /history/{sessionId}` (line 129-154): 同上

**backend/src/main/java/com/ai/food/service/record/RecordService.java**
- 添加 `validateSessionOwnership(String sessionId, Long userId)` 方法，查询 `conversation_session` 表验证 `user_id` 匹配

**backend/src/main/java/com/ai/food/service/conversation/ConversationService.java**
- 添加 `validateOwnership(String sessionId, Long userId)` 方法

### 修改后的 Controller 模式：
```java
@DeleteMapping("/delete/{sessionId}")
public ApiResponse<Void> deleteRecord(@PathVariable String sessionId) {
    Long userId = getCurrentUserId();
    recordService.validateSessionOwnership(sessionId, userId); // 新增
    // ... existing logic
}
```

---

## Phase 2: 修复 WebSocket 认证

**目标：** 让 WebSocket 连接经过 JWT 认证，或至少验证 session 所有权。

**方法：** 在 WebSocketHandler 中添加 token 验证，同时修改 SecurityConfig。

### 方案：
1. **修改 SecurityConfig.java**: 将 `/ws/**` 从 `permitAll` 改为 `authenticated`（但 WebSocket 握手不走 JWT filter，需要在 handler 层验证）
2. **修改 WebSocketConfig.java**: 注册 HandshakeInterceptor，在握手阶段验证 JWT token
3. **修改 ConversationWebSocketHandler.java**: 从 interceptor 中获取 userId，验证 session 所有权
4. **修改 frontend websocket/index.ts**: 连接时在 URL query 中附带 token（`/ws/conversation/{sessionId}?token=xxx`）

### 需要修改的文件：
- `backend/src/main/java/com/ai/food/config/SecurityConfig.java` — 移除 `.requestMatchers("/ws/**").permitAll()`
- `backend/src/main/java/com/ai/food/config/WebSocketConfig.java` — 添加 HandshakeInterceptor
- `backend/src/main/java/com/ai/food/websocket/ConversationWebSocketHandler.java` — `afterConnectionEstablished` 中验证 ownership
- `backend/src/main/java/com/ai/food/util/JwtUtil.java` — 添加 `getUserIdFromToken(token)` 方法供 interceptor 使用
- `frontend/src/websocket/index.ts` — 连接 URL 附带 `?token=xxx`

---

## Phase 3: 修复 sessionId URL 暴露

**目标：** 避免 sessionId 出现在浏览器 URL 中（history / Referer 风险）。

**方法：** 将 Feed.vue 的 `router.push('/result?sessionId=xxx')` 改为通过 Pinia store 传递 sessionId，不走 URL。

### 需要修改的文件：
- `frontend/src/stores/chat.ts` — 添加 `pendingSessionIdFromFeed` 字段
- `frontend/src/views/Feed.vue` (line 522) — 改为 `chatStore.pendingSessionIdFromFeed = pendingSessionId.value; router.push('/result')`
- `frontend/src/views/Result.vue` (line 241-273) — 改为从 `chatStore.pendingSessionIdFromFeed` 读取，读取后立即清除

---

## Phase 4: 移除公开 Feed 中的 sessionId

**目标：** 公开 Feed 列表 API 不再返回 sessionId，防止其他用户看到。

**方法：** 在 FeedService 构建 postMap 时移除 sessionId 字段。FeedDetail 通过 id 获取时仍返回（因为有 ownership 验证）。

### 需要修改的文件：
- `backend/src/main/java/com/ai/food/service/feed/FeedService.java` (line 572) — 从 map 中移除 `"sessionId"` 字段

---

## Phase 5: 收紧 CORS 配置

**目标：** 限制 CORS 允许的 origin、method 和 header。

**方法：** 从通配符改为明确的白名单。

### 需要修改的文件：
- `backend/src/main/java/com/ai/food/config/SecurityConfig.java` (line 47-59)
- 将 `addAllowedOriginPattern("*")` 改为具体的 origin 列表
- 将 `addAllowedMethod("*")` 改为 `GET,POST,PUT,DELETE,OPTIONS`
- 将 `addAllowedHeader("*")` 改为 `Authorization,Content-Type`

---

## Phase 6: 清理后端日志中的 sessionId

**目标：** 将包含 sessionId 的 INFO 级别日志改为 DEBUG 或移除敏感部分。

### 需要修改的文件：
- `backend/src/main/java/com/ai/food/controller/ConversationController.java` — line 55 改为只记 userId
- `backend/src/main/java/com/ai/food/controller/RecordController.java` — line 61, 102 改为 DEBUG
- `backend/src/main/java/com/ai/food/service/record/RecordService.java` — line 93, 98, 137, 152, 169 改为 DEBUG
- `backend/src/main/java/com/ai/food/service/feed/FeedService.java` — line 94, 539 改为 DEBUG
- `backend/src/main/java/com/ai/food/service/conversation/ConversationService.java` — line 317 改为 DEBUG
- `backend/src/main/java/com/ai/food/service/share/ShareService.java` — line 52 改为 DEBUG

---

## Phase 7: 清理前端 console.log

**目标：** 移除生产环境中暴露敏感信息的 console.log。

### 需要修改的文件：
- `frontend/src/websocket/index.ts` (line 54) — 移除或改为占位符：`console.log('[WS] Connecting...')` 不包含 URL
- `frontend/src/websocket/chat.ts` — 审查并清理敏感日志

---

## Phase 8: 添加基础限速

**目标：** 为关键端点添加简单的内存限速。

**方法：** 创建一个 Spring Interceptor 实现简单的 IP 级限速（无需 Redis，内存滑动窗口即可满足小型项目需求）。

### 需要创建/修改的文件：
- 创建 `backend/src/main/java/com/ai/food/config/RateLimitInterceptor.java` — 内存限速拦截器
- 修改 `backend/src/main/java/com/ai/food/config/WebConfig.java` — 注册拦截器
- 对 `/api/auth/login`、`/api/auth/register`、`/api/conversation/start` 应用限速

---

## Phase 9: 添加会话过期机制

**目标：** 超过一定时间的已完成 session 无法再被读取。

**方法：** 在 validateOwnership / getRecordDetail 中检查 session 状态和创建时间。

### 需要修改的文件：
- `backend/src/main/java/com/ai/food/service/record/RecordService.java` — `getRecordDetail` 中检查 session 是否 active/completed，completed 超过 30 天不可读
- `backend/src/main/java/com/ai/food/service/conversation/ConversationService.java` — `validateOwnership` 同理

---

## Phase 10: 禁用生产环境的 Swagger

**目标：** 生产环境关闭 Swagger UI。

**方法：** 使用 `@Profile` 注解或 Spring 配置。

### 需要修改的文件：
- `backend/src/main/java/com/ai/food/config/SecurityConfig.java` — 条件化 permitAll：在 production profile 下 Swagger 端点需要认证
- 或创建 `application-prod.yml` 设置 `springdoc.api-docs.enabled=false`

---

## Verification

完成所有修改后：
1. `mvn compile` (或项目使用的构建命令) 验证后端编译通过
2. `npm run typecheck` 验证前端类型检查通过
3. 手动测试：确认 IDOR 不再可能（尝试用用户 A 的 token 访问用户 B 的 session，应返回 403）
4. 手动测试：WebSocket 连接不带 token 应被拒绝
5. 检查浏览器 URL 不再包含 sessionId 查询参数
6. 检查 Feed API 响应不再包含 sessionId

## Errors Encountered
_(will be updated during execution)_

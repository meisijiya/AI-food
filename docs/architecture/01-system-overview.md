# AI-Food 架构图与核心流程

> 5 个 Mermaid 图,涵盖:架构 / 用户注册登录 / AI 对话 7 参数 / Feed 点赞+热榜 / WebSocket 会话生命周期。
> 用于下次 session 接手时快速理解项目结构。
> 评估期:Phase 5 of P1-finish-system-usable deepwork。

---

## 1. 系统架构图

```mermaid
graph TB
    subgraph 用户端
        Browser[浏览器]
    end

    subgraph 部署 (sandbox)
        Vite[vite preview<br/>:5174]
        Nginx[cloud nginx<br/>:80]
    end

    subgraph 远程 (cloud)
        CAdmin[admin-server.jar<br/>:8081]
    end

    subgraph 本机 (sandbox)
        MApp[ai-food-app.jar<br/>:8080]
        CCommon[ai-food-common<br/>JwtService / Mapper / Entity]
    end

    subgraph 数据层
        MySQL[(MySQL 8.x<br/>127.0.0.1:13306<br/>via SSH tunnel)]
        Redis[(Redis<br/>127.0.0.1:6379)]
    end

    Browser -->|HTTP| Nginx
    Nginx -->|/admin/*| Vite
    Nginx -->|/admin/api/*| CAdmin
    Nginx -->|/api/*| MApp

    MApp --> CCommon
    CAdmin --> CCommon
    CCommon --> MySQL
    CCommon --> Redis
    MApp -->|WebSocket<br/>/ws/chat /ws/conversation| Browser
```

**关键点**:
- admin-server 与 ai-food-app 共享 ai-food-common(JwtService / Mapper / Entity)
- AI 对话走 WebSocket(/ws/conversation/{sessionId}),聊天走 WebSocket(/ws/chat)
- 实时数据(LIKE / 未读 / 在线)走 Redis,持久数据走 MySQL
- cloud nginx 通过 SSH 隧道访问 sandbox 端口(119.29.52.111 是公网 IP,沙箱在 NAT 后面)

---

## 2. 用户注册登录流程

```mermaid
sequenceDiagram
    autonumber
    participant U as 用户
    participant F as Frontend<br/>(api/auth)
    participant C as AuthController
    participant S as AuthService
    participant DB as MySQL<br/>sys_user
    participant R as Redis<br/>token:{userId}
    participant J as JwtService

    U->>F: 1. 填邮箱+密码
    F->>C: 2. POST /api/auth/login
    C->>S: 3. login(req)
    S->>DB: 4. SELECT * FROM sys_user WHERE email=?
    DB-->>S: 5. SysUser(含 bcrypt hash)
    S->>S: 6. BCryptPasswordEncoder.matches(req.password, user.password)
    alt 密码错误
        S-->>C: 7a. throw RuntimeException 401
        C-->>F: 8a. 401 "邮箱或密码错误"
    else 密码正确
        S->>J: 7b. generateToken(userId, username)
        J-->>S: 8b. JWT (signed with JWT_SECRET, 7天过期)
        S->>R: 9. EXPIRE token:{userId} (Redis, 3天TTL)
        S-->>C: 10. LoginResponse(token, user)
        C-->>F: 11. 200 + token
        F->>F: 12. 存 token (Pinia auth store)<br/>setToken(内存,不入 localStorage)
    end
```

**关键点**:
- 密码用 BCrypt 加密(stored as `$2a$10$...`)
- JWT token 默认 7 天过期(JWT_EXPIRATION=604800000 ms)
- **Redis token 缓存 3 天 TTL**(AuthService.renewTokenTtl 主动续期)
- **Token 安全**:存入 Pinia 内存,不入 localStorage(XSS 防护);后端在 cookie 备份(2.0 引入)
- 限流:Caffeine 缓存 60s 内同邮箱/同 IP 1 次(防暴力)

---

## 3. AI 对话 7 参数收集(核心业务流)

```mermaid
sequenceDiagram
    autonumber
    participant U as 用户
    participant W as WebSocket<br/>/ws/conversation/{sid}
    participant H as ConversationWebSocket<br/>Handler
    participant S as ConversationService
    participant PS as ConversationParamService
    participant AS as ConversationAiService
    participant AI as AiService<br/>(DeepSeek)
    participant DB as MySQL<br/>conversation_session<br/>qa_record<br/>collected_param
    participant R as Redis

    Note over U,DB: 用户在 App 发起 AI 对话
    U->>W: 1. open WS /ws/conversation/{sid}
    W->>H: 2. afterConnectionEstablished
    H->>S: 3. initializeConversation(sid)
    S->>DB: 4. SELECT conversation_session
    S->>H: 5. conversationStates.put(sid, state) (内存 ConcurrentHashMap)
    S-->>U: 6. question(time)

    loop 7 轮必选参数收集
        U->>W: 7. answer("晚上")
        W->>H: 8. handleText
        H->>S: 9. processAnswer(sid, "晚上", state)
        Note right of S: processAnswer 是 70 行状态机
        S->>PS: 10. allParamsCollected(state) / determineNextParam
        PS-->>S: 11. param=time 已收集 → next=location
        S->>AS: 12. generateAiResponse(param, answer, state)
        AS->>AI: 13. chat(prompt)
        AI-->>AS: 14. response text
        AS-->>S: 15. aiResponse
        S->>DB: 16. saveQaRecord + saveCollectedParam
        S-->>U: 17. question(location)
    end

    Note over S,DB: 7 必选参数(time/location/weather/<br/>mood/companion/budget/taste)收集完成
    S->>AS: 18. generateRecommendationMessage(sid, state)
    AS->>AI: 19. generateRecommendation(paramsJson)
    AI-->>AS: 20. 推荐 JSON (foodName + reason)
    AS->>DB: 21. saveRecommendationResult
    S->>R: 22. SET pending:recommend:{userId} (7天TTL)
    S-->>U: 23. recommend message

    opt 用户中断(快速发消息)
        U->>W: 24. interrupt: "等下..."
        H->>S: 25. canInterrupt() ? addPendingMessage : ignore
        Note right of S: interruptCount < 10 时允许多条 pending
        H->>S: 26. handleInterrupt(combined, state) 触发合并处理
        S->>AI: 27. chat(汇总 prompt)
        AI-->>S: 28. "你说话太快啦"
        S-->>U: 29. interrupt message
    end
```

**关键点**:
- **必选 7 参数**:time / location / weather / mood / companion / budget / taste(顺序敏感)
- **可选 3 参数**:restriction / preference / health(AI 自由发挥阶段补问)
- **状态机**:processAnswer 70 行本体留 facade(Oracle 修订),只 delegate 叶子调用
- **interrupted/interruptCount** 状态在 ConversationState,interruptCount >= 2 触发 AI 主动响应
- 推荐结果存 `recommendation_result` 表,7 天 TTL 在 Redis

---

## 4. Feed 点赞 + 热榜更新

```mermaid
sequenceDiagram
    autonumber
    participant U as 用户
    participant F as Frontend<br/>(api/feed)
    participant C as FeedController
    participant S as LikeService
    participant LS as LikeStreamProducer
    participant H as HeavyKeeperService
    participant R as Redis
    participant DB as MySQL<br/>feed_post<br/>feed_like<br/>admin_audit_log

    Note over U,DB: 用户点击点赞按钮
    U->>F: 1. POST /api/feed/like/{postId}
    F->>C: 2. likePost(postId)
    C->>S: 3. toggleLike(userId, postId)
    S->>R: 4. EVAL toggleLikeScript (Lua, atomic)
    Note right of S: SISMEMBER → SREM/DECR or SADD/INCR
    R-->>S: 5. ±1 (新点赞数)
    S-->>C: 6. isLiked, likeCount
    C-->>F: 7. 200 + result
    F->>U: 8. UI 立即更新(乐观)

    S->>LS: 9. emit StreamRecord (异步)
    LS->>R: 10. XADD stream:like:events
    Note over LS: 不阻塞主流程

    R-->>S: 11. LikeStreamConsumer poll (每 1s 轮询)
    S->>R: 12. XREAD batch
    S->>S: 13. 批量聚合 likes per post
    loop 每分钟 (HeavyKeeper 衰减)
        H->>H: 14. 衰减 ×0.9 (热度时间衰减)
        H->>R: 15. ZADD hk:like:decay +score
    end

    Note over S,DB: 定时刷盘到 MySQL (FLUSH_INTERVAL_MS=100ms)
    S->>DB: 16. UPDATE feed_post SET like_count=? (每 100ms 一次)
    S->>H: 17. checkAndRefreshHotRank (Top20)
    H->>R: 18. ZREVRANGE hk:like:decay 0 19
    H->>R: 19. SET feed:hot:details (Top20 JSON,**无 TTL** ⚠️ 永不过期)
```

**关键点**:
- **点赞原子性**:Redis Lua 脚本一次完成(查 / 增 / 减),不依赖事务
- **异步刷盘**:LikeStreamProducer → Redis Stream → LikeStreamConsumer 批量聚合,避免每次点赞都写 MySQL
- **热度算法**:HeavyKeeper(局部 LRU + Redis ZSet),每分钟 ×0.9 衰减
- **热榜缓存**:`feed:hot:details` 存 Top20 JSON,**⚠️ 无 TTL**(代码 bug,新点赞触发 refresh 但发布停止后永不刷新)

---

## 5. WebSocket 会话生命周期(Oracle 特别建议)

```mermaid
stateDiagram-v2
    [*] --> CONNECTING: open /ws/chat or /ws/conversation/{sid}
    CONNECTING --> AUTHENTICATING: TCP 握手完成
    AUTHENTICATING --> CONNECTED: JWT 验证通过
    AUTHENTICATING --> REJECTED: 401 invalid token
    REJECTED --> [*]: 连接关闭

    CONNECTED --> IDLE: 空闲(无消息)
    IDLE --> SENDING: 客户端发消息
    SENDING --> PROCESSING: 服务端处理
    PROCESSING --> RESPONDING: AI 响应生成
    RESPONDING --> IDLE: 消息发送完成

    SENDING --> INTERRUPTED: 客户端快速发 1+ 条
    Note right of INTERRUPTED: canInterrupt() = interruptCount < 10
    INTERRUPTED --> PROCESSING: handleInterrupt 合并处理
    PROCESSING --> RESPONDING: AI 汇总响应
    RESPONDING --> IDLE

    IDLE --> DISCONNECTED: 客户端 close
    IDLE --> DISCONNECTED: 网络中断
    DISCONNECTED --> RECONNECTING: 客户端自动重连
    RECONNECTING --> CONNECTING: 重连请求

    state PROCESSING {
        [*] --> HANDLER_DISPATCH
        HANDLER_DISPATCH --> AI_ASYNC: 异步 AI 调用
        AI_ASYNC --> HANDLER_DISPATCH: 回调
    }

    note right of PROCESSING
        aiExecutor(core=8, max=32, queue=200, CallerRunsPolicy)
        防止 AI 慢响应阻塞其他连接
    end note

    note left of DISCONNECTED
        服务端清理:
        - conversationStates.remove(state) (内存 map)
        - chat:online:{userId} expire(5min TTL)
        - 未读计数持久化到 Redis
    end note
```

**关键点**:
- **2 个 WebSocket**:ChatWebSocketHandler(231行) + ConversationWebSocketHandler(299行)
- **JWT 鉴权**:`HandshakeInterceptor` 在握手时验证 Bearer token,principal 为 userId(String)
- **状态机**:每个连接有 `ConversationState` (per-session) 或 `chat message` 流(per-conversation)
- **线程池隔离**:AI 慢响应走 `aiExecutor`(8 core / 32 max / 200 queue),不阻塞 socket I/O
- **断线重连**:前端(aiFood 移动端)自动重连 + JWT 重发
- **心跳**:ChatWebSocketHandler 接受 `ping` action 回 `pong`,**无 server-side 5s 超时**(代码未实现)

---

## 6. 总结

5 个图覆盖:
- **架构**:3 个 backend 模块 + 2 个数据层 + 2 个部署层
- **用户流程**:注册登录(鉴权 / 限流 / bcrypt)
- **核心业务**:AI 对话 7 参数(状态机 + WebSocket)
- **高频操作**:Feed 点赞(原子 Lua + Stream 异步 + HeavyKeeper 热度)
- **实时通信**:WebSocket 生命周期(连接 / 鉴权 / 心跳 / 断线重连)

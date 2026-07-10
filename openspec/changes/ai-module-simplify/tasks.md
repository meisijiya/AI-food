# Tasks: AI Module Simplify

> **Spec**: `docs/superpowers/specs/2026-07-07-ai-module-simplify-design.md` (v4.1)
> **19 步来源**: Spec §6
> **总 task**: 23 · 5 阶段 (A: 9 / B: 6 / C: 2 / D: 5 / E: 1)

---

## 阶段 A: AI 模块简化（9 tasks）

**入口**: Spec §1-§4 · 删除 dead code + 升级 prompt schema + Flyway V6
**出口**: `mvn -pl ai-food-app compile && mvn -pl admin-server compile` 通过

---

### A.1 升级 prompts/recommendation.txt 为 4 字段 JSON schema

- **Type**: impl
- **File**: `backend/ai-food-app/src/main/resources/prompts/recommendation.txt`
- **Dependencies**: —
- **Spec 引用**: §3.2.1
- **Action**: 替换为 4 字段 JSON schema：
  `{foodName, reason, category, flavorTags[]}`
  含完整中文 instruction + `{params}` 占位
- **Verify**:
  - command: `grep -c "flavorTags" backend/ai-food-app/src/main/resources/prompts/recommendation.txt`
  - expected: 输出 `1`
- **Risk**: R2（prompt 改后集成测试需要覆盖）
- **Estimate**: 5min

---

### A.2 改 RecommendationResult 实体（-3 +3 字段）

- **Type**: impl
- **File**: `backend/ai-food-common/src/main/java/com/ai/food/common/model/RecommendationResult.java`
- **Dependencies**: —
- **Spec 引用**: §3.2.2 + §3.2.6 + §10.5.2
- **Action**:
  - 删 `mode`/`oldFood`/`similarityScore` 字段 + 对应 `@TableField`
  - 加 `category`（String）/ `flavorTags`（String，存 JSON 数组）/ `totalTokens`（Integer）+ 3 个 `@TableField`
- **Verify**:
  - command: `mvn -pl ai-food-common compile`
  - expected: BUILD SUCCESS
- **Risk**: R3（admin-server 引用了被删字段，但 A.9 会修）
- **Estimate**: 10min

---

### A.3 写 Flyway V6（recommendation_result 字段改 + qa_record 加 user_id）

- **Type**: impl
- **File**: `backend/ai-food-app/src/main/resources/db/migration/V6__ai_module_simplify.sql`（新建）
- **Dependencies**: A.2（实体改完再写 Flyway）
- **Spec 引用**: §3.7 + §10.3（P1-7 修订：V6 只含 recommendation_result + qa_record）
- **Action**:
  ```sql
  ALTER TABLE recommendation_result
      DROP COLUMN mode,
      DROP COLUMN old_food,
      DROP COLUMN similarity_score,
      ADD COLUMN category VARCHAR(64) DEFAULT NULL,
      ADD COLUMN flavor_tags JSON DEFAULT NULL,
      ADD COLUMN total_tokens INT DEFAULT NULL;
  ALTER TABLE qa_record ADD COLUMN user_id BIGINT DEFAULT NULL;
  CREATE INDEX idx_qa_record_user_date ON qa_record(user_id, created_at);
  ```
- **Verify**:
  - command: `mvn -pl ai-food-app test`（Flyway 自动执行 + 单测跑通）
  - expected: BUILD SUCCESS（含 168 基线单测通过）
- **Risk**: R4（Flyway 命名冲突 — V6 目前不存在，confirmed）
- **Estimate**: 10min

---

### A.4 改 QaRecord 实体（加 userId 字段）

- **Type**: impl
- **File**: `backend/ai-food-common/src/main/java/com/ai/food/common/model/QaRecord.java`
- **Dependencies**: A.3（逻辑依赖：Flyway 建 user_id 列后实体映射才生效）
- **Spec 引用**: §3.2.5 + §10.3.1
- **Action**: 加 `private Long userId;` + `@TableField("user_id")`
- **Verify**:
  - command: `mvn -pl ai-food-common compile`
  - expected: BUILD SUCCESS
- **Risk**: —
- **Estimate**: 5min

---

### A.5 改 ConversationAiService（4 字段解析 + 持久化新字段）

- **Type**: impl
- **File**: `backend/ai-food-app/src/main/java/com/ai/food/service/conversation/ConversationAiService.java`
- **Dependencies**: A.2（RecommendationResult 新字段）, A.4（QaRecord.userId）
- **Spec 引用**: §3.2.3 + §3.2.7
- **Action**:
  - `parseRecommendationPayload(json)`：解析 4 字段 JSON，`flavorTags` 用 `JSON_ARRAY_AS_STRING` 处理
  - `saveRecommendationResult(…)`：写入 `category`/`flavorTags`/`totalTokens` 到 `recommendation_result`
  - 删 `state.getMode()` 调用（state.mode 字段保留）
  - 每次 AI 调用后保存 `totalTokens` 到 `recommendation_result.total_tokens`
- **Verify**:
  - command: `mvn -pl ai-food-app compile`
  - expected: BUILD SUCCESS
- **Risk**: R2（4 字段 schema 解析错误会导致 T3 测试失败）
- **Estimate**: 20min

---

### A.6 删 AiController.java

- **Type**: delete
- **File**: `backend/ai-food-app/src/main/java/com/ai/food/controller/AiController.java`
- **Dependencies**: A.5（ConversationAiService 改完才能删 controller）
- **Spec 引用**: §3.1 #1（4 个 dead endpoint）
- **Action**: 删除整个文件
- **Verify**:
  - command: `ls backend/ai-food-app/src/main/java/com/ai/food/controller/AiController.java 2>&1; mvn -pl ai-food-app compile`
  - expected: `ls: cannot access...` + BUILD SUCCESS
- **Risk**: —
- **Estimate**: 2min

---

### A.7 删 RecommendationController.java

- **Type**: delete
- **File**: `backend/ai-food-app/src/main/java/com/ai/food/controller/RecommendationController.java`
- **Dependencies**: A.5
- **Spec 引用**: §3.1 #2（4 个 dead endpoint）
- **Action**: 删除整个文件
- **Verify**:
  - command: `ls backend/ai-food-app/src/main/java/com/ai/food/controller/RecommendationController.java 2>&1; mvn -pl ai-food-app compile`
  - expected: `ls: cannot access...` + BUILD SUCCESS
- **Risk**: —
- **Estimate**: 2min

---

### A.8 删 dead methods + dead prompts + 修编译断点（5 文件）

- **Type**: impl
- **Files**:
  - `backend/ai-food-app/src/main/java/com/ai/food/service/ai/AiService.java`（删 2 方法 + 2 行 loadPrompts）
  - `backend/ai-food-app/src/main/resources/prompts/answer-validation.txt`（删）
  - `backend/ai-food-app/src/main/resources/prompts/similarity.txt`（删）
  - `backend/ai-food-app/src/main/java/com/ai/food/service/record/RecordService.java`（删 line 117 + RecordListItem.similarityScore 字段）
  - `backend/ai-food-app/src/main/java/com/ai/food/service/share/ShareService.java`（删 line 132 `result.put("mode", rec.getMode())`）
- **Dependencies**: A.6, A.7（controller 已删，确认不依赖 AiService 方法）
- **Spec 引用**: §3.1 #3-#14 + §2.3 oracle P0（#1-#2 编译断点）
- **Action**:
  - `AiService.java`：删 `calculateSimilarity()`、`validateAnswer()` 方法；删 `loadPrompts()` 中加载 `answer-validation.txt` 和 `similarity.txt` 的 2 行
  - 删 `answer-validation.txt`、`similarity.txt`
  - `RecordService.java:117`：删 `item.setSimilarityScore(result.getSimilarityScore())`
  - `RecordService.RecordListItem`：删 `similarityScore` 字段（P2-8 修订）
  - `ShareService.java:132`：删 `result.put("mode", rec.getMode())`
- **Verify**:
  - command: `mvn -pl ai-food-app compile`
  - expected: BUILD SUCCESS（oracle 3 个 P0 编译断点全部消除）
- **Risk**: R3（oracle 已识别 3 个 P0—RecordService:117、ShareService:132、RecordListItem 字段）
- **Estimate**: 15min

---

### A.9 改 admin-server RecommendationController（删 mode 参数 + mode 过滤）

- **Type**: impl
- **File**: `backend/admin-server/src/main/java/com/aifood/admin/controller/RecommendationController.java`
- **Dependencies**: A.2（RecommendationResult.getMode 已删）
- **Spec 引用**: §3.3.1
- **Action**:
  - 删 `@RequestParam String mode` 参数（原 line 54）
  - 删 `w.eq(RecommendationResult::getMode, mode)` 过滤条件（原 line 59）
  - **不动** `ConversationQueryReq.mode`（过滤的是 `conversation_session.mode`，另一张表）
- **Verify**:
  - command: `mvn -pl admin-server compile`
  - expected: BUILD SUCCESS
- **Risk**: R8（误删 `ConversationQueryReq.mode` — oracle 修订 #3 明确不动）
- **Estimate**: 5min

---

## 阶段 B: Token 统计与限额（6 tasks）

**入口**: 阶段 A 全部完成（DB schema 稳定）
**出口**: `mvn -pl ai-food-app compile && mvn -pl admin-server compile` 通过

---

### B.1 写 Flyway V7（system_config + user_token_quota + seed）

- **Type**: impl
- **File**: `backend/ai-food-app/src/main/resources/db/migration/V7__token_quota_tables.sql`（新建）
- **Dependencies**: —
- **Spec 引用**: §10.3.2 + §10.3.3
- **Action**: 建 2 张表 + 初始 seed：
  ```sql
  CREATE TABLE system_config (
      id BIGINT PRIMARY KEY AUTO_INCREMENT,
      config_key VARCHAR(64) NOT NULL UNIQUE,
      config_value VARCHAR(255) NOT NULL,
      description VARCHAR(255) DEFAULT NULL,
      updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
      version INT NOT NULL DEFAULT 0
  );

  INSERT INTO system_config (config_key, config_value, description)
  VALUES ('daily_token_limit_default', '1000000', '默认每日 token 限额（per user）');

  CREATE TABLE user_token_quota (
      id BIGINT PRIMARY KEY AUTO_INCREMENT,
      user_id BIGINT NOT NULL UNIQUE,
      daily_token_limit INT NOT NULL,
      updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
      version INT NOT NULL DEFAULT 0,
      INDEX idx_user (user_id)
  );
  ```
- **Verify**:
  - command: `mvn -pl ai-food-app test`
  - expected: BUILD SUCCESS（Flyway V6/V7 顺序执行无冲突）
- **Risk**: R16（config_key 命名规范冲突 — 用 `daily_token_limit_default` 唯一 key）
- **Estimate**: 10min

---

### B.2 新增 TokenQuotaService（限额检查 + 用量累加）

- **Type**: impl
- **File**: `backend/ai-food-app/src/main/java/com/ai/food/service/token/TokenQuotaService.java`（新建）
- **Dependencies**: B.1（DB 表就绪）
- **Spec 引用**: §3.2.8 + §10.4
- **Action**: 实现 4 个 public 方法：
  - `getEffectiveLimit(long userId) → int`：per-user > 全局配置 > 硬编码 1M
  - `getTodayUsed(long userId) → long`：`SELECT SUM(total_tokens) FROM qa_record WHERE user_id=? AND created_at>=今日 00:00`
  - `checkAndReject(long userId, int estimatedTokens) → String`：超限返回"今日 token 额度已用完"，null=放行
  - `recordUsage(long userId, int totalTokens, String model, String sessionId, String paramName)`：写入 QaRecord
  - 时区用 `LocalDate.now(ZoneId.of("Asia/Shanghai"))`（P2-10 修订）
  - `TokenQuotaService` 构造函数接收 `Clock`（默认 `Clock.systemDefaultZone()`），内部用 `LocalDate.now(clock)` 而非 `LocalDate.now()`，支持时间注入
- **Verify**:
  - command: `mvn -pl ai-food-app compile`
  - expected: BUILD SUCCESS
- **Risk**: R13（时区错乱 — JVM 和 DB 都 `Asia/Shanghai` 已确认），R12（性能 — 单测 168/168 基线）
- **Estimate**: 25min

---

### B.3 改 ConversationState（加 userId + gracefulExit + pendingMessages cap）

- **Type**: impl
- **File**: `backend/ai-food-app/src/main/java/com/ai/food/dto/ConversationState.java`
- **Dependencies**: —
- **Spec 引用**: §3.2.10 + §11.3.4 + §11.4.1
- **Action**:
  - 加 `private Long userId;` + getter/setter
  - 加 `private boolean gracefulExit;`（Lombok 生成 `isGracefulExit()`，**不命名 `completed`** 避免覆盖现有 `isCompleted()`)
  - **P0-1 警告**: 不要命名 `completed`！现有 `isCompleted()` 语义是 `currentQuestionCount >= totalQuestions`（7 问答完），被覆盖会导致 `MessageTagParser:54/57`、`ConversationService:168` 等 10+ 处判断错误
  - 加 `addPendingMessage(msg)` cap：`MAX_PENDING_MESSAGES = 10`，超限 log warn + return
- **Verify**:
  - command: `mvn -pl ai-food-app compile`
  - expected: BUILD SUCCESS
- **Risk**: **R23 (P0-1)**: gracefulExit 命名必须避开 completed
- **Estimate**: 10min

---

### B.4 改 ConversationService.processAnswer 入口（限额检查 + recordUsage）

- **Type**: impl
- **File**: `backend/ai-food-app/src/main/java/com/ai/food/service/conversation/ConversationService.java`
- **Dependencies**: **B.3**（state.userId 就绪, P2-12）, B.2（TokenQuotaService 就绪）
- **Spec 引用**: §3.2.9 + §10.4 + §10.7
- **Action**:
  - 入口：`Long userId = state.getUserId(); String reject = tokenQuotaService.checkAndReject(userId, 500);`
  - 超限：`return List.of(systemError("今日 token 额度已用完"))` — AI 调用数为 0
  - 正常路径中（AI 返回后）：调 `tokenQuotaService.recordUsage(…)`
- **Verify**:
  - command: `mvn -pl ai-food-app compile`
  - expected: BUILD SUCCESS
- **Risk**: R19（race condition — SUM 聚合无原子性问题，短暂超限 < 1 次调用可接受）
- **Estimate**: 15min

---

### B.5 改 MessageTagParser（token 累加 + 限额检查）

- **Type**: impl
- **File**: `backend/ai-food-app/src/main/java/com/ai/food/service/conversation/MessageTagParser.java`
- **Dependencies**: B.2, B.4
- **Spec 引用**: §10.7
- **Action**:
  - `getQuestionContent()` 入口：调 `checkAndReject(userId, 500)`，超限抛异常/返回兜底问题
  - AI 调用 `aiService.generateQuestion()` 返回后：调 `recordUsage(…)` 累加 token
- **Verify**:
  - command: `mvn -pl ai-food-app compile`
  - expected: BUILD SUCCESS
- **Risk**: R12（每答一题一次 QaRecord 写入）
- **Estimate**: 15min

---

### B.6 新增 admin-server SystemConfigController + TokenQuotaController + 2 service

- **Type**: impl
- **Files**（4 新建）:
  - `backend/admin-server/src/main/java/com/aifood/admin/controller/SystemConfigController.java`
  - `backend/admin-server/src/main/java/com/aifood/admin/controller/TokenQuotaController.java`
  - `backend/admin-server/src/main/java/com/aifood/admin/service/SystemConfigService.java`
  - `backend/admin-server/src/main/java/com/aifood/admin/service/UserTokenQuotaService.java`
- **Dependencies**: B.1（DB 表就绪）
- **Spec 引用**: §3.3.3 + §3.3.4 + §3.3.5 + §10.6
- **Action**:
  - `SystemConfigController`: `GET/PUT /admin/api/config/token-limit`
  - `TokenQuotaController`: `GET/PUT/DELETE /admin/api/token-quota/{userId}`
  - `SystemConfigService`: key-value CRUD（通用扩展）
  - `UserTokenQuotaService`: 单用户覆盖 CRUD
  - **P1-6**: 两个 controller 都加 `@PreAuthorize("hasRole('ADMIN')")` 或在 WebSecurityConfig 加 URL guard
- **Verify**:
  - command: `mvn -pl admin-server compile`
  - expected: BUILD SUCCESS
- **Risk**: R17（鉴权遗漏 — 非 ADMIN 能改限额是安全事故）
- **Estimate**: 25min

---

## 阶段 C: 异常退出 + OOM 防御（2 tasks）

**入口**: 阶段 B 全部完成（state.gracefulExit + userId 就绪）
**出口**: `mvn -pl ai-food-app compile` 通过

> C.1 和 C.2 修改同一文件 `ConversationWebSocketHandler.java`，建议顺序执行（先 C.1 后 C.2）避免冲突

---

### C.1 改 ConversationWebSocketHandler（异常退出清理 + userId 注入）

- **Type**: impl
- **File**: `backend/ai-food-app/src/main/java/com/ai/food/websocket/ConversationWebSocketHandler.java`
- **Dependencies**: B.3（state.gracefulExit + userId 就绪）
- **Spec 引用**: §11.3.2 + §11.3.3 + §11.3.5 + §11.3.6
- **Action**:
  - `afterConnectionClosed`：
    - if `state != null && !state.isGracefulExit()`：
      - **P0-2**: 先 `state.setCancelled(true)` 阻断 aiExecutor 异步任务 race
      - `state.setAiProcessing(false); state.clearPendingMessages();`
      - `conversationService.cancelSession(sessionId)` 软删 DB
  - `handleTransportError`：同 `afterConnectionClosed` 逻辑（P0-2 修复）
  - `handleComplete`：`state.setGracefulExit(true)`（P0-1 命名）
  - `handleStart`：`state.setUserId((Long) session.getAttributes().get("userId"))`（P0-3 注入）
- **Verify**:
  - command: `mvn -pl ai-food-app compile`
  - expected: BUILD SUCCESS
- **Risk**: **R14 (P0-2)**: 异常路径必须先 `setCancelled(true)` 再 `cancelSession`，阻止 async race
- **Estimate**: 20min

---

### C.2 改 conversationStates OOM 防御（MAX=3,000）

- **Type**: impl
- **File**: `backend/ai-food-app/src/main/java/com/ai/food/websocket/ConversationWebSocketHandler.java`
- **Dependencies**: —
- **Spec 引用**: §11.4.2
- **Action**:
  - 加 `private static final int MAX_CONCURRENT_STATES = 3_000;`
  - 提取 `registerState(sessionId, state)` 方法：
    - `if (conversationStates.size() >= MAX_CONCURRENT_STATES)` → log error + `throw new IllegalStateException("服务繁忙，请稍后再试")`
    - `conversationStates.put(sessionId, state)`
  - 在 `handleStart` 中调 `registerState` 替代直接 put
- **Verify**:
  - command: `mvn -pl ai-food-app compile`
  - expected: BUILD SUCCESS
- **Risk**: R22（MAX=3,000 保守值，4GB/swap 1.3GB 实测）
- **Estimate**: 10min

---

## 阶段 D: 前端（5 tasks）

**入口**: 阶段 C 全部完成
**出口**: `cd frontend && npm run build && cd frontend/admin-web && npx vite build` 通过

> D.1（6 文件）按规则拆为 D.1（3 文件）+ D.2（3 文件）

---

### D.1 改前端 Result.vue + FeedDetail.vue + ResultPublishDialog.vue（新字段展示）

- **Type**: impl
- **Files**:
  - `frontend/src/views/Result.vue`
  - `frontend/src/views/FeedDetail.vue`
  - `frontend/src/views/components/Result/ResultPublishDialog.vue`
- **Dependencies**: —
- **Spec 引用**: §3.4
- **Action**:
  - `Result.vue`：加 `category` chip + `flavorTags[]` chips 展示；`foodName` / `reason` 保留
  - `FeedDetail.vue`：加 `category` + `flavorTags` chips 展示
  - `ResultPublishDialog.vue`：发布预览改用 `category + reason`
- **Verify**:
  - command: `cd frontend && npm run build`
  - expected: 0 error
- **Risk**: R6（Vant 组件兼容性）
- **Estimate**: 20min

---

### D.2 改前端 RecordDetail.vue + Records.vue + chatStore（旧字段清理 + 类型强化）

- **Type**: impl
- **Files**:
  - `frontend/src/views/RecordDetail.vue`
  - `frontend/src/views/Records.vue`
  - `frontend/src/stores/chat.ts`
- **Dependencies**: A.8（RecordListItem 已无 similarityScore）
- **Spec 引用**: §3.4
- **Action**:
  - `RecordDetail.vue:17`：删 mode 标签（inertia/随机模式），老数据兜底显示"AI 推荐"
  - `Records.vue:136`：interface `RecordListItem` 移除 `mode?: string`
  - `Records.vue:293`：`displayReason` fallback 改静态字符串，不用 `record.mode`
  - `chatStore`：`recommendationResult` 从 `any` 改为 typed interface（4 字段：foodName, reason, category, flavorTags）
- **Verify**:
  - command: `cd frontend && npm run build`（npm run build 内已含 vue-tsc）
  - expected: 0 error
- **Risk**: R6（类型改动传播到其他 view）
- **Estimate**: 20min

---

### D.3 改 admin-web recommendation/index.vue + api/recommendation.ts（删 mode 相关列）

- **Type**: impl
- **Files**:
  - `frontend/admin-web/src/views/recommendation/index.vue`
  - `frontend/admin-web/src/api/recommendation.ts`
- **Dependencies**: A.9（后端不再接受 mode 参数）
- **Spec 引用**: §3.5
- **Action**:
  - `index.vue`：删 `mode` 列（L70）、`similarityScore` 列（L72-76）、`mode` 下拉筛选器（L52-56）；删 L14 `query.mode` 声明、L23 `params.mode` 传递、L39 query 重置中的 `mode`
- **Verify**:
  - command: `cd frontend/admin-web && npx vite build`
  - expected: 0 error
- **Risk**: R11（视觉验证 — T9 明确要求"模式列消失、相似度列消失、模式下拉消失"）
- **Estimate**: 10min

---

### D.4 新增 admin-web token-quota 视图（api + 视图 + 路由 + 菜单）

- **Type**: impl
- **Files**（2 新建 + 2 修改）:
  - `frontend/admin-web/src/api/tokenQuota.ts`（新建）
  - `frontend/admin-web/src/views/token-quota/index.vue`（新建）
  - `frontend/admin-web/src/router/index.ts`（修改：加 `/token-quota` 路由）
  - `frontend/admin-web/src/layouts/DefaultLayout.vue`（修改：加菜单项"Token 限额"）
- **Dependencies**: B.6（后端 API 就绪）
- **Spec 引用**: §3.5 + §10.6 + §P1-6 鉴权
- **Action**:
  - `tokenQuota.ts`：5 个接口 client（GET/PUT 全局限额 + GET/PUT/DELETE 单用户覆盖）
  - `index.vue`：全局限额表单 + 单用户列表 + CRUD
  - `router/index.ts`：加路由
  - `DefaultLayout.vue`：菜单项加 `v-if="$store.state.user.role === 'ADMIN'"`
- **Verify**:
  - command: `cd frontend/admin-web && npx vite build`
  - expected: 0 error
- **Risk**: R6（鉴权遗漏 — 非 ADMIN 能看到菜单但不影响后端 guard）
- **Estimate**: 25min

---

### D.5 改 bench 脚本注释 /api/ai/chat case

- **Type**: impl
- **File**: `scripts/bench/run-all-benchmarks.sh`
- **Dependencies**: —
- **Spec 引用**: §3.6
- **Action**: 注释或删除 line 119-127 的 `/api/ai/chat` 限流测试 case
- **Verify**:
  - command: `grep -c "/api/ai/chat" scripts/bench/run-all-benchmarks.sh`
  - expected: 输出 `0`（或注释符号开头）
- **Risk**: R7（注释而非删除，保留脚本可读性）
- **Estimate**: 2min

---

## 阶段 E: 测试与提交（1 task）

**入口**: 所有阶段 A-D 完成
**出口**: tasks.md 标记完成

---

### E.1 全量测试（T1-T18）→ commit

- **Type**: verify
- **Dependencies**: A.1–D.5 全部完成
- **Spec 引用**: §5（测试矩阵 T1-T18）+ §8（验收标准）
- **Action**: 逐项执行测试矩阵，修复失败项，提交 commit
- **测试矩阵**:

  | # | 范围 | 命令 | 基线 |
  |---|---|---|---|
  | T1 | ai-food-app 单测 | `mvn -pl ai-food-app test` | 168/168 通过 |
  | T2 | admin-server 单测 | `mvn -pl admin-server test` | 维持基线 |
  | T3 | 新增 ConversationAiServiceTest | `mvn -pl ai-food-app test -Dtest=ConversationAiServiceTest` | 4 用例（完整JSON/缺字段/非法JSON/数组降级） |
  | T4 | 前端编译 | `cd frontend && npm run build` | 0 error |
  | T5 | 前端类型检查 | `cd frontend && npx vue-tsc --noEmit` | 0 error |
  | T6 | admin-web 编译 | `cd frontend/admin-web && npx vite build` | 0 error |
  | T7 | 集成手测 Chat→7问→推荐→Result | 浏览器 | 4 字段 foodName/reason/category/flavorTags 全部展示 |
  | T8 | 回归 Match.vue 找人匹配 | 浏览器 | 创建匹配 → 查看结果不受影响 |
  | T9 | 回归 admin-web 推荐记录页 | 浏览器 | 模式/相似度列消失、模式下拉消失 |
  | T10 | 回归 admin-web 会话管理页 | 浏览器 | session 模式下拉仍能过滤 |
  | T11 | 回归 bench 脚本 | `bash scripts/bench/run-all-benchmarks.sh` | 注释 /api/ai/chat 后剩余通过 |
  | T12 | 新增 TokenQuotaServiceTest | `mvn -pl ai-food-app test -Dtest=TokenQuotaServiceTest` | 4 场景：per-user/全局/默认fallback/超限拒绝 |
  | T13 | 新增 ConversationWebSocketHandlerTest | `mvn -pl ai-food-app test -Dtest=ConversationWebSocketHandlerTest` | 10+ 用例（5路径×2子场景） |
  | T14 | 新增 OomDefenseTest | `mvn -pl ai-food-app test -Dtest=OomDefenseTest` | 2 用例（pendingMessages cap + conversationStates cap） |
  | T15 | 集成手测 token 限额 | SQL预插 + WS 验证 | 达限额后 WS 发 system 错误，AI 不调用 |
  | T16 | 集成手测 admin-web 限额管理 | 浏览器 | 改全局/单用户 → 立即生效 |
  | T17 | 回归异常退出 | 浏览器 + SQL | 关浏览器 → qa_record 无该 session 记录 |
  | T18 | 新增跨日限额测试 | `mvn -pl ai-food-app test -Dtest=TokenQuotaServiceTest#testCrossDay` | 用 `@MockBean Clock` 模拟 23:59→00:01，验证限额跨日归零 |
- **Verify**:
  - command: `mvn -pl ai-food-app test && mvn -pl admin-server test && cd frontend && npm run build && npx vue-tsc --noEmit && cd ../admin-web && npx vite build`
  - expected: 全部通过
- **Estimate**: 30min

---

## 依赖关系总图

```
阶段 A:
  A.1 ─┐
  A.2 ─┼→ A.3 → A.4 → A.5 → A.6 → A.7 → A.8
  A.2 ─┘                        └→ A.8 ─┘
  A.2 ─→ A.9（并行于 A.6-A.8）

阶段 B:
  B.1 → B.2 ─┐
  B.3 ───────┼→ B.4 → B.5
  B.1 → B.6（并行于 B.2-B.5）

阶段 C:
  B.3 → C.1
  C.2（可并行于 C.1，但同一文件建议顺序）

阶段 D:
  A.8 → D.2
  A.9 → D.3
  B.6 → D.4
  D.1 / D.5（无依赖，可并行）

阶段 E:
  A.1-D.5 全部 → E.1
```

**关键串行门**：
- **A.2 → A.3 → A.4 → A.5**（实体 → Flyway → 实体 → 解析）
- **B.3 → B.4**（P2-12：state.userId 必须先于 processAnswer 入口）
- 全阶段串行：A → B → C → D → E

---

## Spec Drift 风险点

| # | 风险 | 说明 | 缓解 |
|---|---|---|---|
| SD1 | **A.5 与 B.2 token 边界模糊** | A.5 写 totalTokens 到 recommendation_result，B.2 写 QaRecord。实施时可能混淆"谁负责什么" | A.5 只写 recommend 级 token；B.2 的 recordUsage 管所有 AI 调用的 QaRecord 写入 |
| SD2 | **D.1 文件超限** | D.1 按规则拆为 D.1(3文件)+D.2(3文件)，但实施时可能仍想一次改 6 个 | 分两次 commit，先 D.1 后 D.2，各自 ≤5 文件 |
| SD3 | **C.1 + C.2 同文件冲突** | 两个 task 改同一个 ConversastionWebSocketHandler.java | 顺序执行 C.1 → C.2，避免同时改同一区域的 git 冲突 |
| SD4 | **T18 跨日测试需要 Mock 时间** | SQL user-defined variable 或 Clock 注入 | 用 `@Autowired Clock` + `LocalDate.now(clock)` 可测试；或 `TimeTestUtil` 辅助 |
| SD5 | **Flyway V6 与现网版本顺序** | 现网已有 V5，V6 必须 > V5 且不冲突 | 确认 V5 文件名 `V5__backfill_qa_record_tokens.sql`，V6 按 `V6__` 命名 |
| SD6 | **admin-server 引了被删 RecommendationResult 方法** | 除 mode 外可能还有其他引用 | A.9 后 `mvn -pl admin-server compile` 全量验证 |

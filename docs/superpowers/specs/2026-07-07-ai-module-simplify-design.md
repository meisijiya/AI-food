# AI-food · AI 模块简化重构 · Design Spec

> **Date**: 2026-07-07 (v4)
> **Status**: Draft · 待用户审阅
> **Author**: Orchestrator + @explorer (代码现状审计) + @oracle (独立审查) + 用户增量需求（token 限额 + 异常退出）
> **Brainstorming sessions**:
> - `ses_0b5ed3c8effeLyp5R5MmHuwV7s` (@explorer · AI 模块代码现状审计)
> - `ses_0b5ed1306ffeh4oRYWtewo9w3w` (@explorer · admin-server 字段依赖审计)
> - `ses_0b5626731ffeBxYwoNbm94rVY5` (@oracle · 独立审查 APPROVE-WITH-CHANGES)

---

## 0. 决策摘要 (Decision Log)

通过 brainstorming + 3 份独立审计锁定的核心决策：

| 决策 | 选择 | 理由 |
|---|---|---|
| **业务方向** | 合并 2 个 mode → 1 个；删除 pre-imported 食物池 + Jaccard topk 维护逻辑 | 2 个 mode 维护成本高；Jaccard topk 需要充足数据，与"AI 生成描述 → 用户去现实找"业务冲突 |
| **AI 定位** | "描述生成器"而非"店铺推荐器" | 用户原话："用户拿着 AI 描述去现实寻找类似或相同的食物，否则失去灵活性" |
| **核心 prompt 投入** | 强化 `recommendation.txt` 4 字段 JSON schema | prompt engineering 是唯一可维护的杠杆 |
| **保留 7 问 Q&A** | WebSocket 状态机 7 必选 + 3 可选 + 自由对话阶段 | 这是设计理念核心，必须保留 |
| **匹配功能不受影响** | `Match.vue` 找人匹配走 BloomFilterServiceImpl，跟被删字段无关 | oracle 验证 |
| **8 个 dead HTTP endpoint** | 全删 | 前端、admin、生产均无调用（bench 脚本调 `/api/ai/chat` 单独处理） |
| **3 个死字段** | 全删（mode / old_food / similarity_score）| production 从不写入（oracle 验证）；admin 端同步改 |
| **新增 2 字段** | `category` (VARCHAR(64)) + `flavor_tags` (JSON) | 给用户更丰富的描述；JSON 类型防格式错误、可建索引（oracle 修订 #6） |
| **不动 `ConversationQueryReq.mode`** | 保留 | 该字段过滤的是 `conversation_session.mode`（另一张表），不能误删（oracle 修订 #3） |
| **Token 存储** | `QaRecord` 加 `user_id` 字段 + 复合索引 | userId 入库 + 每日限额查询都靠这一列 |
| **限额默认值** | 1,000,000 tokens/天（可调整） | 用户确认；可在 admin 后台全局改 / 单用户覆盖 |
| **超限行为** | 硬拒绝 | WS 发 `system` 错误消息"今日 token 额度已用完"，AI 调用数 0 |
| **限额配置层级** | 默认 → 全局配置 → 单用户覆盖（per-user 优先） | 2 层覆盖，admin 后台管理 |
| **异常退出清理** | `sweepIdle` / `afterConnectionClosed` / `handleTransportError` 全部调 `cancelSession` 软删 DB | 用户选 A：异常退出记录不保留 |
| **OOM 防御** | `state.pendingMessages` List 大小上限 + `conversationStates` Map 大小上限 | 防御恶意刷连接 / 抢话堆积 |

---

## 1. 背景与目标

### 1.1 业务背景

AI 模块当前有两个推荐模式（inertia / random）：

- **inertia 模式** (`/api/recommendation/inertia`)：参数 → AI → 返回推荐。简单但与 `/api/ai/recommend` 重复。
- **random 模式** (`/api/recommendation/random`)：参数 → AI → 查 `recommendation_result` 表取最近推荐当"oldFood" → 调 AI 算相似度 → 相似度 ≥ 0.7 用新食物、否则退回旧食物。

random 模式的设计存在根本问题：
1. **需要充足历史数据**才能稳定工作（pre-imported 食物池），跟"用户带着状态描述去找食物"的业务冲突。
2. **代码逻辑混乱**：相似度阈值 0.7 + LLM 算相似度（不稳定）+ 查全表最近 5 条（无 userId 过滤）。
3. **维护成本高**：两个 mode 两套逻辑、两个 endpoint、两份 prompt 工程。

### 1.2 目标

- **业务目标**：简化用户决策路径，强化 AI prompt 让推荐描述更具体可执行。
- **工程目标**：删除 dead code，保留核心 7 问 Q&A 流程，让代码可被一个工程师独立维护。
- **架构目标**：AI 模块变成"纯生成层"——输入用户状态参数，输出结构化描述，无历史数据依赖。

### 1.3 非目标

- 不重写 WebSocket 状态机
- 不动 Match.vue 找人匹配功能
- 不重构 ai-food-app / admin-server 整体架构
- 不优化 prompt 调优流程（工具链）

---

## 2. 当前状态审计结果

### 2.1 @explorer 代码现状审计（`ses_0b5ed3c8effeLyp5R5MmHuwV7s`）

| 制品 | 状态 |
|---|---|
| **8 个 HTTP endpoint** | 全部 dead code（生产无调用） |
| → `/api/ai/chat` 例外 | `scripts/bench/run-all-benchmarks.sh:121-127` 性能压测 |
| **`AiService.calculateSimilarity`** | dead |
| **`AiService.validateAnswer`** | dead |
| **`AiService.chat`** | **active**（3 处生产调用） |
| **`AiService.generateQuestion`** | **active**（`MessageTagParser:104`） |
| **`AiService.generateRecommendation`** | **active**（`ConversationService:235`） |
| **`prompts/answer-validation.txt`** | dead |
| **`prompts/similarity.txt`** | dead |
| **`prompts/question-generation.txt`** | **active** |
| **`prompts/recommendation.txt`** | **active** |
| **`recommendation_result.old_food`** | production **从不写入**（永远 NULL） |
| **`recommendation_result.similarity_score`** | production **从不写入**（永远 NULL） |
| **`recommendation_result.mode`** | production **只写 "inertia"**（WebSocket 主流程不切换 mode） |
| **前端 mode 选择 UI** | **不存在** |
| **Match.vue 找人匹配** | 用 `BloomFilterServiceImpl:127/188`，独立功能 |

### 2.2 @explorer admin-server 审计（`ses_0b5ed1306ffeh4oRYWtewo9w3w`）

| 待删项 | admin 端影响 |
|---|---|
| 8 个 HTTP endpoint | ✅ admin 无依赖 |
| `AiService` 方法 | ✅ admin 无依赖 |
| `recommendation_result.old_food` | ✅ admin 零引用 |
| `recommendation_result.mode` | ⚠️ admin-server `RecommendationController.java:54,59` 编译会爆 |
| `recommendation_result.similarity_score` | ⚠️ admin-web `recommendation/index.vue:72-76` 展示该列 |
| **`ConversationQueryReq.mode`** | ⚠️ **不能删**！该字段过滤的是 `conversation_session.mode`（另一张表） |

### 2.3 @oracle 独立审查（`ses_0b5626731ffeBxYwoNbm94rVY5`）— APPROVE-WITH-CHANGES

发现 3 个 P0 修订点 + 5 个 P1/P2 建议，全部吸收到本 spec：

| # | 优先级 | 修订 |
|---|---|---|
| 1 | P0 | `RecordService.java:117` `item.setSimilarityScore(...)` 删后编译爆 |
| 2 | P0 | `ShareService.java:132` `result.put("mode", rec.getMode())` 删后编译爆 |
| 3 | P0 | **不要删** `ConversationQueryReq.mode`（过滤 `conversation_session.mode`，跟 `recommendation_result.mode` 是不同表） |
| 4 | P1 | `Records.vue:136` interface 移除 `mode?: string` |
| 5 | P1 | `Records.vue:293` fallback 改静态字符串 |
| 6 | P1 | `flavor_tags` 用 MySQL JSON 类型，不用 VARCHAR(512) |
| 7 | P1 | 实施步骤：Flyway 紧跟实体修改后（避免 mvn test schema 不匹配） |
| 8 | P2 | 检查 `Share.vue` 是否引用 mode 字段 |
| 9 | P2 | admin-web 视觉验证"模式列消失" |

---

## 3. 方案详述

### 3.1 后端 ai-food-app — 删除清单

| # | 路径 | 类型 | 理由 |
|---|---|---|---|
| 1 | `backend/ai-food-app/src/main/java/com/ai/food/controller/AiController.java` | 整个文件 | 4 个 endpoint 全 dead（/chat / validate-answer / recommend / similarity） |
| 2 | `backend/ai-food-app/src/main/java/com/ai/food/controller/RecommendationController.java` | 整个文件 | 4 个 endpoint 全 dead（/inertia / random / similarity / history） |
| 3 | `AiService.calculateSimilarity(food1, food2)` | 方法 | 仅被 #2 调用 |
| 4 | `AiService.validateAnswer(param, question, answer)` | 方法 | 仅被 #1 调用 |
| 5 | `backend/ai-food-app/src/main/resources/prompts/answer-validation.txt` | 文件 | 仅被 #4 使用 |
| 6 | `backend/ai-food-app/src/main/resources/prompts/similarity.txt` | 文件 | 仅被 #3 使用 |
| 7 | `AiService.loadPrompts()` 加载 #5 #6 的两行 | 代码 | 跟随 #5 #6 删除 |
| 8 | `recommendation_result.mode` 字段 | DB 列 | production 只写 "inertia"，前端无选择 UI |
| 9 | `recommendation_result.old_food` 字段 | DB 列 | production 从不写入 |
| 10 | `recommendation_result.similarity_score` 字段 | DB 列 | production 从不写入 |
| 11 | `RecordService.java:117` `item.setSimilarityScore(result.getSimilarityScore())` | 代码行 | 跟随 #10 删除，否则编译爆 |
| 12 | `RecordService.RecordItem` 类对应 `similarityScore` 字段 | 代码 | 跟随 #10 移除 |
| 13 | `ShareService.java:132` `result.put("mode", rec.getMode())` | 代码行 | 跟随 #8 删除，否则编译爆 |
| 14 | `frontend/src/views/Share.vue` 中 mode 字段引用 | 代码（如有） | 跟随 #8 删除（先 grep 验证） |

### 3.2 后端 ai-food-app — 强化清单

#### 3.2.1 改 `prompts/recommendation.txt`

**新内容**（4 字段 JSON schema）：

```
你是一个专业的美食推荐助手。根据用户当前状态（时间/地点/天气/心情/同行者/预算/口味），
推荐一道适合的美食。用户拿着推荐去现实寻找类似或相同的食物，所以描述要够具体。

请以JSON格式返回（不要markdown代码块）：
{
    "foodName": "推荐的具体食物名称",
    "reason": "50字以内的推荐理由，说明为什么适合当前场景",
    "category": "菜系/品类（如：粤菜/日料/川菜/快餐/甜品/饮品）",
    "flavorTags": ["口味标签1", "口味标签2", "口味标签3"]
}

只返回JSON，不要任何解释或markdown代码块。

用户当前状态：
{params}

请根据以上信息推荐一道美食，描述要具体真实，便于用户去现实寻找。
```

#### 3.2.2 改 `RecommendationResult` 实体

**删除字段**：`mode`、`oldFood`、`similarityScore`

**新增字段**：
```java
@TableField("category")
private String category;

@TableField("flavor_tags")
private String flavorTags;  // 存 JSON 数组字符串
```

#### 3.2.3 改 `ConversationAiService`

- `parseRecommendationPayload(json)`：解析新 schema，`flavorTags` 用 `JSON_ARRAY_AS_STRING` 处理
- `saveRecommendationResult(sessionId, state, recommendationJson)`：写入新字段
- `state.getMode()` 调用删除（state.mode 仍保留，session 还在写）

#### 3.2.4 改 `AiService.loadPrompts`

删除加载 #5 #6 的两行：
```java
// 删
answerValidationPrompt = loadPrompt("prompts/answer-validation.txt");
similarityPrompt = loadPrompt("prompts/similarity.txt");
```

#### 3.2.5 改 `QaRecord` 实体（Token 限额需要）

加 `userId` 字段（详见 Section 10.3.1）：

```java
@TableField("user_id")
private Long userId;
```

#### 3.2.6 改 `RecommendationResult` 实体（加 `total_tokens` 字段）

为支持推荐级 token 统计，加 `totalTokens` 字段（详见 Section 10.5.1）：

```java
@TableField("total_tokens")
private Integer totalTokens;
```

#### 3.2.7 改 `ConversationAiService`（token 累加）

每次 `aiService.chat/generateQuestion/generateRecommendation` 返回 `ChatResult` 后，把 `userId + promptTokens + completionTokens + totalTokens + model` 写入 `QaRecord`。**注意 4 字段 schema 升级后**，`saveRecommendationResult` 也要把 `totalTokens` 写入 `recommendation_result.total_tokens`。

#### 3.2.8 新增 `TokenQuotaService`（限额检查 + 配置查询）

```java
@Service
public class TokenQuotaService {
    // 查有效限额（per-user 覆盖 > 全局 > 默认 1M）
    public int getEffectiveLimit(long userId);
    // 查今日已用 token
    public long getTodayUsed(long userId);
    // 检查并拒绝（返回 null=放行 / 错误消息=拒绝）
    public String checkAndReject(long userId, int estimatedTokens);
    // 累加今日用量（每次 AI 调用后调）
    public void recordUsage(long userId, int totalTokens, String model, String sessionId, String paramName);
}
```

#### 3.2.9 改 `ConversationService.processAnswer`（入口限频）

```java
public List<WebSocketMessage> processAnswer(String sessionId, String content, ConversationState state) {
    Long userId = state.getUserId();  // state 加 userId 字段
    String reject = tokenQuotaService.checkAndReject(userId, 500);
    if (reject != null) {
        return List.of(systemError(reject));  // WS 发 system 错误
    }
    // ... 现有逻辑
}
```

#### 3.2.10 改 `ConversationState`（加 userId + completed 字段）

```java
private Long userId;          // 新增：构造时由 ConversationService 注入
private boolean completed;    // 新增：handleComplete 时设 true
```

#### 3.2.11 改 `ConversationWebSocketHandler`（异常退出清理 + OOM 防御）

详见 Section 11.3：

- `afterConnectionClosed` 调 `cancelSession` 软删 DB（if state != null && !state.isCompleted()）
- `handleTransportError` 调 `cancelSession` 软删 DB
- `handleComplete` 设 `state.setCompleted(true)`
- `state.addPendingMessage` 加 size 上限（MAX=10）
- `conversationStates` Map size 上限（MAX=10,000）

### 3.3 后端 admin-server — 同步改

#### 3.3.1 `RecommendationController.java` (admin-server)

**删除**：
- `@RequestParam String mode` 参数（line 54）
- `w.eq(RecommendationResult::getMode, mode)` 过滤条件（line 59）
- 引用 `RecommendationResult::getMode` 的方法引用

**保留**：admin-server `RecommendationController` 整体（推荐记录查询功能还在）

#### 3.3.2 `ConversationQueryReq.java` — **不动**

该 DTO 的 `mode` 字段用于过滤 `conversation_session.mode`（另一张表的 `mode` 列），跟被删的 `recommendation_result.mode` 不是同一个。

#### 3.3.3 新增 `TokenQuotaController`（admin-server）

详见 Section 10.6。新增 5 个接口：

```java
@RestController
@RequestMapping("/admin/api/token-quota")
public class TokenQuotaController {
    GET    /admin/api/config/token-limit
    PUT    /admin/api/config/token-limit
    GET    /admin/api/token-quota/{userId}
    PUT    /admin/api/token-quota/{userId}
    DELETE /admin/api/token-quota/{userId}
}
```

#### 3.3.4 新增 `SystemConfigController`（admin-server）

封装 `system_config` 表的 key-value 访问（不只是 token 限额，未来可扩展）：

```java
@RestController
@RequestMapping("/admin/api/config")
public class SystemConfigController {
    // 通用 GET/PUT by key（不在本 spec 内展开）
}
```

#### 3.3.5 新增 `UserTokenQuotaService` + `SystemConfigService`（admin-server）

- `UserTokenQuotaService`：CRUD `user_token_quota` 表
- `SystemConfigService`：CRUD `system_config` 表（通用 key-value）

### 3.4 前端 ai-food-app — 最小改动

| 文件 | 改动 |
|---|---|
| `frontend/src/views/Result.vue` | 加 `category` chip + `flavorTags[]` chips 展示；`foodName` / `reason` 保留 |
| `frontend/src/views/RecordDetail.vue:17` | 删 mode 标签（inertia/随机模式），老数据兜底显示"AI 推荐" |
| `frontend/src/views/Records.vue:136` | `RecordItem` interface 移除 `mode?: string` |
| `frontend/src/views/Records.vue:293` | `displayReason` fallback 改静态字符串，不用 `record.mode` |
| `frontend/src/views/FeedDetail.vue` | 加 `category` + `flavorTags` chips 展示 |
| `frontend/src/views/components/Result/ResultPublishDialog.vue` | 发布预览改用 `category + reason` |
| `frontend/src/stores/chat.ts` | `recommendationResult` 从 `any` 改 typed interface（4 字段） |

### 3.5 前端 admin-web — 同步改

| 文件 | 改动 |
|---|---|
| `frontend/admin-web/src/views/recommendation/index.vue` | 删 `mode` 列（L70）、`similarityScore` 列（L72-76）、`mode` 筛选器（L52-56、L54） |
| `frontend/admin-web/src/api/recommendation.ts` | 不传 `mode` 参数 |
| `frontend/admin-web/src/api/tokenQuota.ts` | **新增**：5 个接口的 client（GET/PUT 全局限额 + GET/PUT/DELETE 单用户覆盖） |
| `frontend/admin-web/src/views/token-quota/index.vue` | **新增**：token 限额管理视图（全局表单 + 单用户列表 + CRUD） |
| `frontend/admin-web/src/router/index.ts` | 加 `/token-quota` 路由 |
| `frontend/admin-web/src/layouts/DefaultLayout.vue` | 加菜单项"Token 限额" |

### 3.6 脚本同步改

| 文件 | 改动 |
|---|---|
| `scripts/bench/run-all-benchmarks.sh:119-127` | 注释或删除 `/api/ai/chat` 限流测试 case |

### 3.7 数据迁移

**Flyway `V6__ai_module_simplify.sql`**：

```sql
-- 删除 production 从不写入的 3 个字段
ALTER TABLE recommendation_result
    DROP COLUMN mode,
    DROP COLUMN old_food,
    DROP COLUMN similarity_score;

-- 新增 2 个字段：category (菜系) + flavor_tags (口味标签 JSON 数组)
ALTER TABLE recommendation_result
    ADD COLUMN category VARCHAR(64) DEFAULT NULL COMMENT '菜系/品类',
    ADD COLUMN flavor_tags JSON DEFAULT NULL COMMENT '口味标签 JSON 数组';
```

**注意**：`flavor_tags` 用 MySQL JSON 类型（5.7+ 原生支持），不用 VARCHAR(512)（oracle 修订 #6：可建索引、防格式错误、无长度限制）。

---

## 4. 保留与不变清单

### 4.1 必须保留的 Java 方法

```
aiService.chat()                        → ConversationAiService + MessageTagParser + handleInterrupt + AiParamNormalizer
aiService.generateQuestion()            → MessageTagParser.getQuestionContent()
aiService.generateRecommendation()      → ConversationService.generateRecommendationMessage()
```

### 4.2 必须保留的 prompt 模板

```
prompts/question-generation.txt         → aiService.generateQuestion
prompts/recommendation.txt              → aiService.generateRecommendation（schema 升级见 3.2.1）
```

### 4.3 必须保留的 DB 字段

```
recommendation_result.id
recommendation_result.session_id
recommendation_result.food_name
recommendation_result.reason
recommendation_result.photo_url
recommendation_result.comment
recommendation_result.created_at
recommendation_result.is_deleted
recommendation_result.version

-- 新增
recommendation_result.category
recommendation_result.flavor_tags
```

### 4.4 必须保留的 DB 字段（其他表）

```
conversation_session.mode                → ConversationQueryReq.mode 过滤用，本 spec 不动
```

### 4.5 必须保留的功能

- WebSocket 7 问 Q&A 状态机（`ConversationService.processAnswer`）
- AI 确认/自由对话阶段（`ConversationAiService.generateAiResponse`）
- Match.vue 找人匹配（基于 BloomFilterServiceImpl）
- 用户上传照片 + 评论（`recommendation_result.photo_url` / `comment`）
- Feed 发布 + 分享

---

## 5. 测试矩阵

| # | 范围 | 命令 | 基线 / 期望 |
|---|---|---|---|
| T1 | ai-food-app 单测 | `mvn -pl ai-food-app test` | 168/168 通过 |
| T2 | admin-server 单测 | `mvn -pl admin-server test` | 维持基线 |
| T3 | **新增** `ConversationAiServiceTest` | 同 T1 | 测试 4 字段 JSON 解析：完整 / 缺字段 / 非法 JSON / 数组降级 |
| T4 | ai-food-app 前端编译 | `cd frontend && npm run build` | 0 error |
| T5 | ai-food-app 前端类型检查 | `cd frontend && vue-tsc --noEmit` | 0 error |
| T6 | admin-web 前端编译 | `cd frontend/admin-web && npx vite build` | 0 error |
| T7 | 集成手测 Chat → 7 问 → AI 推荐 → Result 4 字段 | 浏览器 | foodName/reason/category/flavorTags 全部展示 |
| T8 | **回归** Match.vue 找人匹配 | 浏览器 | 创建匹配 → 查看匹配结果不受影响 |
| T9 | **回归** admin-web 推荐记录页 | 浏览器 | 模式列消失、相似度列消失、模式下拉消失、表格不报错 |
| T10 | **回归** admin-web 会话管理页 | 浏览器 | session 模式下拉仍能过滤（`conversation_session.mode` 表未动） |
| T11 | **回归** bench 脚本 | `bash scripts/bench/run-all-benchmarks.sh` | 注释 `/api/ai/chat` 后剩余 case 仍通过 |
| T12 | **新增** `TokenQuotaServiceTest` | 同 T1 | 测试限额检查 4 场景：per-user 覆盖 / 全局配置 / 默认 fallback / 超限拒绝 |
| T13 | **新增** `ConversationWebSocketHandlerTest` | 同 T1 | 测试异常退出清理：cancel / complete / sweepIdle / connectionClose / transportError 5 路径 |
| T14 | **新增** `OomDefenseTest` | 同 T1 | 测试 `pendingMessages` size cap + `conversationStates` size cap |
| T15 | 集成手测 token 限额 | 浏览器 | 用户用满 1M tokens → 下次 answer 时 WS 发"额度已用完"，AI 不调用 |
| T16 | 集成手测 admin-web token 限额管理 | 浏览器 | admin 改全局默认 + 单用户覆盖 → 立即生效 |
| T17 | **回归** 异常退出不污染 DB | 浏览器 + SQL | 用户答 2 题后关浏览器 → SQL 查 `qa_record` 应无该 session 记录 |

---

## 6. 实施步骤（19 步）

按依赖关系排序。Flyway 紧跟实体修改后（oracle 修订 #7）。

```
阶段 A: AI 模块简化（spec v3 主体）
1.  改 prompts/recommendation.txt（4 字段 schema）
2.  改 RecommendationResult 实体（删 3 字段 + 加 category/flavor_tags/total_tokens 3 字段）
3.  写 Flyway V6（recommendation_result 字段改 + qa_record 加 user_id + system_config + user_token_quota 4 件事合并）
4.  改 QaRecord 实体（加 userId 字段）
5.  改 ConversationAiService（4 字段解析 + 持久化新字段 + 写入 token）
6.  删 AiController + RecommendationController + 修 RecordService:117 + 修 ShareService:132
    + 删 AiService.calculateSimilarity/validateAnswer/loadPrompts 部分
    + 删 prompts/answer-validation.txt + similarity.txt
    + 删 RecordItem.similarityScore 字段
7.  改 admin-server RecommendationController（删 mode 参数 + mode 过滤）    ← 不改 ConversationQueryReq

阶段 B: Token 统计与限额
8.  新增 TokenQuotaService + QaRecord 累加（recordUsage）
9.  改 ConversationService.processAnswer 入口（checkAndReject + 调 recordUsage）
10. 改 ConversationState（加 userId + completed 字段）
11. 改 MessageTagParser（用 token 累加 + 限额检查）
12. 新增 admin-server SystemConfigController + UserTokenQuotaController + 2 service
13. 新增 admin-web token-quota 视图 + api/tokenQuota.ts + 路由

阶段 C: 异常退出 + OOM 防御
14. 改 ConversationWebSocketHandler（afterConnectionClosed / handleTransportError 调 cancelSession + state.completed）
15. 改 ConversationState（pendingMessages size cap + OOM 防御）

阶段 D: 前端
16. 改前端 ai-food-app（Result / RecordDetail / Records / FeedDetail / ResultPublishDialog / chatStore）
17. 改 admin-web 前端（recommendation/index.vue 删 3 处 + api/recommendation.ts + 新增 token-quota 视图）
18. 改 bench 脚本去 /api/ai/chat

阶段 E: 测试与提交
19. 全量测试（T1-T17）→ 提交
```

**并行机会**：
- A 组：阶段 A 步骤 1-3（prompt + 实体 + Flyway 同步推进）
- B 组：阶段 B 步骤 8-11 后端 + 步骤 12-13 前端
- C 组：阶段 C 步骤 14-15（独立）
- D 组：阶段 D 步骤 16-18 三个独立前端改

**必须串行**：
- 步骤 2 → 步骤 3 → 步骤 4 → 步骤 5（实体 → Flyway → 解析）
- 阶段 A 完成 → 阶段 B 启动（确保 DB schema 稳定后做 token 累加）
- 步骤 3 → 步骤 4 → 步骤 5（实体 → Flyway → 解析）
- 步骤 5 → 步骤 6（ConversationAiService 改完才能删 controller）

---

## 7. 风险登记

| # | 风险 | 严重度 | 缓解 |
|---|---|---|---|
| R1 | 删 endpoint 影响其他 admin / 调试路径 | 低 | 已审计：admin-server 零引用 |
| R2 | 改 prompt schema 让现有 WebSocket 主流程测试失败 | 中 | 新增 T3 专门测试 4 字段解析 |
| R3 | admin-server 删字段编译失败 | 高 | oracle 已识别 3 个 P0 编译断点，已加到删除清单 #11 #13 |
| R4 | Flyway migration 在生产跑失败 | 中 | 命名 V6 不冲突；本地先 mvn test 验证 |
| R5 | `flavor_tags` 用 VARCHAR 长度不够 | 低 | 用 MySQL JSON 类型（oracle 修订 #6） |
| R6 | 前端 chatStore 类型改动破坏其他 view | 中 | vue-tsc --noEmit 强制类型检查 |
| R7 | 删 bench 脚本 case 让 CI 误判 PASS | 低 | 注释而非删除；保留脚本可读 |
| R8 | `conversation_session.mode` 误删 | 中 | **明确保留** `ConversationQueryReq.mode`，不动 session 表 |
| R9 | `Share.vue` 引用 mode 字段 | 中 | 实施步骤前 grep 验证 |
| R10 | 老 `recommendation_result` 数据迁移后字段为 NULL | 低 | 历史 mode 字段删除，老数据自然降级；前端兜底"AI 推荐" |
| R11 | admin-web `recommendation/index.vue` 视觉验证不全 | 低 | T9 明确要求"模式列消失、相似度列消失、模式下拉消失" |
| R12 | Token 累加影响性能 | 中 | 写 QaRecord 是同步 SQL，每答一题就一次。168/168 单测验证无回归 |
| R13 | 限额检查时区错乱 | 中 | 用 `LocalDate.now()` + 业务库时区；明确用 DB 时区（MySQL `created_at` 是 DATETIME，无时区信息）；查询 `WHERE created_at >= ?` 用 `LocalDateTime.of(date, LocalTime.MIN)` |
| R14 | 异常退出时 AI 正在异步处理中 | 中 | state.isCancelled() 已经在异步任务里检查；再加 isCompleted 区分；异步任务检查 isCompleted() 后丢弃结果 |
| R15 | user_token_quota 唯一约束 vs 多用户覆盖 | 低 | user_id 设 UNIQUE；同一用户只能有一条覆盖 |
| R16 | system_config 通用 key 命名冲突 | 低 | 用 `daily_token_limit_default` 唯一 key；未来加新配置走 `xxx_xxx` snake_case 命名规范 |
| R17 | admin 改限额后用户已开始的会话不感知 | 低 | 限额查询是每次入口实时查，无需通知；改完立即生效 |
| R18 | 老用户 QaRecord 历史数据 userId 字段为 NULL | 低 | 历史 userId NULL 不影响 SUM 查询（NULL 不会被聚合）；统计排除 NULL userId |
| R19 | token 累加与 limit 检查 race condition | 中 | 限额检查用 `SUM(total_tokens)` 而非 Redis 计数器，无原子性问题；用户连续调用时可能短暂超限（误差 < 1 次调用）可接受 |
| R20 | 异常退出 cancelSession 软删时 user 已经答了 5 题 | 低 | 用户已选 A：异常断开记录不保留；可接受 |
| R21 | OOM 防御 `pendingMessages` 上限影响抢话功能 | 低 | MAX=10 足够，正常用户抢话 ≤ 3 次 |
| R22 | OOM 防御 `conversationStates` 上限影响正常用户 | 中 | MAX=10,000 远超正常用户量（活跃用户通常 < 1,000）；触顶时返回"服务繁忙" |
| R23 | `state.completed` 标志在并发场景下丢失 | 中 | boolean 写无原子性问题；handleComplete 单一入口写；读取处加 volatile |

---

## 8. 验收标准

**满足以下所有条件视为完成**：

**AI 模块简化（v3 主体）**：
- [ ] T1（ai-food-app 168/168）、T2（admin-server 基线）、T4-T6（前端 0 error）通过
- [ ] T3 新增 `ConversationAiServiceTest` 4 用例通过
- [ ] T7 集成手测 Result.vue 展示 4 字段（foodName / reason / category / flavorTags）
- [ ] T8 回归 Match 找人匹配不受影响
- [ ] T9 回归 admin-web 推荐记录页（模式列/相似度列/模式下拉消失）
- [ ] T10 回归 admin-web 会话管理页（session 模式下拉仍工作）
- [ ] T11 bench 脚本剩余 case 通过
- [ ] Flyway V6 在本地 mvn test 通过
- [ ] 老 `recommendation_result` 行的 photo / comment 仍可访问
- [ ] 9 个 dead-code 文件已删

**Token 统计与限额（v4 增量）**：
- [ ] T12 新增 `TokenQuotaServiceTest` 4 用例通过
- [ ] T15 集成手测：用户用满 1M tokens 后 WS 发"额度已用完"错误
- [ ] T16 集成手测：admin-web 改全局默认 + 单用户覆盖 → 立即生效
- [ ] `qa_record.user_id` 索引存在；按 user 聚合 token 查询 < 100ms（10 万行级）

**异常退出 + OOM 防御（v4 增量）**：
- [ ] T13 新增 `ConversationWebSocketHandlerTest` 5 路径用例通过
- [ ] T14 新增 `OomDefenseTest` 2 用例通过
- [ ] T17 回归：用户答 2 题后关浏览器 → SQL 查 `qa_record` 无该 session 记录

---

## 9. 文件影响汇总

### 9.1 后端 ai-food-app

**删除**（5 文件 + 多行代码）：
- `backend/ai-food-app/src/main/java/com/ai/food/controller/AiController.java`
- `backend/ai-food-app/src/main/java/com/ai/food/controller/RecommendationController.java`
- `backend/ai-food-app/src/main/resources/prompts/answer-validation.txt`
- `backend/ai-food-app/src/main/resources/prompts/similarity.txt`
- `backend/ai-food-app/src/main/java/com/ai/food/service/ai/AiService.java`（删 2 方法 + 2 行 loadPrompts）
- `backend/ai-food-app/src/main/java/com/ai/food/service/record/RecordService.java`（删 1 行 + RecordItem 字段）
- `backend/ai-food-app/src/main/java/com/ai/food/service/share/ShareService.java`（删 1 行）

**修改**（6 文件 + 1 新文件 + 1 新 service）：
- `backend/ai-food-app/src/main/resources/prompts/recommendation.txt`（4 字段 schema）
- `backend/ai-food-app/src/main/java/com/ai/food/common/model/RecommendationResult.java`（-3 +3 字段：category/flavor_tags/total_tokens）
- `backend/ai-food-app/src/main/java/com/ai/food/common/model/QaRecord.java`（+userId）
- `backend/ai-food-app/src/main/java/com/ai/food/dto/ConversationState.java`（+userId +completed 字段 + pendingMessages size cap）
- `backend/ai-food-app/src/main/java/com/ai/food/service/conversation/ConversationAiService.java`（解析 + 持久化 + token 累加）
- `backend/ai-food-app/src/main/java/com/ai/food/service/conversation/ConversationService.java`（processAnswer 入口加限额检查 + recordUsage）
- `backend/ai-food-app/src/main/java/com/ai/food/websocket/ConversationWebSocketHandler.java`（afterConnectionClosed/handleTransportError 调 cancelSession + state.completed 标志）
- `backend/ai-food-app/src/main/java/com/ai/food/service/token/TokenQuotaService.java`（**新增**）
- `backend/ai-food-app/src/main/resources/db/migration/V6__ai_module_simplify.sql`（**新增**）

### 9.2 后端 admin-server

**修改**（1 文件）：
- `backend/admin-server/src/main/java/com/aifood/admin/controller/RecommendationController.java`（删 mode 参数 + mode 过滤）

**新增**（3 文件 + 2 service）：
- `backend/admin-server/src/main/java/com/aifood/admin/controller/SystemConfigController.java`（**新增**）
- `backend/admin-server/src/main/java/com/aifood/admin/controller/TokenQuotaController.java`（**新增**）
- `backend/admin-server/src/main/java/com/aifood/admin/service/SystemConfigService.java`（**新增**）
- `backend/admin-server/src/main/java/com/aifood/admin/service/UserTokenQuotaService.java`（**新增**）

### 9.3 前端 ai-food-app

**修改**（6 文件）：
- `frontend/src/views/Result.vue`
- `frontend/src/views/RecordDetail.vue`
- `frontend/src/views/Records.vue`
- `frontend/src/views/FeedDetail.vue`
- `frontend/src/views/components/Result/ResultPublishDialog.vue`
- `frontend/src/stores/chat.ts`

### 9.4 前端 admin-web

**修改**（2 文件）：
- `frontend/admin-web/src/views/recommendation/index.vue`
- `frontend/admin-web/src/api/recommendation.ts`

**新增**（4 文件）：
- `frontend/admin-web/src/api/tokenQuota.ts`（**新增**：5 个接口 client）
- `frontend/admin-web/src/views/token-quota/index.vue`（**新增**：token 限额管理视图）
- `frontend/admin-web/src/router/index.ts`（**修改**：加 `/token-quota` 路由）
- `frontend/admin-web/src/layouts/DefaultLayout.vue`（**修改**：加菜单项"Token 限额"）

### 9.5 脚本

**修改**（1 文件）：
- `scripts/bench/run-all-benchmarks.sh`（注释 `/api/ai/chat` case）

---

## 10. 增量需求 · Token 统计与每日限额

### 10.1 现状

- `QaRecord` 表已有 `promptTokens` / `completionTokens` / `totalTokens` / `model` 字段（V2/V5 加的）
- `AiService.chat()` 已返回 `ChatResult(text, promptTokens, completionTokens, totalTokens, model)` — token 已在捕获
- `TokenUsageService`（admin-server）已能按 user/model/day 聚合
- **`QaRecord` 没有 `userId` 字段**（注释明示）— 当前 user 维度实际按 session 分组
- 没有任何**每日 token 限额**机制

### 10.2 改造目标

1. **统计**：每次 LLM 调用都记 `userId + tokens` 到 `QaRecord`（可按用户聚合）
2. **限额**：用户每日 token 用量超 1,000,000（默认）后，**硬拒绝**后续 AI 调用
3. **可配置**：admin 后台可设全局限额 + 单用户覆盖（per-user 优先）

### 10.3 数据模型

#### 10.3.1 `qa_record` 表加字段

**Flyway `V6__add_user_id_to_qa_record.sql`**：

```sql
ALTER TABLE qa_record
    ADD COLUMN user_id BIGINT DEFAULT NULL COMMENT '用户ID（关联 sys_user）' AFTER session_id,
    ADD INDEX idx_user_created (user_id, created_at, total_tokens);
```

**`QaRecord.java` 实体加字段**：

```java
@TableField("user_id")
private Long userId;
```

#### 10.3.2 新表 `system_config`（全局限额配置）

**Flyway `V6__create_system_config.sql`**（与 10.3.1 合并为 V6）：

```sql
CREATE TABLE system_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    config_key VARCHAR(64) NOT NULL UNIQUE COMMENT '配置 key',
    config_value VARCHAR(255) NOT NULL COMMENT '配置 value',
    description VARCHAR(255) DEFAULT NULL,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version INT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '系统级配置';

-- 初始默认 token 限额
INSERT INTO system_config (config_key, config_value, description)
VALUES ('daily_token_limit_default', '1000000', '默认每日 token 限额（per user）');
```

#### 10.3.3 新表 `user_token_quota`（单用户覆盖）

```sql
CREATE TABLE user_token_quota (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL UNIQUE COMMENT '用户ID',
    daily_token_limit INT NOT NULL COMMENT '每日 token 限额（per user）',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version INT NOT NULL DEFAULT 0,
    INDEX idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '用户 token 限额覆盖';
```

### 10.4 限额检查流程

`ConversationService.processAnswer()` 入口（userId 已知）：

```
1. user_token_quota.user_id = ?   → 命中 → userLimit
2. 未命中 → system_config['daily_token_limit_default']       → globalLimit
3. 未命中 → 硬编码 1,000,000                                   → hardcodedDefault

effectiveLimit = userLimit ?? globalLimit ?? hardcodedDefault
```

```
SELECT SUM(total_tokens) FROM qa_record
WHERE user_id = ? AND created_at >= ?  -- 当日 00:00:00
```

如果 `todayUsed + requestEstimatedTokens > effectiveLimit` → **拒绝**（WS 发 `system` 错误："今日 token 额度已用完"）

**保守策略**：预估本次调用会用 ~500 tokens（中间数），超限前就拦；防止单次超大请求把限额打爆。

### 10.5 Token 累加流程

每次 `aiService.chat()` / `aiService.generateQuestion()` / `aiService.generateRecommendation()` 返回后：

- 调用方（`ConversationAiService`、`MessageTagParser`、`ConversationService`）拿到 `ChatResult`，把 `userId + promptTokens + completionTokens + totalTokens + model` 写入 `QaRecord`（**注意**：现有的 `QaRecord` 入库点需要在保存时补 `userId`）

**重要**：4 字段 prompt schema 升级后，`saveRecommendationResult` 也要把 `totalTokens` 一并存入 `recommendation_result`（`recommendation_result` 加新字段 `total_tokens`）—— 这样**推荐结果级**的 token 也可统计

#### 10.5.1 `recommendation_result` 表加字段

```sql
ALTER TABLE recommendation_result
    ADD COLUMN total_tokens INT DEFAULT NULL COMMENT '本次推荐消耗 token' AFTER reason;
```

#### 10.5.2 `RecommendationResult.java` 实体加字段

```java
@TableField("total_tokens")
private Integer totalTokens;
```

### 10.6 Admin 后台配置接口

新增 2 个 controller（admin-server）：

| 接口 | 方法 | 路径 | 用途 |
|---|---|---|---|
| 全局限额 | `GET` | `/admin/api/config/token-limit` | 查 `system_config.daily_token_limit_default` |
| 全局限额 | `PUT` | `/admin/api/config/token-limit` | 改全局默认值 |
| 单用户覆盖 | `GET` | `/admin/api/token-quota/{userId}` | 查单用户覆盖 |
| 单用户覆盖 | `PUT` | `/admin/api/token-quota/{userId}` | 改/新建单用户覆盖 |
| 单用户覆盖 | `DELETE` | `/admin/api/token-quota/{userId}` | 删除单用户覆盖（fallback 到全局） |

admin-web 前端对应视图：复用现有 `admin-web/src/views/model/index.vue` 或新增 `token-quota/index.vue`。

### 10.7 限额时机与状态

- 限额检查位置：`ConversationService.processAnswer()` 入口 + `MessageTagParser.getQuestionContent()` 入口
- **不**检查位置：`/api/ai/chat` 已删；`/api/ai/recommend` 已删；`/api/ai/similarity` 已删
- Token 累加位置：所有调 `aiService.chat/generateQuestion/generateRecommendation` 的地方

---

## 11. 增量需求 · 异常退出处理 + OOM 防御

### 11.1 现状

`ConversationWebSocketHandler` 有 4 种退出路径：

| 路径 | 触发 | 内存清理 | DB 清理 |
|---|---|---|---|
| `handleCancel` | 用户主动 cancel action | ✅ 清 `conversationStates` | ✅ 调 `cancelSession` 软删 |
| `sweepIdle` | heartbeat 60s 超时 | ❌ 不直接清（依赖 `afterConnectionClosed`） | ❌ 不调 `cancelSession` |
| `afterConnectionClosed` | WS 正常关闭 | ✅ 清 `conversationStates` | ❌ 不调 `cancelSession` |
| `handleTransportError` | 网络异常 | ✅ 清 `conversationStates` | ❌ 不调 `cancelSession` |

**问题**：异常退出（3 种路径）只清内存 state，**DB 记录留存**——用户答了 3 题、关浏览器、记录还在 DB 里，污染数据。

### 11.2 改造目标

异常退出时**记录不保留**：3 种路径都调 `cancelSession` 软删 DB。

### 11.3 实施

#### 11.3.1 `WebSocketSessionRegistry.sweepIdle()` 改造

```java
// sweepIdle 关闭 idle session 后,afterConnectionClosed 会触发;
// 关键：afterConnectionClosed 现在会调 cancelSession 软删
public void sweepIdle() {
    // ... 现有代码,关闭 idle session
    // sweepIdle 关闭后,Spring 会触发 afterConnectionClosed
    // 在 afterConnectionClosed 里调 cancelSession 即可
}
```

#### 11.3.2 `ConversationWebSocketHandler.afterConnectionClosed()` 改造

```java
@Override
public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
    String sessionId = extractSessionId(session);
    if (sessionId != null) {
        sessions.remove(sessionId);
        registry.unregister(sessionId);
        // ponytail: 异常关闭也走 cancel 流程,DB 不留脏记录
        // 检查 state 是否已正常 complete 或 cancel,避免重复清理
        ConversationState state = conversationStates.get(sessionId);
        if (state != null && !state.isCompleted()) {
            conversationService.cancelSession(sessionId);
            log.info("[{}] abnormal close, cleaned DB records", sessionId);
        }
        conversationStates.remove(sessionId);
        log.debug("WebSocket closed: {}", sessionId);
    }
}
```

#### 11.3.3 `handleTransportError` 复用同一逻辑

复用 `afterConnectionClosed` 行为（`handleTransportError` 现有已清 state，**加 DB 软删**）：

```java
@Override
public void handleTransportError(WebSocketSession session, Throwable exception) {
    String sessionId = extractSessionId(session);
    log.error("Transport error for session {}: {}", sessionId, exception.getMessage());
    if (sessionId != null) {
        ConversationState state = conversationStates.get(sessionId);
        if (state != null && !state.isCompleted()) {
            conversationService.cancelSession(sessionId);
        }
        sessions.remove(sessionId);
        registry.unregister(sessionId);
        conversationStates.remove(sessionId);
    }
}
```

#### 11.3.4 `ConversationState.isCompleted` 字段

加 boolean 字段，跟 `isCancelled` 区分：

- `isCompleted = true`：用户正常完成 7 问 / 调 `complete` action → **不删 DB**
- `isCancelled = true`：用户主动 cancel → **删 DB**
- 都没设：异常断开 → **删 DB**（新行为）

```java
// ConversationState.java 加
private boolean completed = false;
public boolean isCompleted() { return completed; }
public void setCompleted(boolean completed) { this.completed = completed; }
```

#### 11.3.5 `handleComplete` 设置 `completed = true`

```java
private void handleComplete(WebSocketSession session, String sessionId, ConversationState state) {
    state.setCompleted(true);  // ← 新增
    // ... 现有代码
}
```

### 11.4 OOM 防御

#### 11.4.1 `state.pendingMessages` List 大小上限

防止用户狂发抢话消息堆内存：

```java
// ConversationState.java
private static final int MAX_PENDING_MESSAGES = 10;

public void addPendingMessage(String msg) {
    if (pendingMessages.size() >= MAX_PENDING_MESSAGES) {
        log.warn("[{}] pending message limit reached, dropping", sessionId);
        return;
    }
    pendingMessages.add(msg);
}
```

#### 11.4.2 `conversationStates` Map 大小上限

防止恶意大量建连接：

```java
// ConversationWebSocketHandler.java
private static final int MAX_CONCURRENT_STATES = 10_000;

private void registerState(String sessionId, ConversationState state) {
    if (conversationStates.size() >= MAX_CONCURRENT_STATES) {
        log.error("conversationStates full, refusing new session {}", sessionId);
        throw new IllegalStateException("服务繁忙，请稍后再试");
    }
    conversationStates.put(sessionId, state);
}
```

#### 11.4.3 sweepIdle 已存在（60s）

已有 `WebSocketSessionRegistry.sweepIdle()` 5s 扫描 + 60s 超时——这部分保留。

#### 11.4.4 边界 case：用户重连后记录丢失

接受这个 trade-off（用户已选 A）。如果需要未来支持"异常断开后 N 分钟内重连保留记录"，加 `incomplete_at` 字段 + cron 清理即可，**本期不做**。

---

## 12. 后续（out of scope）

本 spec 不涉及但相关：

- prompt 调优工作流（无版本管理、无 A/B、无评估指标）
- AI 错误处理（rate limit、超时、降级到静态推荐）
- 多语言 prompt（i18n）
- WebSocket 协议扩展（推荐结果是否带图片、是否流式输出）

这些等本 spec 落地后再单独 brainstorm。

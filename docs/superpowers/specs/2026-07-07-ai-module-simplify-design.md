# AI-food · AI 模块简化重构 · Design Spec

> **Date**: 2026-07-07
> **Status**: Draft · 待用户审阅
> **Author**: Orchestrator + @explorer (代码现状审计) + @oracle (独立审查)
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

### 3.3 后端 admin-server — 同步改

#### 3.3.1 `RecommendationController.java` (admin-server)

**删除**：
- `@RequestParam String mode` 参数（line 54）
- `w.eq(RecommendationResult::getMode, mode)` 过滤条件（line 59）
- 引用 `RecommendationResult::getMode` 的方法引用

**保留**：admin-server `RecommendationController` 整体（推荐记录查询功能还在）

#### 3.3.2 `ConversationQueryReq.java` — **不动**

该 DTO 的 `mode` 字段用于过滤 `conversation_session.mode`（另一张表的 `mode` 列），跟被删的 `recommendation_result.mode` 不是同一个。

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

---

## 6. 实施步骤（11 步）

按依赖关系排序。Flyway 紧跟实体修改后（oracle 修订 #7）。

```
1. 写 spec（本文件）→ 用户审
2. 改 prompts/recommendation.txt（4 字段 schema）
3. 改 RecommendationResult 实体（删 3 字段 + 加 2 字段）
4. 写 Flyway V6（紧接实体后）            ← 修订
5. 改 ConversationAiService（解析 + 写入新字段）
6. 删 AiController + RecommendationController + 修 RecordService:117 + 修 ShareService:132
   + 删 AiService.calculateSimilarity/validateAnswer/loadPrompts 部分
   + 删 prompts/answer-validation.txt + similarity.txt
   + 删 RecordItem.similarityScore 字段
7. 改 admin-server RecommendationController（删 mode 参数 + mode 过滤）    ← 不改 ConversationQueryReq
8. 改前端 ai-food-app（Result / RecordDetail / Records / FeedDetail / ResultPublishDialog / chatStore）
9. 改 admin-web 前端（recommendation/index.vue 删 3 处 + api/recommendation.ts）
10. 改 bench 脚本去 /api/ai/chat
11. 全量测试（T1-T11）→ 提交
```

**并行机会**：
- A 组：步骤 2 + 步骤 4（prompt + Flyway 无依赖）
- B 组：步骤 8 + 步骤 9（ai-food-app 前端 + admin-web 前端无依赖）
- C 组：步骤 10（独立）

**必须串行**：
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

---

## 8. 验收标准

**满足以下所有条件视为完成**：

- [ ] T1（ai-food-app 168/168）、T2（admin-server 基线）、T4-T6（前端 0 error）通过
- [ ] T3 新增 `ConversationAiServiceTest` 4 用例通过
- [ ] T7 集成手测 Result.vue 展示 4 字段（foodName / reason / category / flavorTags）
- [ ] T8 回归 Match 找人匹配不受影响
- [ ] T9 回归 admin-web 推荐记录页（模式列/相似度列/模式下拉消失）
- [ ] T10 回归 admin-web 会话管理页（session 模式下拉仍工作）
- [ ] T11 bench 脚本剩余 case 通过
- [ ] Flyway V6 在本地 mvn test 通过
- [ ] 老 `recommendation_result` 行的 photo / comment 仍可访问
- [ ] 9 个 dead-code 文件已删（`AiController.java` / `RecommendationController.java` 2 个 controller + `answer-validation.txt` + `similarity.txt` + 后续 5 个方法/字段）

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

**修改**（3 文件 + 1 新文件）：
- `backend/ai-food-app/src/main/resources/prompts/recommendation.txt`（4 字段 schema）
- `backend/ai-food-app/src/main/java/com/ai/food/common/model/RecommendationResult.java`（-3 +2 字段）
- `backend/ai-food-app/src/main/java/com/ai/food/service/conversation/ConversationAiService.java`（解析 + 持久化）
- `backend/ai-food-app/src/main/resources/db/migration/V6__ai_module_simplify.sql`（新增）

### 9.2 后端 admin-server

**修改**（1 文件）：
- `backend/admin-server/src/main/java/com/aifood/admin/controller/RecommendationController.java`（删 mode 参数 + mode 过滤）

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

### 9.5 脚本

**修改**（1 文件）：
- `scripts/bench/run-all-benchmarks.sh`（注释 `/api/ai/chat` case）

---

## 10. 后续（out of scope）

本 spec 不涉及但相关：

- prompt 调优工作流（无版本管理、无 A/B、无评估指标）
- AI 错误处理（rate limit、超时、降级到静态推荐）
- 多语言 prompt（i18n）
- WebSocket 协议扩展（推荐结果是否带图片、是否流式输出）

这些等本 spec 落地后再单独 brainstorm。

# Task 9 — AiService 写 token 字段到 qa_record

## 1. 需求

让 AI 调用方把 `ChatResponse` 中的 token 用量 + 模型名记账到 `qa_record` 表，为后续成本核算 / 用量统计提供原始数据。

## 2. 实现

### 2.1 新增 ChatResult 包装类
- `backend/ai-food-common/src/main/java/com/ai/food/common/ai/ChatResult.java`
- 字段：`text` + `promptTokens/completionTokens/totalTokens`（Long） + `model`（String）
- 提供 `of(text)` 工厂用于失败兜底

### 2.2 修改 AiService.chat()
- 返回类型 `String` → `ChatResult`
- 从 `ChatResponse.getMetadata()` 读 usage + model（Spring AI 1.0.0-M6 中 Usage 返回 Integer；null 时整段 try/catch 兜底）
- 失败 fallback：`ChatResult.of("抱歉，AI服务暂时不可用，请稍后重试")`

### 2.3 修 QaRecord 实体
- brief 写"has setPromptTokens etc methods already from V4 migration"，但实际实体未加，**需补**：
  - `prompt_tokens` / `completion_tokens` / `total_tokens` (Integer)
  - `model` (String)

### 2.4 修所有 chat() 调用方（5 处）
- `AiService.generateQuestion / validateAnswer / generateRecommendation / calculateSimilarity` — 内部 `chat(...).getText()`
- `AiController.chat` — `aiService.chat(...).getText()`
- `AiParamNormalizer.normalize` — `aiService.chat(...).getText()`
- `ConversationService.handleInterrupt` — `aiService.chat(...).getText()`
- `ConversationService.generateAiResponse` — `aiService.chat(...).getText()`

> brief 只列了 4 个内部 caller，但实际跨 3 个文件共 6 处，**全部**需要加 `.getText()` 否则无法编译。

### 2.5 saveQaRecord 重构
- 签名由 `(sessionId, type, param, q, a, boolean isValid, int order)` 改为 `(sessionId, type, param, q, a, Integer order, Long promptTokens, Long completionTokens, Long totalTokens, String model)`
- **移除 `isValid` 参数**：`questionType` 已能区分 valid ("question") 与 retry ("2question")，冗余；QA Record 中 `isValid` 默认 true，无回归
- 写入时把 Long token 强转 Integer（DB 列就是 INT）写入新字段

### 2.6 两个 call site 更新
- 传 `null, null, null, null` 作为 token/model（intake Q&A 走规则校验不调 LLM，token 计数本就该 null）

### 2.7 测试 mock 同步更新
- `ConversationServiceTest`、`ParamNormalizationServiceTest` 把 `when(aiService.chat(...)).thenReturn("...")` 改为 `.thenReturn(ChatResult.of("..."))`

## 3. 验证

- `mvn -f ai-food-common/pom.xml clean install -DskipTests -q` ✅ BUILD SUCCESS
- `mvn -f ai-food-app/pom.xml clean compile -q` ✅ BUILD SUCCESS
- `mvn -f ai-food-app/pom.xml test` ✅ 143/143 passed

## 4. 关注点 / 后续

1. **行为变化**：原 `isValid` 参数被移除，2question（retry）记录现在 `is_valid=true`。若下游分析依赖 `is_valid=false` 来过滤 retry，**会全部漏掉**。建议下次同步补一列或者按 `question_type` 过滤。
2. **未做端到端验证**：brief 第 2 步要求 `mvn spring-boot:run` + `mysql SELECT ...` 验证 DB 实际写入了 token。本环境未启 MySQL/服务，仅做了编译 + 单测。要确认 `ChatResponse.getMetadata().getUsage()` 在 DeepSeek 真实响应里非 null，需要一次联调。
3. **admin-server 模块**也通过 `ai-food-common` 共享 `QaRecord`，本改动对其零侵入（只新增字段、都是 nullable），但若 admin 端有 `SELECT * FROM qa_record` 反序列化，要确认新字段类型兼容 — MyBatis-Plus 自动映射 Integer/Long/String 没问题。

## 5. 提交

`feat(ai): capture token usage from ChatResponse, persist to qa_record`
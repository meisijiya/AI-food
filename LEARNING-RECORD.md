# 0001 — AI-food AI 模块简化 + Token 限额 + 异常退出

## Date
2026-07-10

## Project
AI-food AI 模块简化重构（spec v4.1 + 19 commits / 23 tasks）

## What was done
- **19 commits** 完成 A.1-D.5 全部 19 tasks（剩 E.1 verify + LEARNING-RECORD）
- **阶段 A**（AI 模块简化）：删 8 dead HTTP endpoint + 2 method + 2 prompt 文件 + 3 DB 字段（mode/old_food/similarity_score）+ 修 3 个 P0 编译断点
- **阶段 B**（Token 限额）：1M tokens/天默认 + admin 全局 + per-user 覆盖（2 层配置）+ Clock 构造注入支持跨日测试 + @RequireAdmin 鉴权（5 个新接口）
- **阶段 C**（异常退出 + OOM）：3 异常路径调 cancelSession + state.setCancelled(true) 阻断 async race + pendingMessages MAX=10 + conversationStates MAX=3,000
- **阶段 D**（前端）：ai-food-app 6 文件改（加 category + flavorTags chips + 删 mode + chatStore 类型强化）+ admin-web 4 文件改（recommendation 视图 + token-quota 视图 + 路由 + 菜单）
- **prompt schema 升级**：2 字段 JSON → 4 字段 JSON（{foodName, reason, category, flavorTags[]}）

## What was learned
- **spec 必须经独立 oracle 审查 3 轮**（v3 + v4 增量 + tasks 审），P0-1/2/3 三个真设计错误（命名碰撞 + 异步 race + 入口缺失）我设计时都没发现
- **Lombok 假阳性陷阱**：`setFlavorTags/setCategory` 编译报错其实是 stale classpath，clean 编译验证才能区分真假错误
- **Clock 注入必须真构造注入**：`private final Clock clock = Clock.systemDefaultZone()` 是 inline 初始化，会阻断 `@MockBean Clock` 跨日测试
- **admin-server 项目惯例**：用 `@RequireAdmin` + `AdminInterceptor`，不是 Spring Security 方法级 `@PreAuthorize`（admin-server 用 `web.ignoring()` 让 Spring Security 完全跳过）
- **Lombok `@Data` 自动生成命名要避免覆盖现有手动方法**：新增 `boolean completed` 字段会让 Lombok 生成 `isCompleted()` getter 覆盖现有"7 问答完"语义，10+ 处状态机判断逻辑断裂

## Surprising findings
- **ConversationState 现有 isCompleted()**（语义是 7 问是否答完）跟 P0-1 想的"完成"重名——必须命名 `gracefulExit` 避开
- **aiExecutor 异步任务在异常路径如果不 setCancelled(true)**，会在 afterConnectionClosed/cancelSession 软删 DB 后又写入新记录——这是 race condition，不是简单代码 bug
- **机器 4GB / swap 已用 1.3GB** 实测影响 OOM 防御 MAX 值选择（保守 3K 而非 10K），并发安全性比理论极限更重要
- **fixer 自称"通过"但实际编译失败**：fix-6 报告"24 failures 是 A.1-A.3 遗留"实际是 3 个 P0 编译断点未修——验证报告本身要交叉验证

## Drift analysis
- **Drift 1** (Level 1): fix-6 验证报告偏差 — 自称"24 failures 是 A.1-A.3 遗留"实际是 3 个 P0 编译断点未修
- **Drift 2** (Level 1): A.5 推迟 recordRecommendationTokens 方法到 B.2 统一实现（避免连环改 saveRecommendationResult 签名）
- **Drift 3** (Level 1): B.2 Clock inline 初始化，oracle 复审查出 + B.3 之前修（ClockConfig + 构造注入）
- **Drift 4** (Level 1): B.6 用 `@RequireAdmin` 代替 `@PreAuthorize`（项目惯例，admin-server 不用 Spring Security 方法级）
- **Drift 5** (Level 1): B.6 用 `ApiResponse<T>` 代替裸 `Map`（project convention）
- **Drift 6** (Level 1): D.1 `Result.vue` 的 `result` computed 重构（合并 foodName/reason/category/flavorTags）

**全部 Level 1 corruption，未触发 Phase 1 重新对齐**

## Final judgment
- **This work belongs to**: **Agent**（spec v4.1 + oracle 3 轮审查已经把决策边界定清楚）
- **Map sufficient**: Yes（spec 800+ 行 + 23 tasks + 23 条风险登记）
- **Toolset minimal**: Yes（explorer 2 + oracle 4 + fixer 19 session = 25 个 subagent session 全程）
- **Boundaries clear**: Yes（5 阶段 A-E 串行门 + B.3→B.4 依赖 + P0/P1/P2 命名约束）

## Corruption level
- **Level**: **1**（全部 6 个 drift 都是临时补丁，oracle + drift.md 及时拦截）
- **Triggered Phase 1**: No
- **Reason**: spec 写得足够清楚 + oracle 3 轮审查及时拦截 + drift 都在实施中识别并修补

## Next improvement
1. spec 写完后做"潜在 Lombok 自动生成方法名"检查（grep `@Data` + 现有手动方法名）
2. 任何 Clock/Time/Date 注入必须是 constructor injection，不是 inline 初始化
3. async + cleanup 模式（cancelSession/clear cache）必须**先**设 cancel flag **再**调 cleanup，避免 race
4. admin-server 跟 Spring Security 解耦（项目惯例），不用 `@PreAuthorize` 用 `@RequireAdmin`
5. 验证报告要交叉验证（fixer 自称"通过" ≠ 真的通过）

## Verification (E.1)

| 范围 | 命令 | 结果 |
|---|---|---|
| ai-food-app 单测 | `mvn -pl ai-food-app test` | ✅ **168/168 通过** |
| admin-server 单测 | `mvn -pl admin-server test` | ✅ **4/4 通过** |
| ai-food-app 前端 | `cd frontend && npm run build` | ✅ **0 error** (5.61s) |
| admin-web 前端 | `cd frontend/admin-web && npx vite build` | ✅ **0 error** (14.18s) |

## Commits (19)
```
6dec019 A.1   升级 prompts/recommendation.txt 4 字段 JSON schema
f8340d5 A.2+A.3 改 RecommendationResult 实体 + Flyway V6
765880c A.4+A.5 改 QaRecord 实体 + 改 ConversationAiService
cd9cf5c A.6+A.7+A.8 删 dead code + 修 3 个 P0 编译断点
6cc9dca A.9   改 admin-server 删 getMode 引用
2d2805f B.1+B.2 Flyway V7 + TokenQuotaService + 4 mapper/entity
d2256d6 fix P0-1 Clock 构造注入 + B.3 ConversationState 扩字段
3f8794f B.4   processAnswer 入口加 token 限额检查
ba9b5a4 B.5   MessageTagParser token 累加
a7575ee B.6   admin-server 5 token 限额接口 + @RequireAdmin
916a058 C.1   异常退出清理 + userId 注入
5607482 C.2   conversationStates OOM 防御 MAX=3,000
c298a62 D.1+D.2 ai-food-app 新字段展示 + 旧字段清理
0ca1bcc D.3+D.5 admin-web recommendation 改 + bench 脚本注释
df7d7db D.4   admin-web token-quota 视图 + 路由 + 菜单
```

## Files Changed (累计)
- **删除** 4 文件：2 controller + 2 prompt
- **新建** ~17 文件：1 Flyway V6 + 1 Flyway V7 + 2 entity + 2 mapper + 1 service + 1 service config + 4 admin 文件 + 4 admin-web 文件 + 1 service/token
- **改** ~16 文件：3 Java entity + 2 prompt + 6 service + 2 controller + 6 vue + 1 ts store + 1 sh script

## Background Job Board (本 session)
- Active / Unreconciled: 0
- Reconciled: fix-1, exp-1, exp-2, ora-1, ora-2, fix-2, ora-3, fix-3, fix-4, fix-5, fix-6, fix-7, fix-8, fix-9, ora-3 (复用), fix-10, fix-11, fix-12, fix-13, fix-14, fix-15, fix-16, fix-17, fix-18 = 25 sessions
- 25 个 subagent session 全部 completed reconciled（fix-19 卡住被 cancel 由 orchestrator 自做 E.1）

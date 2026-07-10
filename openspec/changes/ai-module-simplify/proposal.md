# Proposal: AI-food AI 模块简化 + Token 限额 + 异常退出处理

> **Status**: Phase 1 (Align) 完成 · spec v4.1 commit f760e41
> **Spec**: `docs/superpowers/specs/2026-07-07-ai-module-simplify-design.md`
> **Brainstorming sessions**:
> - `ses_0b5ed3c8effeLyp5R5MmHuwV7s` (@explorer · AI 模块代码现状审计)
> - `ses_0b5ed1306ffeh4oRYWtewo9w3w` (@explorer · admin-server 审计)
> - `ses_0b5626731ffeBxYwoNbm94rVY5` (@oracle · v3 独立审 APPROVE-WITH-CHANGES)
> - `ses_0b509c78bffez9rfpF9vhggisL` (@oracle · v4 增量审 APPROVE-WITH-CHANGES, 12 修订全部吸收)

## Trigger Conditions (Five Soul Question ❶)

**MUST start when**：
- AI 模块 dead code（8 个 HTTP endpoint、2 个 method、3 个 prompt、3 个 DB 字段）确认无生产调用
- Token 用量需要统计 + 每日限额（1M tokens/天，admin 可全局 + 单用户覆盖）
- 异常退出需要清理（sweepIdle / connectionClose / transportError 三个路径）
- OOM 防御需要（state.pendingMessages + conversationStates 大小上限）

**MUST NOT start when**：
- 用户还在用 inertia/random 双 mode 模式（已确认无前端 UI）
- 用户没有 token 限额需求（已确认默认 1M，admin 可调）
- 机器内存 > 8GB（4GB 机器 + swap 已用 1.3GB 必须做 OOM 防御）

## Done Criteria (Five Soul Question ❷)

**Observable result**:
- T1-T18 全部通过（mvn test 168/168 + admin test + 前端 0 error + 集成手测 + 回归）
- 8 个 dead HTTP endpoint 全部删除（grep `/api/ai/` 和 `/api/recommendation/` 在生产代码 = 0）
- 3 个死字段从 DB 删除（`mode` / `old_food` / `similarity_score`）
- 2 个新 prompt schema 字段（category / flavorTags）入库并前端展示
- Token 限额 admin 后台可视化可改
- 异常退出 SQL 验证不污染 DB（T17）

**NOT done when**：
- 只删 endpoint 没改 prompt schema
- 只加 userId 字段没做限额检查
- 只调 cancelSession 没设 setCancelled(true)（P0-2 race condition）
- 命名 `completed` 字段（覆盖现有 isCompleted() 7 问语义）

## Minimal Toolset (Five Soul Question ❸)

**Required**:
- 后端 Java：Spring Boot + MyBatis-Plus + Spring Security + WebSocket + Flyway
- 前端：Vue 3 + Vant 4 + TypeScript + 设计令牌
- 数据库：MySQL 8.0（cloud 内网直连）
- AI：DeepSeek API（已有）
- 工具：mvn / npm / vite / rtk / git
- Skills: brainstorming, oracle (P0 review), fixer (impl), explorer (audit), vibe-loop

**Noise (avoid)**:
- 任何"未来可能用"的抽象层（YAGNI）
- 新增依赖（npm/maven 不变）
- 微服务拆分（保持单体）
- 队列（Redis 已够）
- 任何模式选择 UI 改动（前端已无 inertia/random 切换）

**Stack note**: 本项目用 `mvn -pl <module>` 而非根 `mvn test`，用 `npm run build` + `vue-tsc --noEmit`，不引入 Python/Go。

## Information Gap Fallback (Five Soul Question ❹)

**Stop and admit "don't know" when**:
- 不确定 8 个 dead endpoint 是否真的无人调 → 已用 2 份 explorer 报告交叉验证
- 不确定 admin-server 是否依赖被删字段 → 已审计零引用
- 不确定 WebSocket race condition 行为 → 已 P0-2 修订先 setCancelled(true) 阻断
- 不确定 `isCompleted()` 命名是否冲突 → 已 P0-1 改 gracefulExit

**Not stop**:
- 已知死代码（直接删）
- 已知 schema 升级（按 spec 改）
- 已知限额值（1M tokens，admin 可调）

## Mandatory Human Approval (Five Soul Question ❺)

**Step 必须等用户批准**:
- Step 5: 删 AiController + RecommendationController 整个文件（无回滚）
- Step 6: Flyway V6 + V7 在生产执行（不可逆 DDL）
- Step 8: 新增 admin-web token-quota 视图（鉴权 + UI 风格）
- Phase 3 (Teach): LEARNING-RECORD.md 写完后

**可以自主推进**:
- Step 1-4: prompt 升级、实体改、Flyway 写（本地 mvn test 验证）
- Step 7: admin-server RecommendationController 改（编译可验证）
- Step 9-15: 各 service 改（单测验证）

## MVP (Minimum Viable Product)

**核心（必做）**:
- 8 endpoint + 2 method + 2 prompt + 3 字段删除
- prompt 4 字段 schema 升级
- QaRecord.user_id + 复合索引
- Token 限额检查（硬拒绝）
- 异常退出 cancelSession
- OOM 防御 MAX=3000

**Cut (非核心，本期不做)**:
- prompt A/B 调优工具链
- token 用量 admin 图表聚合（已有但本期不优化）
- 异常断开后 N 分钟重连保留记录（用户已选 A：立即清）
- WebSocket 协议升级（流式输出等）
- 多语言 prompt

## Phase 2 (Build) Entry Criteria

- [x] Phase 1 (Align) 完成（spec v4.1 commit f760e41）
- [x] Oracle 独立审 2 轮（v3 + v4）全部吸收
- [x] 用户批准 v4.1 进入实施
- [ ] tasks.md 生成 + 审 + 用户批准
- [ ] drift.md 初始化（实施中跟踪）
- [ ] Phase 2 Build 开始

## References

- spec v4.1: `docs/superpowers/specs/2026-07-07-ai-module-simplify-design.md` (800+ 行)
- AGENTS.md (项目级): `/home/ubuntu/projects/AI-food/AGENTS.md`
- 已知陷阱 (Lombok 假阳性、bash nohup 等等)

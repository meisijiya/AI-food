# Drift Log — AI Module Simplify

> **Phase**: Phase 2 (Build) 初始
> **Status**: 占位文件，build 实施中遇 spec 偏差时记录

## Four Stages of Corruption 等级阈值

- **Level 1 (Temporary patch illusion)**: 允许快速修复 + 记录到本文件
- **Level 2 (Context window squeezed)**: 触发"refactor instead of patch"信号
- **Level 3 (Old-new rule conflict)**: 强制回到 Phase 1 重新对齐
- **Level 4 (Passive patch death loop)**: 终止，回到 Phase 1 完整重设计

## 当前 drift 记录

### A.5: recordRecommendationTokens 推迟到 B.2

- **Spec 引用**: §3.2.7 推荐级 token 累加
- **问题**: `saveRecommendationResult` 不接 `ChatResult` 参数，改签名会触发连环改动（调用方 `ConversationService.generateRecommendationMessage` 也要改）
- **决策**: 按 task spec 简化版指示，推迟到 B.2 统一在 `TokenQuotaService` 实现
- **Level**: 1（Temporary patch illusion — 已在 task spec 中明确标记）

### A.5 fix-6 验证报告偏差

- **Spec 引用**: §3.1 #11 #12 #13
- **问题**: A.5 报告"24 failures 是 A.1-A.3 遗留"有误。实际 failures 来自 3 个未修文件的 P0 编译断点：
  - `RecommendationController.java`: getOldFood() + getSimilarityScore() — A.7 删文件后消
  - `RecordService.java`: getSimilarityScore() — A.8 修
  - `ShareService.java`: getMode() — A.8 修
- **决策**: 在当前 task（A.6+A.7+A.8）中一并修复；不再追回 A.5 阶段重测。
- **Level**: 1（事实偏差，已在当前 task 修正，不触发回退）


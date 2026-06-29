# 🤖 OpenCode 并行开发三档自动化光谱

> 本文档解析 OpenCode 并行开发的**三种自动化层级**，帮你判断每个项目该用哪一档。
>
> 核心问题：并行开发需要我自己去各个工作区指挥吗？还是只需要在 master session 就行？还是必须自己创建并对齐？

---

## 🎯 一句话答案

**你可以完全不动手就完成并行开发**——但需要 AI 帮你"调度"。具体分 **三档自动化光谱**：

| 档次 | 你的参与度 | AI 的角色 | 适用人群 |
|---|---|---|---|
| 🟢 第 1 档：纯手动 | 100%（全做） | 无 | 传统开发者、不信任 AI |
| 🟡 第 2 档：半自动 | 60%（创建+切换+合并） | 沙盒内写代码 | 想用 AI 但保留控制感 |
| 🔴 第 3 档：全自动 | 20%（只做决策） | 全程调度 | 想解放双手、信任 AI |

---

## 🟢 第 1 档：纯手动（最累）

**所有事都你自己干**：

### 完整流程

```bash
# 1. 自己创建 worktree
git worktree add ../AI-food-feat-login -b feature/login

# 2. 自己切到那个目录开发
cd ../AI-food-feat-login
# 写代码、测试、提交...

# 3. 切回 master 同步另一个
cd ~/projects/AI-food
git worktree add ../AI-food-feat-recommend -b feature/recommend
cd ../AI-food-feat-recommend
# 写代码...

# 4. 自己合并回来
cd ~/projects/AI-food
git merge feature/login
git merge feature/recommend
```

### 优点 / 缺点

| 优点 | 缺点 |
|---|---|
| ✅ 完全掌控 | ❌ 累，所有事都自己干 |
| ✅ 传统习惯 | ❌ 切目录切到眼花 |
| ✅ 不依赖 AI | ❌ 无法利用 AI 并行能力 |

**适用场景**：想完全掌控、传统开发习惯、不信任 AI 的复杂项目。

---

## 🟡 第 2 档：半自动（推荐新手）

**你创建 worktree，AI 在每个沙盒里帮你写代码**：

### 操作流程

#### Step 1：在 master session 下达指令

> "帮我开两个 worktree：feature/login 和 feature/recommend"

AI 执行 `git worktree add` 创建两个沙盒。

#### Step 2：手动切换 session

在 OpenCode 侧边栏点击不同的沙盒 session（`gentle-lagoon`、`gentle-panda` 等）。

#### Step 3：在每个沙盒里 AI 干活

在沙盒 A session 里说：
> "帮我实现用户登录功能"

AI 在那个 worktree 目录里写代码、测试、提交。

#### Step 4：你自己合并回 master

```bash
cd ~/projects/AI-food
git merge feature/login
git merge feature/recommend
```

### 优点 / 缺点

| 优点 | 缺点 |
|---|---|
| ✅ 仍保留人工切换感 | ⚠️ 还是要手动切 session |
| ✅ 创建/合并自己控 | ⚠️ 多 session 切换繁琐 |
| ✅ 每个沙盒看得清 | ⚠️ 无法真正并行 |

**适用场景**：想用 AI 但保留人工切换感的开发者、新手过渡期。

---

## 🔴 第 3 档：全自动（OpenCode 杀手锏）

**你只在 master session，AI 用 subagent 并行调度多个沙盒**：

### 操作流程（一句话搞定）

你在 master session 直接说：

> "并行做两个功能：登录 + 推荐，每个独立 worktree，完成后自动合并回 master"

### AI 自动执行的步骤

1. **创建 worktree A**（feature/login）
2. **创建 worktree B**（feature/recommend）
3. **派 subagent #1** 进沙盒 A 干活（@fixer agent）
4. **派 subagent #2** 进沙盒 B 干活（@fixer agent）
5. 两个 subagent **同时跑**，互不干扰
6. 都完成后，**AI 帮你合并回 master**

你全程在 master session，**不需要切换**。

### 优点 / 缺点

| 优点 | 缺点 |
|---|---|
| ✅ 不用切来切去 | ⚠️ subagent 改同一文件可能冲突 |
| ✅ AI 自己处理冲突、测试、提交 | ⚠️ 需信任 AI 的合并决策 |
| ✅ 多个功能同时推进，效率爆炸 | ⚠️ 复杂任务先 brainstorm + design |

**适用场景**：信任 AI、想解放双手、任务明确可并行。

---

## 🛠️ 第 3 档背后的机制（项目已具备）

你的项目里装了几个关键 skill，正好支持全自动档：

| Skill / Agent | 作用 |
|---|---|
| `subagent-driven-development` | 派多个 subagent 并行干活 |
| `dispatching-parallel-agents` | 调度多个 agent 到不同任务 |
| `using-git-worktrees` | 自动创建/管理 worktree |
| `comet` workflow | 完整流程：open → design → build → verify → archive |
| `@fixer` agent | 实际写代码的"工人" |
| `@oracle` agent | 架构决策和 code review |

> 这些组合起来，就是 **"AI 调度 + subagent 执行 + worktree 隔离"** 的全自动并行开发。

---

## 🤔 三档对比：你需要做什么？

| 档次 | 你要做的事 | AI 要做的事 |
|---|---|---|
| 🟢 手动 | 创建 worktree、切目录、写代码、合并 | 无 |
| 🟡 半自动 | 创建 worktree + 切换 session + 合并 | 在沙盒内写代码 |
| 🔴 全自动 | **只做决策**：做啥、合并时机、要不要合 | 全程调度（创建+执行+合并） |

---

## ⚠️ 第 3 档的真实使用感受

### 真实痛点

1. **subagent 之间冲突**：如果两个 agent 改同一文件，合并时会冲突
2. **AI 决策风险**：AI 可能合并一些你没准备好合的代码
3. **复杂任务需要规划**：上来就全自动会翻车

### 缓解策略

```bash
# 1. 先 brainstorm（明确需求）
# 2. 再 design（架构设计）
# 3. 然后 build（拆任务给 subagent）
# 4. 最后 verify（人工 review）
```

> 💡 **Comet 工作流** 就是这个套路：open → design → build → verify → archive，五步走完才放心。

---

## 🎯 针对不同人的建议

| 你的状态 | 推荐档位 | 理由 |
|---|---|---|
| 🔰 新手 | 🟡 半自动 | 边用 AI 边学 worktree |
| 🧑‍💻 熟练 | 🔴 全自动 | 解放双手，专注决策 |
| 🧑‍🔬 高风险改动 | 🟢 手动 | 全程自己盯，零信任 AI |
| 🏢 团队协作 | 🔴 + 🟢 混合 | 高风险手动，低风险自动 |

---

## 📋 速查表

| 你的问题 | 答案 |
|---|---|
| 并行开发要去各个工作区指挥吗？ | ⚠️ 看你选哪档 |
| 能不能只在 master session？ | ✅ 第 3 档可以 |
| 必须自己创建 worktree 吗？ | ⚠️ 第 1/2 档要，第 3 档 AI 创建 |
| 必须自己合并吗？ | ⚠️ 第 1/2 档要，第 3 档 AI 合并 |
| 怎么上第 3 档？ | ✅ 用 comet workflow + subagent-driven-development |

---

## 🔗 相关文档

- [Git Worktree 沙盒开发完全指南](./git-worktree沙盒开发指南.md)
- [同步分支概念详解](./同步分支概念详解.md)
- [合并 vs 同步：本质量化对比](./合并vs同步-本质区别.md)

---

> 📅 文档生成时间：2026-06-29
> 🎯 核心一句话：**新手用 🟡，熟练用 🔴，高风险用 🟢。第 3 档 = AI 调度 + subagent 并行 + worktree 隔离**。
# 🚀 Git Worktree 沙盒开发完全指南

> 本文档基于 OpenCode 工作区机制整理，适用于需要**并行开发多个功能**或**AI 协作隔离**的场景。
>
> 适用版本：OpenCode 当前版本 ｜ 适用人群：个人开发者 / 协作小团队

---

## 📌 核心概念速览

| 概念 | 是什么 | 在哪 |
|---|---|---|
| **本地 (master)** | 主 worktree，git 主工作区 | `~/projects/AI-food` |
| **沙盒 (sandbox)** | OpenCode 创建的副 worktree，**不是 git branch** | 同 repo 下的独立目录 |
| **沙盒 ID** | `opencode/<形容词-动物>` 格式的友好名 | 侧边栏显示用 |
| **真实分支名** | 沙盒对应的 git 分支名（如 `feature/login`） | worktree 目录里看 |

> 💡 **关键认知**：UI 把"沙盒"和"git 分支"画成同一个图标，**但语义完全不同**。判断方法：**看前缀**。`本地:` 是 git 分支，`沙盒:` 是 OpenCode 沙盒 ID。

---

## 🧱 沙盒的真实身份：Git Worktree

每个 `沙盒: opencode/<name>` = **1 个 git worktree** = **1 个独立分支 + 1 个独立文件系统副本**。

OpenCode SDK 源码里的定义：

> *"Create a new git worktree for the current project and run any configured startup scripts."*

也就是说：
- ✅ 沙盒之间**物理隔离**，互不串扰
- ✅ 每个沙盒有独立的文件系统视图
- ✅ 共享同一个 `.git` 目录（同一个 repo）
- ❌ 沙盒之间**不会自动同步文件变更**

---

## 📍 你现在在哪开发？

- 你日常在 `本地: master` 工作区写代码，对应目录 `~/projects/AI-food`，git 分支为 `master`
- 侧边栏里看到的 `沙盒: opencode/gentle-lagoon` 等，是 OpenCode **之前替你自己开过、或别的会话开过的临时工作区**
- 这些沙盒**不会自动同步到本地**，是物理隔离的副本

---

## 🚀 并行开发多个功能，怎么搞？

### 方案 A：让 AI 帮你开沙盒（推荐）

直接对 AI 说：

> 帮我并行开发两个功能：
> - 沙盒 A：做用户登录（feature/login）
> - 沙盒 B：做菜谱推荐（feature/recommend）
> 每个功能独立沙盒，互不打架。

OpenCode 会自动：
1. 在主 worktree 上执行 `git worktree add ../AI-food-feature-login -b feature/login`
2. 再执行 `git worktree add ../AI-food-feature-recommend -b feature/recommend`
3. 两个沙盒**同时跑**、**互不污染**——甚至可以让两个 AI 分别进不同沙盒干活

### 方案 B：自己手动开 Worktree（更可控）

```bash
cd ~/projects/AI-food

# 创建一个新 worktree 对应新分支
git worktree add ../AI-food-feat-login -b feature/login
git worktree add ../AI-food-feat-recommend -b feature/recommend

# 查看所有 worktree
git worktree list
```

然后在 OpenCode 侧边栏就能看到这两个新工作区。

---

## 🎁 并行开发的核心好处

- ✅ 切到 A 沙盒改文件，**完全不影响** B 沙盒和本地 master
- ✅ 每个沙盒跑独立的 `npm run dev` / 测试，**端口互不冲突**
- ✅ 一个功能搞砸了，**直接 rm -rf 沙盒目录**，主分支毫发无损
- ✅ 多个 AI agent 可以同时进不同沙盒（subagent-driven-development 模式就是干这个的）

---

## ❓ 灵魂拷问：master 领先了要不要重建沙盒？

### 直接答案

**不需要重建沙盒。**

master 领先 N 个版本 ≠ 沙盒要重建。沙盒只是一个"目录 + 当前分支指针"，分支是可以追着 master 同步的，工作区本身不需要换。

### 为什么？

```
master:           A---B---C---D---E---F   ← 主分支往前走了
                       \
feature/login:          X---Y---Z         ← 你的沙盒分支还停在 C 之后
```

你的沙盒（worktree）只是把 `feature/login` 这个分支"挂"在一个独立目录里给你用。master 往前走时：

- ✅ 你的分支不变（还停在 C 后面）
- ✅ 你的工作目录不变（还在 Z 那次提交的文件状态）
- ❌ master 的新提交 F 不会自动跑进你的工作区——因为你切的是 `feature/login`，不是 `master`

所以沙盒**完全不需要重建**，只需要让 `feature/login` 跟上 master 即可。

---

## 🔧 三种"同步 master"的姿势

### 姿势 1：Rebase（推荐，提交历史干净）

```bash
# 在你的沙盒工作目录里
cd ~/AI-food-feat-login     # 或 opencode/gentle-lagoon 对应的目录
git fetch origin
git rebase master
```

**效果**：`feature/login` 变成基于最新的 master，像从没分叉过。

### 姿势 2：Merge（保守，不改历史）

```bash
git fetch origin
git merge origin/master     # 或 git merge master
```

**效果**：多一个 merge commit，历史清楚但会"分叉再合拢"。

### 姿势 3：用 OpenCode 自带的 Reset（一键回到当前 master）

OpenCode 有内置 API：

> *"Reset a worktree branch to the primary default branch."*

也就是 UI 里某个按钮能把你的沙盒分支**直接重置成当前 master**——适合"我这功能不想要了，砍掉重来"的场景。

---

## 🤔 什么时候才要重建沙盒？

| 场景 | 要不要重建 | 处理方式 |
|---|---|---|
| master 领先 5 个提交，你还在开发 feature/login | ❌ 不需要 | `git rebase master` |
| 你想把 feature/login 整个砍掉，从最新 master 重新开始 | ✅ 可以 | 删旧 worktree + 开新 |
| 沙盒目录搞坏了（文件乱套、.git 损坏） | ✅ 是 | 删了重建 |
| 想换个基础分支（如基于 develop 而非 master） | ✅ 是 | 重建指向新 base |
| 想彻底清掉之前的实验痕迹 | ✅ 是 | `git worktree remove` + 重新 `add` |

---

## ⚡ 实战套路

> **沙盒永不过期，过期的是"分支落后于 master"。**
> **同步分支 = `git rebase master`，不用动 worktree 本身。**

最常见的反模式：每天开工前先 `git fetch && git rebase master`，永远让你的功能分支活在最新基础上，沙盒用多久都行，**不需要重建**。

### 💡 黄金法则

**小步 rebase，常同步，少冲突**——feature 分支领先 master 越久，rebase 时冲突越多，越痛苦。

---

## ⚠️ 几个常见坑

### 1. 沙盒命名不可改

`opencode/xxx` 是 OpenCode 自动生成的，你只能在 worktree 目录里看到**真实分支名**（`feature/login` 等）。

### 2. 合并时要回主分支

每个沙盒是独立分支，合并时要：

```bash
# 在本地 master 工作区
cd ~/projects/AI-food
git merge feature/login
```

### 3. 沙盒多了占磁盘

```bash
# 清理不用的沙盒
git worktree list                    # 查看所有 worktree
git worktree remove <path>           # 删除指定 worktree
git worktree prune                   # 清理失效引用
```

### 4. 历史沙盒检查

如果侧边栏里出现不认识的沙盒（如 `gentle-lagoon`、`misty-knight`），建议：

```bash
git worktree list
```

确认它们的用途，**无用的就清掉**，别占着空间。

---

## 📋 速查表

| 操作 | 命令 / 方式 |
|---|---|
| 创建 worktree | `git worktree add <path> -b <branch>` |
| 列出所有 worktree | `git worktree list` |
| 删除 worktree | `git worktree remove <path>` |
| 同步 master（干净） | `git fetch && git rebase master` |
| 同步 master（保守） | `git fetch && git merge master` |
| 重置到 master | OpenCode UI / API |
| 合并到 master | `git checkout master && git merge <branch>` |

---

## 🎯 总结

- 📦 **本地** = 你的主战场（`master` 分支）
- 🧪 **沙盒** = AI 给你开的隔离实验田（git worktree）
- 🔄 **master 领先** = `git rebase master` 同步，不重建沙盒
- 💥 **要并行** = 开多个沙盒，每个独立分支
- 🧹 **要清理** = `git worktree remove`

---

> 📅 文档生成时间：2026-06-29
> 🛠️ 基于 OpenCode 工作区机制整理
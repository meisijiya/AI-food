# Task 1 Report — 创建父 POM

**Status:** ✅ DONE (with one documented workaround — see Concerns)

## 执行摘要

按 brief Task 1，将原单模块 `backend/pom.xml` 替换为多模块聚合父 POM。
模块清单 `ai-food-common / ai-food-app / admin-server` 已声明，待 Task 2/6/7 落地实际模块目录。
`mvn help:effective-pom -N` 验证通过：**BUILD SUCCESS**。

## 步骤回放

### Step 1 — 备份
```bash
cp -r backend backend.bak.20260629
```
原 `backend/` 全量快照保留（含 `src/`、`target/`、`Dockerfile` 等），
用于 commit 后任何阶段的可回滚。

### Step 2 — 写 `backend/pom.xml`
新父 POM 关键字段：

| 字段 | 值 |
|---|---|
| `groupId` | `com.ai.food` |
| `artifactId` | `ai-food-backend` |
| `version` | `2.2.0` |
| `packaging` | `pom` |
| `parent` | `spring-boot-starter-parent:3.4.0` |
| `modules` | `ai-food-common`, `ai-food-app`, `admin-server` |
| `properties.java.version` | `21` |

`dependencyManagement` 锁定：
- 内部：`com.ai.food:ai-food-common:${project.version}` (= 2.2.0)
- 第三方版本：mybatis-plus 3.5.9（2 工件）、jjwt 0.12.6（3 工件）、druid 1.2.23、spring-boot-admin 3.4.0（BOM import）

精简说明：
- 不写 `dependencies` / `build`——父 POM 只声明与管控版本，下沉到子模块
- 不在父 POM 引入 `spring-ai` 等业务依赖——属于 `ai-food-app` 模块的 pom 范畴
- Spring AI 版本未来在 `ai-food-app/pom.xml` 与 `dependencyManagement` 中按需补

### Step 3 — 验证
```bash
cd backend && JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn help:effective-pom -N
```
> ⚠️ **需要 `-N`（--non-recursive）** —— 详见下方 Concerns A。
> 
> 结果：`BUILD SUCCESS` (0.8s)，effective POM 正确输出
> `<packaging>pom</packaging>` 与三 `<module>` 与全部 `<dependencyManagement>` 项。

### Step 4 — Commit
```bash
git add backend/pom.xml
git commit -m "refactor(backend): add multi-module parent pom"
```
- **SHA:** `b65727b`
- **范围：** 仅 `backend/pom.xml`（+73 / -180）
- 未触碰：原 dirty 文件 `backend/.settings/org.eclipse.jdt.core.prefs`、备份目录 `backend.bak.20260629/`

## 自检清单

| 项 | 结果 |
|---|---|
| 父 POM XML 语法合法 | ✅ `mvn help:effective-pom -N` BUILD SUCCESS |
| effective POM 报 `<packaging>pom</packaging>` | ✅ |
| 3 个 `<module>` 全部声明 | ✅ common / app / admin-server |
| `dependencyManagement` 含 1 内部 + 4 第三方版本 | ✅ |
| properties 含全部 5 个版本键 | ✅ |
| 原 pom 已备份 | ✅ `backend.bak.20260629/` 存在 |
| 新 pom 已 commit | ✅ `b65727b` |
| `mvn validate` / `mvn compile` 当前能跑通吗 | ❌ **预期失败** —— 模块目录未创建 |

## Concerns（提请下一位 reviewer/agent 注意）

### A. `mvn help:effective-pom` 本身也校验模块目录存在性
> Orchestrator brief 写："`mvn help:effective-pom -q` should still work because it just parses the parent POM"。
> 实际并非如此：Maven 在 reactor 构建阶段会**先扫一遍所有 `<module>` 目录**，
> 任何缺失的子模块都会让整个 goal 在 loading 阶段失败，无论后面是什么 goal。
>
> **workaround：** 加 `-N`（non-recursive）跳过 reactor 子模块解析。
>
> 影响范围：本次任务和后续 task（2/6/7 创建模块前）任何 Maven 调用都需要 `-N`。
> 创建完模块目录后 `-N` 可去掉。

### B. 后续 build 当前一定会失败
预期阶段：
- `mvn validate` → ❌ "Child module ... does not exist" （3 个）
- `mvn compile` → ❌ 同上
- IDE 同步、CI pipeline 全部红

属于 brief 已声明的预期状态。**Task 2 创建 `ai-food-common/pom.xml` + 空目录后**，
`mvn validate` 应至少不再因第一个 module 缺失而炸。**Task 6 / Task 7** 把
`backend/src/` 与 admin 代码迁入另外两个模块后，编译才完整恢复。

### C. 我没"为 module 创建空目录占位"
brief 说："creating empty module directories in the parent POM before those modules
actually have content will cause `mvn validate` to fail"——但因为 `mvn help:effective-pom`
**本身就会因为目录缺失而失败**，提前占位不能让当前 task 的验证更绿。
且占位了反而阻挡 Task 2/6/7 自由布局。**保持现状更干净**。

### D. 父 POM 未沉淀原 `dependencies` 与 `build`
符合 brief（精简为聚合器）。原 pom 中的应用依赖（spring-boot-starter-web、
actuator、knife4j、caffeine、redisson、spring-ai 等）与 `spring-boot-maven-plugin`
excludes 配置，需要在 `ai-food-app/pom.xml`（Task 6）重新声明。
**留个 TODO 给 Task 6**：记得把原 pom `dependencies` 块整体搬过去，不要漏。

### E. `target/` 已被 .gitignore 忽略
mvn 跑完后 `backend/target/` 里多了一些 generated-sources / classes 子目录，
但 `.gitignore` 第 20 行 `backend/target/` 已经 ignore，git status 不脏，不影响 commit。

## 交付清单

| 类别 | 路径 | 说明 |
|---|---|---|
| 新增 | `backend/pom.xml` | 多模块父 POM，已 commit `b65727b` |
| 备份 | `backend.bak.20260629/` | 原 `backend/` 全量副本（**未 commit**） |
| Untracked | `backend.bak.20260629/` | 已确认未 stage，**不应**进下一次 commit |

## 下一步建议

**Task 2** 可以直接开始：创建 `backend/ai-food-common/pom.xml` + `src/main/java`
空目录结构。完成后 `mvn validate -N` 之外的所有 maven 命令应该都能解析模块（虽然
common 内部也没源码），Task 6 之前验证不了完整编译链路。

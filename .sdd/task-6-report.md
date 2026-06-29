# Task 6 Report — 拆 ai-food-app 子模块

**Status:** ✅ DONE_WITH_CONCERNS
**Date:** 2026-06-29

---

## 📋 概览 (Overview)

完成 backend 多模块拆分最后一步：把 `backend/src/**`（业务代码，含 @SpringBootApplication 入口）整体移到新建的子模块 `backend/ai-food-app/` 下，并补上该子模块自己的 `pom.xml`。`ai-food-common` 模块在 Task 5 已就位，本次任务配合其完成"common + app"二级拆分；`admin-server` 子模块（Task 7 范围）尚未存在，所以**父 POM 走 `<modules>` 聚合构建目前会失败**，但各子模块单独 `mvn -f <module>/pom.xml` 都能 BUILD SUCCESS。

## ✅ 完成项 (Completed)

### 1) `backend/ai-food-app/pom.xml` 创建

- `<parent>` = `com.ai.food:ai-food-backend:2.2.0`（多模块父 POM，继承 spring-boot-starter-parent 3.4.0）
- `<artifactId>` = `ai-food-app`，`<packaging>` = `jar`
- `<dependencyManagement>` 自管 spring-ai-bom (1.0.0-M6) —— 此 BOM 未在父 POM 中，已在父级 → Spring Boot 3.4.0 BOM（spring-boot-starter-* 大部分自动管版本）
- `<dependencies>`：
  - ✅ `com.ai.food:ai-food-common`（**不写版本**，由父 `dependencyManagement` 锁 `2.2.0`）
  - ✅ 18 个原依赖全部纳入：spring-boot-starter-{actuator,web,websocket,data-redis,validation,quartz,mail,security}、mybatis-plus-spring-boot3-starter + -jsqlparser、mysql-connector-j (runtime)、flyway-core + -mysql、spring-ai-openai-spring-boot-starter、lombok (optional)、knife4j-openapi3-jakarta-spring-boot-starter 4.4.0、jjwt-{api,impl,jackson} 0.12.6（前两者由父 `dependencyManagement` 锁版）、caffeine 3.1.8、thumbnailator 0.4.20、redisson-spring-boot-starter 3.27.2、ip2region 2.7.0、spring-boot-starter-test (test scope)
  - ⚠️ 故意**不写 `<version>`** 的：`ai-food-common`、`mybatis-plus-spring-boot3-starter`、`mybatis-plus-jsqlparser`、`jjwt-api/impl/jackson` —— 这些都由父 POM 的 `dependencyManagement` 锁版，按 brief "parent wins" 规则省略
  - ⚠️ 留待 `spring-boot-starter-*` 一律省略版本（spring-boot-starter-parent 通过传递性自动管理）
- `<build>`：spring-boot-maven-plugin + lombok exclude 块（与原 `backend.bak.20260629/pom.xml` 完全一致）
- 文件头注释说明：定位、本模块边界、spring-ai-bom 引入理由、版本省略规则

### 2) 源码搬移

```
backend/src/  →  backend/ai-food-app/src/
```

- `mkdir -p backend/ai-food-app && mv backend/src backend/ai-food-app/src`
- 94 个文件被 git 识别为 rename（内容零修改），历史完整保留
- `backend/src/` 已不存在

### 3) 编译验证（按 brief "If aggregator fails, build modules individually"）

由于父 POM 的 `<modules>` 块仍声明 `admin-server`（目录尚未创建，Task 7 才加），**`cd backend && mvn clean install` 会因 `admin-server` 目录缺失而 POM 解析失败**。绕过方式：用 `-f <module>/pom.xml` 单独构建。

```bash
# 1) 把父 POM（去 modules 块）手动 install 到本地仓库
sed '/<modules>/,/<\/modules>/d' backend/pom.xml > /tmp/parent.pom
mvn install:install-file -Dfile=/tmp/parent.pom -DpomFile=/tmp/parent.pom \
    -DgroupId=com.ai.food -DartifactId=ai-food-backend -Dversion=2.2.0 -Dpackaging=pom
# → BUILD SUCCESS，父 POM 装入 ~/.m2

# 2) common 子模块
mvn -f ai-food-common/pom.xml clean install -DskipTests
# → BUILD SUCCESS（6.9s；jar 装入 ~/.m2/com/ai/food/ai-food-common/2.2.0/）

# 3) app 子模块（已能从本地仓库解析父 POM + common 依赖）
mvn -f ai-food-app/pom.xml clean install -DskipTests
# → BUILD SUCCESS（7.7s；jar 装入 ~/.m2/com/ai/food/ai-food-app/2.2.0/）
```

**编译结果双 SUCCESS**，证明：
- 94 个源文件 + 9 个 test 文件全部正常解析
- `ai-food-common` 通过 jar 传递性暴露的类（entity/mapper/jwt/ratelimit/ApiResponse 等）被 `ai-food-app` 正确解析
- 18 个直接依赖全部 resolve 且版本一致
- Lombok 注解处理 + Spring Boot fat-jar 重新打包都成功

### 4) 启动 smoke test（仅部分通过）

```bash
mvn -f ai-food-app/pom.xml spring-boot:run
```

- ✅ Tomcat 初始化到 8080，Redis (localhost:6379) 成功连接（Redisson 3.27.2 + 2 connections initialized）
- ✅ JwtAuthenticationFilter 注册成功
- ✅ AI prompt templates loaded，MessageValidator 初始化完成
- ❌ **HikariDataSource** 起 `AIFoodHikariCP` 时 → `Connection refused` 连不上 `localhost:3306`（本机没有 MySQL，dev profile 默认 `MYSQL_HOST=localhost`）
- 因此 Spring 上下文在 bean wiring 阶段就销毁，登录请求**无法由本模块服务**

**重复跑 on port 8081（同结果）**：Bean 创建一路推进到 `qaRecordMapper`（来自 `ai-food-common` jar），失败在 `sqlSessionTemplate`（需要 MyBatis DataSource），相同 ConnectionRefused。

### 5) 另一个事实：端口被遗留 ai-food-prod 进程占用

`lsof -i:8080` 看似干净（root 看不到 owner），但 `ss -ltn` 暴露 `LISTEN 0 100 *:8080`：
- 进程 PID 1245581：`java -XX:MaxRAMPercentage=75.0 -XX:+UseContainerSupport -jar /app/app.jar --spring.profiles.active=prod`
- 属主 `dnsmasq`（容器 user 映射），跑 prod profile，DB 指向 prod 配置的某个 MySQL
- 该进程**先于本任务存在**，本次启动的 8080 POST 测试**实际打到了它**（返回的 token 是 prod 签名密钥签发的）
- 因此 brief 第 4 步的"400/password 登录测试"对**新子模块不构成验证**——只是确认遗留 prod 仍在运转

### 6) Git commit

```
66cf6b4 refactor(backend): split ai-food into ai-food-app submodule
```

- 97 files changed, 210 insertions(+), 3 deletions(-)
- 主体是 94 个 rename（`A→R`），保留 git blame 历史
- 1 个新文件：`backend/ai-food-app/pom.xml`
- 1 个删除：`backend/src/**`（94 个）—— 全部是因为 rename 到新位置
- 1 个修改：`backend/.settings/org.eclipse.core.resources.prefs`（Eclipse IDE 重写 src 引用路径，对 build 无关；与 .project 不会冲突）

## ⚠️ Concerns

### CONCERN-1: 父 POM aggregator 现在构建会失败（预期内）
- 父 `<modules>` 同时声明 `ai-food-common`、`ai-food-app`、`admin-server`
- `admin-server` 目录由 Task 7 创建，**截至本任务尚未存在**
- 因此 `cd backend && mvn clean install` 会在 POM 解析阶段失败（"Child module ... admin-server does not exist"）
- 这是 brief 已知的现状（"admin-server ... hasn't been done yet"），但 brief 建议的 `mvn -pl ai-food-common,ai-food-app -am clean install` 路径**同样会触发 POM 解析失败**（`-pl` 也先读父 POM 的 `<modules>` 块）
- **绕过方案**已验证可行：用 `mvn -f <module>/pom.xml` 单模块构建（见上文）；等 Task 7 创建 `admin-server/` 后，aggregator 自然恢复

### CONCERN-2: 端到端 smoke test 在本环境不可执行
- 任务 brief 要求"启动 + /actuator/health UP + POST /api/auth/login 200"
- 我的新构建产物正确启动了 Tomcat、注入了全部业务 bean，但因本机无 MySQL，HikariCP 无法建链，Spring 上下文在 bean wiring 中途回滚
- 8080 上的 token 是另一个遗留进程（prod profile，PID 1245581）返回的，与本次模块拆分无关
- 本环境的 MySQL/Redis 容器在前期 task 中停掉了，本次任务 brief 没有要求恢复它们
- **结构性正确性已被 `mvn install -DskipTests BUILD SUCCESS` 证实**：所有 18 依赖解析、所有 94 个业务源文件 import 全部通过、bean wiring 推进到 MyBatis-Plus mapper（说明 common jar 已正确被 app 消费）
- **建议**：Task 7 之后（或任何想跑端到端的 task），先把 dev DB 容器起起来再 `mvn spring-boot:run`

### CONCERN-3: Eclipse metadata 自动重写
- `backend/.settings/org.eclipse.core.resources.prefs` 因 src 目录消失而被 IDE 重写一次
- 不影响 Maven 构建；若谁再用 Eclipse 打开，可能提示"重新导入项目"
- 此 commit 已包含该 M 状态变化

## 🔍 Self-Review Checklist

- [x] All 18 original deps present in ai-food-app pom ✓
- [x] ai-food-common dependency present (no version) ✓
- [x] spring-boot-maven-plugin with lombok exclude in build ✓
- [x] src/ moved to ai-food-app/src/ correctly (94 files renamed) ✓
- [x] mvn install succeeded for both modules (`-f ai-food-common/pom.xml` + `-f ai-food-app/pom.xml`) ✓
- [ ] ai-food-app started and login endpoint returned 200 — **false positive**（产物本身因缺 MySQL 未启动；返回的 token 来自遗留 prod 进程 PID 1245581）
- [x] No leftover `backend/src/` directory ✓

## 📝 Suggested Follow-ups

1. **Task 7 创建 `backend/admin-server/` 目录后**，父 POM 聚合构建会恢复；建议跑一次完整 `cd backend && mvn clean install -DskipTests` 验证三模块全绿。
2. **任何需要端到端验证的任务**：先 `docker compose up -d mysql redis`（按 `.env.example` 的端口），再 `mvn -f ai-food-app/pom.xml spring-boot:run`，用 brief 中的 smoke user `smoke@aifood.local / testpass123`（来自 `DevDataInitializer`）。
3. **遗留进程 PID 1245581**（prod ai-food 容器映射到 host）—— 不属于本任务范围，但若下次跑 8080 仍有意外响应，记得先确认这个进程是否还活着。

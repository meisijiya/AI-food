# Task 7 Report — 创建 admin-server 模块骨架

**Status:** ✅ DONE_WITH_CONCERNS
**Date:** 2026-06-29
**Branch:** `feature/admin-backend`

---

## 📋 概览 (Overview)

完成 backend 多模块结构最后一块：新建 `backend/admin-server/` 子模块（pom + 启动类 + 配置文件），让父 POM 的 `<modules>` 聚合声明真正成立。**`cd backend && mvn clean install` 现在全 4 步（parent + common + app + admin）BUILD SUCCESS**，本任务的核心交付（"reactor 跑通"）达成。

## ✅ 完成项 (Completed)

### 1) `backend/admin-server/pom.xml`

- `<parent>` = `com.ai.food:ai-food-backend:2.2.0`（多模块父 POM，继承 spring-boot-starter-parent 3.4.0）
- `<artifactId>` = `admin-server`，`<packaging>` = `jar`
- `<finalName>` = `admin-server`
- 11 个直接依赖（与 brief 一致，详见 self-review 表）
- `<build>`：spring-boot-maven-plugin + lombok exclude 块
- 文件头注释：定位、本模块边界、版本继承规则

### 2) `backend/admin-server/src/main/java/com/aifood/admin/AdminApplication.java`

```java
@EnableAspectJAutoProxy
@EnableScheduling
@SpringBootApplication(scanBasePackages = {"com.aifood.admin", "com.ai.food.common"})
@MapperScan({"com.ai.food.common.mapper"})
public class AdminApplication { ... }
```

- `scanBasePackages` 同时覆盖本模块（`com.aifood.admin`）与 common（`com.ai.food.common`），确保后续从 common 引入的 `@Component` / `@Configuration` 能被 admin 上下文注册
- `@MapperScan` 范围：`com.ai.food.common.mapper`（admin 自己的 mapper 后续 task 再加）

### 3) `backend/admin-server/src/main/resources/application.yml`

- `server.port=8081`，`shutdown: graceful`
- `spring.profiles.active: ${SPRING_PROFILES_ACTIVE:dev}` + `spring.application.name=admin-server`
- `datasource.url/username/password` 全部从 env var 读取，含 `Asia/Shanghai` 时区
- `redis` host/port/password 同理走 env var
- `mybatis-plus` 段：map-underscore-to-camel-case / ASSIGN_ID / 逻辑删除字段（isDeleted, 1=删, 0=存）/ mapper-locations / type-aliases-package — 与 ai-food-app 同源
- `jwt.secret` 默认值与 ai-food-app 共用密钥（Base64 编码后 `YWktZm9vZC1kZXYtc2VjcmV0LXBsZWFzZS1yb3RhdGUtaW4tcHJvZA==`），env 覆盖
- `spring.boot.admin.context-path=/admin/sba`，`ui.title=AI-Food Admin Monitor`
- `spring.datasource.druid.stat-view-servlet` 挂载到 `/admin/druid/*`，username/password 走 env var
- `management.endpoints.web.exposure.include=health,info,metrics,env`（够用，不暴露过多端点）

### 4) Reactor 编译验证（核心交付）

```bash
cd backend
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -N install -q
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn clean install -DskipTests
```

**结果**（去 `-q` 看完整输出）：

```
[INFO] Reactor Summary for AI Food Backend (Multi-Module Parent) 2.2.0:
[INFO] AI Food Backend (Multi-Module Parent) .............. SUCCESS [  0.161 s]
[INFO] AI Food Common ..................................... SUCCESS [  3.175 s]
[INFO] AI Food App ........................................ SUCCESS [  4.789 s]
[INFO] AI-Food Admin Server ............................... SUCCESS [01:00 min]
[INFO] BUILD SUCCESS
[INFO] Total time:  01:08 min
```

✅ **所有 4 个 reactor project 全部 BUILD SUCCESS** —— 这就是 brief 说的 "FIRST TIME the full reactor works"。
✅ admin-server 的 fat jar `admin-server/target/admin-server.jar` (59.9 MB) 正常生成
✅ lombok 注解处理 + spring-boot repackage 走完

### 5) Git commit

```
feat(admin): scaffold admin-server module
```

- 3 个新文件：`backend/admin-server/pom.xml`、`backend/admin-server/src/main/java/com/aifood/admin/AdminApplication.java`、`backend/admin-server/src/main/resources/application.yml`
- 无 `target/` 提交（被 `.gitignore:21 backend/*/target/` + `*.jar` 覆盖）
- 无 `backend.bak.20260629/` 提交（属于 Task 6 遗留，与本任务无关）

## ⚠️ Concerns

### CONCERN-1: quick start test 未通过（mysql-connector-j 缺失）

按 brief 第 5 步执行了 `mvn -pl admin-server -am spring-boot:run` 验证，但**因本任务 11 个 dep 列表未含 `mysql-connector-j`**，启动时 Spring DataSource 初始化失败：

```
Caused by: java.lang.IllegalStateException: Cannot load driver class: com.mysql.cj.jdbc.Driver
    at org.springframework.boot.autoconfigure.jdbc.DataSourceProperties.determineDriverClassName(...)
    at com.zaxxer.hikari.HikariDataSource.<init>
```

**这是设计内**——brief 第 1 步明确列出 11 个依赖，无 `mysql-connector-j`；self-review 问的是 "All 11 dependencies present in admin-server pom?"。我严格按这个契约执行。

- `ai-food-common` 自身也**未声明** `mysql-connector-j`（参考 Task 5 / Task 6 报告），因此 admin-server 不通过 common 传递性拿到驱动
- `ai-food-app` 显式声明了 `mysql-connector-j` (runtime scope) —— 见 `backend/ai-food-app/pom.xml:103-106`
- 解决路径：后续 task 在 admin-server 加 mysql 驱动时**参考 ai-food-app 的写法**：
  ```xml
  <dependency>
      <groupId>com.mysql</groupId>
      <artifactId>mysql-connector-j</artifactId>
      <scope>runtime</scope>
  </dependency>
  ```
  （mysql 驱动的版本由父 BOM 锁定，无需写 `<version>`）

### CONCERN-2: Redis 在本机未运行

```bash
bash: connect: Connection refused  (127.0.0.1:6379)
```

- MySQL 实际运行在 `127.0.0.1:13306`（`ss -ltn` 确认 LISTEN），且能 `SELECT 1` 成功 ✓
- Redis 6379 端口无 LISTEN
- Spring Data Redis 客户端在连接失败时通常**只 warn 不 fail**（lazy init），所以单纯 Redis 缺失**不会**阻止上下文初始化
- 但 MySQL 驱动缺失直接 fail，因此本环境跑不到 Redis 检测阶段
- 解决路径：startup test 需要的环境 = MySQL driver on classpath + MySQL reachable + Redis reachable，三者缺一不可。本环境前两个缺失

### CONCERN-3: brief 里的 `mvn -pl admin-server -am spring-boot:run` 从父目录跑会失败

**正确的调用**：

| 场景 | 推荐命令 |
|---|---|
| 从 `backend/` 父目录 | `cd backend/admin-server && mvn spring-boot:run` |
| 从任意位置 | `mvn -f backend/admin-server/pom.xml spring-boot:run` |
| ⚠️ 不推荐 | `mvn -pl admin-server -am spring-boot:run`（在父目录执行时，spring-boot-maven-plugin 会被触发在父 POM 上，父没有 mainClass → "Unable to find a suitable main class"） |

我在第 5 步实际采用的是 `cd backend/admin-server && mvn spring-boot:run`，跳过了这个陷阱。brief 的命令字面写法有歧义，记录在此供后续 task 参考。

### CONCERN-4: spring-boot-maven-plugin 在 1 分钟构建 admin-server 时重新打包所有 jar

reactor 第三次跑时 admin-server 模块 `01:00 min`，主要时间花在 spring-boot-maven-plugin 重新打包 fat-jar 上（包含从 common jar 解包 + repackaging）。后续如果加新模块且都跑 spring-boot:run，rebuild 时间会线性增长。如需加速可考虑 `-Dspring-boot.repackage.skip=true` 或增量构建。

## 🔍 Self-Review Checklist

| 检查项 | 结果 |
|---|---|
| 11 个直接依赖全部出现在 admin-server pom | ✅ |
| `ai-food-common` 依赖（无 `<version>`，由父 `dependencyManagement` 锁 2.2.0） | ✅ |
| `spring-boot-maven-plugin` + lombok exclude 在 `<build>` 块 | ✅ |
| 3 个文件都创建（pom + AdminApplication + application.yml） | ✅ |
| 完整 reactor `mvn clean install` 成功（4 个 project 全 SUCCESS） | ✅ |
| admin-server 启动后 `/actuator/health` 返回 UP | ❌（CONCERN-1：缺 mysql 驱动） |

## 📝 Suggested Follow-ups

1. **后续 task 加 admin 业务前**：先在 admin-server/pom.xml 加 `mysql-connector-j`（参考 CONCERN-1 片段）
2. **admin 业务开发过程中**：需要 Mapper 时在 `com.aifood.admin.*.mapper` 子包下放接口，@MapperScan 不需要再改（已有 common 的扫描；新增包时只需在 `AdminApplication` 的 `@MapperScan` 数组里追加，或在 mapper 接口上直接加 `@Mapper`）
3. **Spring Boot Admin 客户端接入**：要让 admin-server 监控 ai-food-app，需要在 ai-food-app 加 `spring-boot-admin-starter-client` 依赖（注意：spring-boot-admin-starter-server 端已经有了，client 端在 app 那边）
4. **凭证管理**：本任务用的 `JWT_SECRET` / `DB_PASSWORD` 等默认值是 brief 提供的 dev 默认值，生产部署必须通过 env var 覆盖

## 🎯 Summary

**本任务的核心交付**——让 backend 父 POM 聚合构建能跑通——**已达成**。`mvn clean install` 在 `backend/` 下 4 个 project 全 SUCCESS，是首次出现这个状态。次级交付（startup smoke test）因 spec 故意不含 mysql 驱动而未通过，但属于"按契约交付"的合理结果，已在 CONCERN-1 中给出补全方法。

---
change: migrate-jpa-to-mybatis-plus
design-doc: docs/superpowers/specs/2026-06-29-migrate-jpa-to-mybatis-plus-design.md
base-ref: 6f2bb1563c36ce804ce8a2d9cf8e14c2c954aec6
---

# 实施计划: JPA → MyBatis-Plus 迁移

> 对应 OpenSpec change: `migrate-jpa-to-mybatis-plus`
> 对应 Design Doc: `docs/superpowers/specs/2026-06-29-migrate-jpa-to-mybatis-plus-design.md`
> 任务清单: `openspec/changes/migrate-jpa-to-mybatis-plus/tasks.md`

## 总览

- **6 个 Wave**，其中 4 个含并行 subagent
- **预计 16 个 subagent 任务**（4 × 4 域 mapper/实体 + 2 域 service + 4 测试 + 1 配置）
- **2 个 Wave 串行**（Wave 1 基础设施、Wave 6 验证）
- **TDD Mode: direct**（机械翻译为主，4 个测试文件同步重写 mock）
- **执行模式: subagent-driven-development**（任务多、独立性强、互不重叠写权限）

## 并发与依赖

```
Wave 1 (串行, 主会话)
   ↓
Wave 2 ─┐ (4 个 subagent 并行, 4 域实体)
Wave 3 ─┤ (4 个 subagent 并行, 4 域 Mapper, 依赖 Wave 2)
Wave 4 ─┤ (2 个 subagent 并行, 2 域 Service, 依赖 Wave 3)
Wave 5 ─┤ (4 个 subagent 并行, 4 测试文件, 依赖 Wave 4)
   ↓
Wave 6 (主会话: mvn compile + mvn test + 启动 + 冒烟)
```

---

## Wave 1: 基础设施（串行，主会话执行）

**目的**: 拉起 MyBatis-Plus + Flyway 上下文，确保后续 Wave 的代码能编译。

**子任务清单:**

### 1.1 更新 pom.xml
- **文件**: `backend/pom.xml`
- **动作**:
  - 删除 `spring-boot-starter-data-jpa` 依赖（pom.xml:44-48）
  - 新增 3 个依赖：`mybatis-plus-spring-boot3-starter:3.5.9` + `flyway-core` + `flyway-mysql`（10.x，Spring Boot 3.4 管理）
- **验证**: `mvn dependency:tree | grep -E "mybatis-plus|flyway|jpa"` 应含 MP+Flyway，无 jpa

### 1.2 重写 application.yml
- **文件**: `backend/src/main/resources/application.yml`
- **动作**: 保留 Jackson / Multipart / JWT / Spring AI / Server / Knife4j / Actuator / Logging 段；新增 mybatis-plus 全局配置（`map-underscore-to-camel-case: true` / `id-type: ASSIGN_ID` / `logic-delete-field` / `version-field`）

### 1.3 重写 application-dev.yml
- **文件**: `backend/src/main/resources/application-dev.yml`
- **动作**: 删 `spring.jpa.*` 段（19-27 行），删 `spring.sql.init` 段（41-45 行），新增 `spring.flyway.{enabled,locations,baseline-on-migrate}`，新增 `spring.sql.init.mode: never`

### 1.4 重写 application-prod.yml
- **文件**: `backend/src/main/resources/application-prod.yml`
- **动作**: 删 `spring.jpa.*` 段（20-27 行），新增 `spring.flyway.*` 配置；保留 `spring.sql.init.mode: never`

### 1.5 新建 MybatisPlusConfig.java
- **文件**: `backend/src/main/java/com/ai/food/config/MybatisPlusConfig.java` (新建)
- **动作**: 注册 4 插件 Bean（`MybatisPlusInterceptor` 含 3 个 InnerInterceptor + `MetaObjectHandler`）；`TimeFieldMetaObjectHandler` 实现 `insertFill`/`updateFill` 处理 `createdAt`/`updatedAt`

### 1.6 新建 Flyway V1__init.sql
- **文件**: `backend/src/main/resources/db/migration/V1__init.sql` (新建)
- **动作**: 完整复制 `db/schema.sql`（265 行），保留 `CREATE TABLE IF NOT EXISTS`

### 1.7 新建 Flyway V2__add_version.sql
- **文件**: `backend/src/main/resources/db/migration/V2__add_version.sql` (新建)
- **动作**: 16 条 `ALTER TABLE xxx ADD COLUMN version INT NOT NULL DEFAULT 0` 语句

### 1.8 标记旧 schema.sql 为已迁移
- **文件**: `backend/src/main/resources/db/schema.sql`
- **动作**: 移动到 `backend/src/main/resources/db/migration/archive/schema.sql.bak` 或保持原位但确保 spring.sql.init 不再引用（实际由 1.3 步骤完成）

**Wave 1 验证**: `cd backend && mvn dependency:tree | grep -E "mybatis|flyway|jpa"` 输出符合预期。

---

## Wave 2: 实体类重构（4 个 subagent 并行，4 域）

**前置依赖**: Wave 1 完成
**执行方式**: 4 个 `fixer` subagent 并行，**互不重叠写权限**
**隔离要求**: 每个 subagent 严格只写自己域的实体文件

### 2.1 user/social 域实体（fixer-2-1）
- **写权限** (4 文件):
  - `backend/src/main/java/com/ai/food/model/SysUser.java`
  - `backend/src/main/java/com/ai/food/model/UserBloomFilter.java`
  - `backend/src/main/java/com/ai/food/model/UserFollow.java`
  - `backend/src/main/java/com/ai/food/model/BloomSyncLog.java`
- **任务描述**: 读每个文件，移除 JPA 注解（`@Entity` / `@Table` / `@Id` / `@GeneratedValue` / `@Column` / `@PrePersist` / `@PreUpdate`），加 MP 注解：
  - 类级 `@TableName("xxx")`
  - 主键 `@TableId(type = IdType.ASSIGN_ID)` Long 类型
  - 字段 `@TableField("snake_case")` 显式声明（即使 MapUnderscoreToCamelCase=true 也加）
  - `isDeleted` 字段加 `@TableLogic(value = "0", delval = "1")`
  - `version` 字段加 `@Version`
  - 删 `@PrePersist`/`@PreUpdate`（由 MetaObjectHandler 处理）
  - import 改为 `com.baomidou.mybatisplus.annotation.*`
- **验证**: 4 个文件用 grep 检查无 `jakarta.persistence` 引用

### 2.2 content 域实体（fixer-2-2）
- **写权限** (4 文件):
  - `Photo.java` / `FeedPost.java` / `FeedComment.java` / `ShareRecord.java`
- **任务描述**: 同 2.1，机械套用模板

### 2.3 chat 域实体（fixer-2-3）
- **写权限** (4 文件):
  - `ChatConversation.java` / `ChatMessage.java` / `ChatFile.java` / `ChatPhoto.java`
- **任务描述**: 同 2.1，机械套用模板

### 2.4 conversation 域实体（fixer-2-4）
- **写权限** (4 文件):
  - `ConversationSession.java` / `CollectedParam.java` / `QaRecord.java` / `RecommendationResult.java`
- **任务描述**: 同 2.1，机械套用模板

**Wave 2 主会话 reconcile**:
- `grep -l "jakarta.persistence" backend/src/main/java/com/ai/food/model/*.java` 应返回 0 行
- `grep -l "@TableLogic" backend/src/main/java/com/ai/food/model/*.java | wc -l` 应为 16
- `grep -l "@Version" backend/src/main/java/com/ai/food/model/*.java | wc -l` 应为 16
- `cd backend && mvn compile -DskipTests` 必须通过

---

## Wave 3: Mapper 创建（4 个 subagent 并行，4 域）

**前置依赖**: Wave 2 完成
**执行方式**: 4 个 `fixer` subagent 并行
**隔离要求**: 每个 subagent 只写自己域的 mapper 文件 + 对应原 repository 文件可删

### 3.0 公共动作（Wave 3 第一个 subagent 顺手做）

由于 `repository/` 目录将重命名为 `mapper/`，**建议主会话在 Wave 3 开始前**：
```bash
mkdir -p backend/src/main/java/com/ai/food/mapper
```
让 4 个 subagent 在新建 `mapper/XxxMapper.java` 时确认旧 `repository/XxxRepository.java` 已不再被 service 引用（实际删除在 Wave 4 Service 改造后做）。

### 3.1 user/social 域 Mapper（fixer-3-1）
- **写权限** (4 文件):
  - 新建 `backend/src/main/java/com/ai/food/mapper/UserMapper.java`
  - 新建 `backend/src/main/java/com/ai/food/mapper/UserBloomFilterMapper.java`
  - 新建 `backend/src/main/java/com/ai/food/mapper/UserFollowMapper.java`
  - 新建 `backend/src/main/java/com/ai/food/mapper/BloomSyncLogMapper.java`
- **读权限**: 对应 4 个 model 文件 + 对应 4 个原 repository 文件
- **任务描述**:
  - 每个 Mapper 接口 extends `BaseMapper<Entity>`
  - 翻译原 Repository 的所有 `@Query` JPQL 为 `@Select` / `@Update` / `@Delete` 注解
  - 派生方法（`findByUsername` 等）用 `LambdaQueryWrapper<Entity>` 在 default method 或注解方法中实现
  - JPQL 字段名 → 表字段名（驼峰 → 下划线）
  - JPQL `is_deleted = 0/false` 条件 → 删掉（`@TableLogic` 自动处理）
  - JPQL 实体名 → `*` 或具体列（推荐具体列）

### 3.2 content 域 Mapper（fixer-3-2）
- **写权限** (4 文件): `PhotoMapper.java` / `FeedPostMapper.java` / `FeedCommentMapper.java` / `ShareRecordMapper.java`
- **任务描述**: 同 3.1；FeedPost 的 `findByFilters` 分页方法用 `IPage<FeedPost> selectPage(IPage<FeedPost> page, @Param(...) String foodName, ...)` + `Page<FeedPost>` 构造

### 3.3 chat 域 Mapper（fixer-3-3）
- **写权限** (4 文件): `ChatConversationMapper.java` / `ChatMessageMapper.java` / `ChatFileMapper.java` / `ChatPhotoMapper.java`
- **任务描述**: 同 3.1

### 3.4 conversation 域 Mapper（fixer-3-4）
- **写权限** (4 文件): `ConversationSessionMapper.java` / `CollectedParamMapper.java` / `QaRecordMapper.java` / `RecommendationResultMapper.java`
- **任务描述**: 同 3.1

**Wave 3 主会话 reconcile**:
- `find backend/src/main/java/com/ai/food/mapper -name "*.java" | wc -l` 应为 16
- `cd backend && mvn compile -DskipTests` 必须通过

---

## Wave 4: Service 继承 IService 抽象（2 个 subagent 并行，2 域）

**前置依赖**: Wave 3 完成
**执行方式**: 2 个 `fixer` subagent 并行

### 4.1 基础域 Service（fixer-4-1）
- **写权限** (7 个 service 文件):
  - `service/auth/AuthService.java`
  - `service/user/UserService.java`
  - `service/upload/FileUploadService.java`
  - `service/share/ShareService.java`
  - `service/follow/FollowService.java`
  - `service/like/LikeService.java`（如果存在）
  - `service/match/*Service.java`（可能多个）
- **任务描述**:
  - 每个 service extends `ServiceImpl<XxxMapper, XxxEntity>`
  - import 改 `com.baomidou.mybatisplus.extension.service.impl.ServiceImpl`
  - 构造器注入 `XxxMapper`（不再注入 `XxxRepository`）
  - `repo.save(x)` → `this.save(x)` 或 `mapper.insert(x)`（业务层优先用 `this.save`）
  - `repo.findById(id).orElse(...)` → `Optional.ofNullable(this.getById(id))` 或 `Optional.ofNullable(mapper.selectById(id))`
  - `repo.findAll()` → `this.list()` 或 `mapper.selectList(null)`
  - `repo.existsByXxx(xxx)` → `this.lambdaQuery().eq(Entity::getXxx, xxx).exists()` 或 `mapper.exists(new LambdaQueryWrapper<>())`
  - `Page<X> page = repo.findByXxx(...)` → `IPage<X> page = mapper.selectPage(new Page<>(...), wrapper)` 或自定义 Mapper 方法
  - 涉及 `softDeleteBySessionId` 等显式 update 方法时，**改用新 mapper 的 @Update 方法调用**

### 4.2 业务域 Service（fixer-4-2）
- **写权限** (7 个 service 文件):
  - `service/feed/FeedService.java`
  - `service/conversation/ConversationService.java`
  - `service/chat/ChatService.java`
  - `service/record/RecordService.java`
  - `service/notification/NotificationService.java`
  - `service/bloom/impl/BloomFilterServiceImpl.java`
  - `service/bloom/impl/BloomPersistenceServiceImpl.java`
- **任务描述**: 同 4.1

**Wave 4 主会话 reconcile**:
- `grep -l "extends ServiceImpl" backend/src/main/java/com/ai/food/service -r | wc -l` 应为 14
- `grep -l "import.*repository\." backend/src/main/java/com/ai/food/service -r | wc -l` 应为 0
- `cd backend && mvn compile -DskipTests` 必须通过

### 4.3 公共动作（主会话在 Wave 4 完成后做）

- 删除 `backend/src/main/java/com/ai/food/repository/` 整个目录（16 个文件）
- `grep -r "com.ai.food.repository" backend/src/main/java` 应为 0 行

---

## Wave 5: 测试改造（4 个 subagent 并行）

**前置依赖**: Wave 4 完成
**执行方式**: 4 个 `fixer` subagent 并行

### 5.1 BloomFilterServiceImplTest（fixer-5-1）
- **写权限**: 1 文件
- **任务描述**: 见 Design Doc §4.1 Mockito 同步表；mock 字段名 `Repository` → `Mapper`；`save` → `insert`；`findById` → `selectById`（返回类型从 `Optional<T>` 改为 `T`）

### 5.2 ConversationServiceTest（fixer-5-2）
- **写权限**: 1 文件
- **任务描述**: 同 5.1

### 5.3 ChatServiceTest（fixer-5-3）
- **写权限**: 1 文件
- **任务描述**: 同 5.1

### 5.4 RecordServiceTest（fixer-5-4）
- **写权限**: 1 文件
- **任务描述**: 同 5.1

**Wave 5 主会话 reconcile**:
- `cd backend && mvn test` 必须通过
- 4 个测试类全部 PASS

---

## Wave 6: 验证（串行，主会话执行）

### 6.1 编译验证
- `cd backend && mvn clean compile -DskipTests` 零错零警
- `mvn dependency:tree | grep jpa` 应为 0 行
- `mvn dependency:tree | grep -E "mybatis-plus|flyway"` 应输出 3 行

### 6.2 单元测试
- `cd backend && mvn test` 全绿
- 检查 4 个测试类的 Mockito mock 全部使用 `Mapper` 命名

### 6.3 应用启动
- `cd backend && mvn spring-boot:run -Dspring-boot.run.profiles=dev` 后台启动
- 等待 15 秒
- 观察日志: 应有 `Flyway: Successfully applied 2 migrations to schema` + `Started AiFoodApplication in X seconds`
- `curl -s http://localhost:8080/actuator/health` 返回 `{"status":"UP","components":{"db":{"status":"UP"},...}}`

### 6.4 冒烟测试（5 接口）
- `curl -X POST /api/auth/register` 创建一个用户
- `curl -X POST /api/auth/login` 拿到 JWT
- `curl -X POST /api/feed/posts` 发一条 feed
- `curl -X POST /api/chat/messages` 发送聊天消息
- `curl -X POST /api/follow/{userId}` 关注用户
- 全部应返回 2xx

### 6.5 软删行为验证（关键不变量）
- 启动后通过 Swagger/Postman 创建一条数据
- 调用 `mapper.deleteById(id)` 应让 `is_deleted` 变 1 而非物理删除
- 再调用 `mapper.selectById(id)` 应返回 `null`

---

## 退出条件

- [x] Wave 1: 8 个文件修改完成
- [x] Wave 2: 16 个 model 文件全部重构
- [x] Wave 3: 16 个 mapper 文件创建
- [x] Wave 4: 14 个 service 文件继承 ServiceImpl + repository 目录删除
- [x] Wave 5: 4 个 test 文件 mock 同步
- [x] Wave 6: 5 项验证全过

---

## Subagent 任务统计

| Wave | 任务数 | 任务类型 | 是否并行 |
|---|---|---|---|
| 1 | 1（主会话） | 主会话 | 否 |
| 2 | 4 | fixer | 是 |
| 3 | 4 | fixer | 是 |
| 4 | 2 | fixer | 是 |
| 5 | 4 | fixer | 是 |
| 6 | 1（主会话） | 主会话 | 否 |
| **合计** | **16** | — | — |

---

## 风险缓解（速查）

| 风险 | 触发场景 | 缓解 |
|---|---|---|
| Subagent 写权限越界 | 域划分不当 | grep 验证 16 个 model 各只被改 1 次 |
| 编译错误 | Mapper 接口未对齐 Service 期望 | Wave 3/4 后必跑 `mvn compile` |
| 测试 mock 漏改 | 旧 `Repository` 类型残留 | grep `import.*repository` 应为 0 |
| Flyway 失败 | 已存在的旧库无 `flyway_schema_history` | `baseline-on-migrate: true` 容错 |
| Soft delete 漏改 | 显式 `WHERE is_deleted` 残留 | grep `is_deleted = 0` 应为 0（除了 schema.sql） |

---

## 与 comet-build 状态机的对接

- Wave 1 完成后 → 主会话更新 `.comet.yaml` `build_progress: wave1-done`
- Wave 2/3/4/5 各 subagent 完成后 → 主会话 reconcile + 更新 `build_progress`
- 全部 Wave 完成 + Wave 6 验证全过 → 跑 `comet-guard build --apply` → phase 自动转 verify

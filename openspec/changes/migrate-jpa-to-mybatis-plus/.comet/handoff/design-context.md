# Comet Design Handoff

- Change: migrate-jpa-to-mybatis-plus
- Phase: design
- Mode: compact
- Context hash: 1d7b4474936ae79c9dde329f03270aeb038cf5d92fb1fa99550d2126588b9081

Generated-by: comet-handoff.sh

OpenSpec remains the canonical capability spec. This handoff is a deterministic, source-traceable context pack, not an agent-authored summary.

## openspec/changes/migrate-jpa-to-mybatis-plus/proposal.md

- Source: openspec/changes/migrate-jpa-to-mybatis-plus/proposal.md
- Lines: 1-56
- SHA256: 21ec90618d4d1eaa6dd6d594a9c4d06891739663c6a3e9a13c9c8a5689bb9ce4

```md
# 变更提案: JPA → MyBatis-Plus 迁移

## Why

当前数据层基于 **Spring Data JPA（Hibernate）** 实现，在本项目 16 个扁平实体场景下，过度自动化的"全自动 ORM"反而成为负担：
- JPQL 的 UPDATE/DELETE 散落在 30+ 处 `@Query` 注解中，SQL 不直观，调试困难
- 100+ 个 `findById().orElse(...)` 调用依赖 JPA 行为，弱类型且不可空
- 软删除 `is_deleted` 字段在每个查询都需手写 `WHERE` 过滤，重复且易漏
- Schema 变更依赖 `spring.jpa.hibernate.ddl-auto`，生产环境 (`validate`) 与开发环境 (`update`) 行为不一致，缺版本化管理

本次迁移到 **MyBatis-Plus 3.5.9 + Flyway 10**，目标是在保留 JPA 便利性的同时获得 SQL 完全可控 + Schema 版本化。

## What Changes

- **BREAKING**: 移除 `spring-boot-starter-data-jpa` 依赖；引入 `mybatis-plus-spring-boot3-starter` + `flyway-core` + `flyway-mysql`
- **BREAKING**: 16 个实体类 (`model/`) 重构 JPA 注解 → MyBatis-Plus 注解（`@TableName` / `@TableId` / `@TableField` / `@TableLogic` / `@Version`）
- **BREAKING**: 16 个 Repository 接口 (`repository/`) 改造为 Mapper 接口 (`mapper/`)，继承 `BaseMapper<T>`，保留 `IService<T>` 抽象
- **BREAKING**: 14 个 Service 类继承 `ServiceImpl<Mapper, Entity>`，方法调用从 `repo.save/findById` 改为 `mapper.insert/selectById` 或继承自 `IService` 的方法
- **新增**: 16 个实体全部增加 `version INT DEFAULT 0` 列（Flyway `V2__add_version.sql`）
- **新增**: `db/schema.sql` 改造为 Flyway `V1__init.sql`，启动时由 Flyway 自动管理
- **新增**: MyBatis-Plus 全插件（PaginationInner + OptimisticLocker + BlockAttack + IllegalSQL）
- **修改**: 4 个测试文件 (`ConversationServiceTest` / `ChatServiceTest` / `RecordServiceTest` / `BloomFilterServiceImplTest`) 的 Mockito mock 目标与方法名同步重写
- **修改**: `application.yml` / `application-dev.yml` / `application-prod.yml` 移除 JPA 配置段，添加 MyBatis-Plus 与 Flyway 配置

## Capabilities

### New Capabilities

- `data-access-layer`: 基于 MyBatis-Plus 的持久层，配套 Flyway 版本化 Schema 管理、软删除自动注入、分页/乐观锁/防全表更新插件

### Modified Capabilities

无（项目尚无既有 spec 涉及持久层行为约束，本次为首次建立）

## Impact

**影响面（32 个文件级别）:**

| 类别 | 数量 | 备注 |
|---|---|---|
| `backend/pom.xml` | 1 | 依赖替换 |
| `backend/src/main/resources/application*.yml` | 3 | 配置段调整 |
| `backend/src/main/resources/db/schema.sql` | 1 | 改造为 Flyway V1 |
| `backend/src/main/resources/db/migration/V1__init.sql` | 1 | 新建（来自旧 schema.sql） |
| `backend/src/main/resources/db/migration/V2__add_version.sql` | 1 | 新建（添加 version 列） |
| `backend/src/main/java/com/ai/food/model/*.java` | 16 | 实体类注解重构 |
| `backend/src/main/java/com/ai/food/repository/*.java` | 16 | 改造为 `mapper/*.java` |
| `backend/src/main/java/com/ai/food/service/**/*.java` | 14 | Service 继承结构改造 |
| `backend/src/main/java/com/ai/food/config/MybatisPlusConfig.java` | 1 | 新建（插件注册 + MetaObjectHandler） |
| `backend/src/test/java/com/ai/food/**/*.java` | 4 | Mockito mock 同步重写 |

**API 行为:** 100% 保持兼容，所有 REST 接口契约不变。

**部署:** 单次冷切换（DB schema 不变 → 应用重启 → 自动 Flyway migrate + 启动 MyBatis-Plus 上下文），无停机窗口要求外的额外步骤。

**回滚:** 通过 `git revert` 恢复代码；Flyway 迁移可手动 `flyway:undo` 或备份 DB 还原。
```

## openspec/changes/migrate-jpa-to-mybatis-plus/design.md

- Source: openspec/changes/migrate-jpa-to-mybatis-plus/design.md
- Lines: 1-135
- SHA256: 9e634c71ff2b770bc8709ef3e332f2a82f3ca1edca60e1b14c88000ebb22cf49

[TRUNCATED]

```md
# 设计文档: JPA → MyBatis-Plus 迁移

## Context

**当前状态:**
- 16 个扁平 `@Entity` 实体，全部带 JPA 注解（`@Entity` / `@Table` / `@Id` / `@GeneratedValue` / `@Column` / `@PrePersist` / `@PreUpdate`）
- 16 个 `JpaRepository<T, Long>` 子接口，含 100+ 条 `@Query` JPQL（含 1 条 `nativeQuery`）
- 14 个 `Service` 类通过 `@Autowired` 注入 Repository，调用 `.save()` / `.findById()` / `.findAll()` / `.existsByXxx()` / 分页 `Pageable`
- Schema 由 `db/schema.sql`（265 行）配合 `spring.sql.init` + `ddl-auto: update|validate` 兜底
- 16 个实体全部使用软删除字段 `is_deleted`，散落 30+ 处 `WHERE is_deleted = 0/false`
- 4 个测试文件通过 Mockito mock Repository 接口

**约束条件:**
- JDK 21 + Spring Boot 3.4 + Hibernate 6（JPA 当前栈）
- MySQL 8，Schema 已就绪
- 必须保留所有 REST API 行为不变
- 不修改业务逻辑层（Service / Controller / DTO / Validator 行为保持）

## Goals / Non-Goals

**Goals:**
- 完整移除 JPA，引入 MyBatis-Plus 3.5.9 作为持久层
- 软删除自动化（`@TableLogic`）
- 分页、乐观锁、防全表更新与删除插件开箱即用
- Flyway 10 接管 Schema 版本化（V1 初始 + V2 加 version 列）
- Service 层继承 `IService<T>` 抽象，简化 CRUD 调用
- 16 个实体 + 16 个 Mapper + 14 个 Service + 4 个测试完整迁移

**Non-Goals:**
- 不改业务逻辑、API、DTO
- 不做 SQL 性能优化（按 1:1 翻译）
- 不停机迁移（单次冷切换）
- 不引入 MyBatis-Plus 高级特性（如多租户、动态表名）
- 不重写测试逻辑（仅同步 mock 目标）

## Decisions

### Decision 1: 选 MyBatis-Plus 而非原生 MyBatis

**理由:**
- BaseMapper<T> 提供 18 个内置方法（insert/updateById/selectById/selectList/deleteById 等），16 个实体 90% 的 CRUD 自动覆盖
- IService<T> + ServiceImpl<M, T> 让 Service 层直接继承 `save/findById/list` 等方法，省去 100+ 处机械调用代码
- LambdaQueryWrapper 提供类型安全的条件构造，避免写 SQL 字符串拼装
- 与原生 MyBatis 100% 兼容，复杂 SQL 仍可用 `@Select` 注解或 XML

**备选:** 原生 MyBatis — 每个 Mapper 需手写 5-6 个基础方法，~16*5=80 个重复方法，工作量翻倍。

### Decision 2: 注解式 Mapper 而非 XML

**理由:**
- 100+ 条 JPQL 翻译成 `@Select` / `@Update` / `@Delete` / `@Insert` 注解，方法定义与 SQL 同行，IDE 跳转无需跨文件
- 减少 XML 维护成本（虽然 MyBatis 生态习惯 XML，但本项目 SQL 都比较直白）
- MyBatis-Plus 注解兼容性更好

**备选:** XML — 传统 MyBatis 风格，SQL 集中管理但维护成本更高。

### Decision 3: @TableLogic 自动化软删除

**理由:**
- 16 个实体**全部**有 `is_deleted` 字段，30+ 条 `@Query` 显式过滤——这是 JPA 给你"全自动"的核心痛点
- `@TableLogic` 让 MP 自动在 `selectXxx` / `deleteById` 注入 `WHERE is_deleted=0` / `UPDATE ... SET is_deleted=1`
- 30+ 条 JPQL 中的 `is_deleted` 过滤可以删除，代码量 -15%
- 行为与 JPA `@SQLDelete` + `@Where` 等价，但更轻量

**注意事项:**
- `@TableLogic` 默认值必须与 schema 一致：`@TableLogic(value = "0", delval = "1")` 表示 0=未删、1=已删
- 已有的 `softDeleteBySessionId` / `markSoftDeleted` 等方法保留（业务要求显式更新 `is_deleted`，与 `@TableLogic` 的自动 deleteById 不冲突）

### Decision 4: @Version 乐观锁

**理由:**
- 16 个实体均有并发更新场景（FeedPost 点赞、ChatMessage 已读、UserFollow 互关等）
- `@Version` + `OptimisticLockerInnerInterceptor` 让所有 `updateById` 自动 `WHERE version = ?`，防止丢失更新
- Flyway V2 添加 `version INT DEFAULT 0` 列；存量数据默认 0 不影响

**注意:** `insert` 时 version 默认 0，每次 `updateById` 自动 +1。开发者无需手动设置。

### Decision 5: 包名 repository/ → mapper/

**理由:**
```

Full source: openspec/changes/migrate-jpa-to-mybatis-plus/design.md

## openspec/changes/migrate-jpa-to-mybatis-plus/tasks.md

- Source: openspec/changes/migrate-jpa-to-mybatis-plus/tasks.md
- Lines: 1-50
- SHA256: cf65eb1a58e80281c4368acd602e83e30d2078b691a0da8a7b826fbf565900a0

```md
# 任务清单: JPA → MyBatis-Plus 迁移

## 1. 基础设施层（顺序执行）

- [ ] 1.1 更新 `backend/pom.xml`：移除 `spring-boot-starter-data-jpa`，新增 `mybatis-plus-spring-boot3-starter` (3.5.9) + `flyway-core` + `flyway-mysql` (10.x)
- [ ] 1.2 重写 `application.yml`：移除 JPA 相关默认配置
- [ ] 1.3 重写 `application-dev.yml`：移除 `spring.jpa.*` 与 `spring.sql.init` 段，添加 MyBatis-Plus 与 Flyway 配置
- [ ] 1.4 重写 `application-prod.yml`：移除 `spring.jpa.hibernate.ddl-auto: validate`，添加 Flyway 配置
- [ ] 1.5 新建 `config/MybatisPlusConfig.java`：注册 `PaginationInnerInterceptor` + `OptimisticLockerInnerInterceptor` + `BlockAttackInnerInterceptor` + `MetaObjectHandler`

## 2. Schema 迁移（Flyway）

- [ ] 2.1 创建 `db/migration/V1__init.sql`：复制旧 `db/schema.sql` 内容（保留 `CREATE TABLE IF NOT EXISTS`）
- [ ] 2.2 创建 `db/migration/V2__add_version.sql`：为 16 张业务表添加 `version INT NOT NULL DEFAULT 0` 列
- [ ] 2.3 删除旧 `db/schema.sql`（或保留作为参考但不加载）

## 3. 实体类注解重构（16 个 model/*.java）

- [ ] 3.1 重构 user/social 域 4 个实体：`SysUser` / `UserBloomFilter` / `UserFollow` / `BloomSyncLog`（去掉 JPA 注解，加 `@TableName` / `@TableId` / `@TableField` / `@TableLogic` / `@Version`）
- [ ] 3.2 重构 content 域 4 个实体：`Photo` / `FeedPost` / `FeedComment` / `ShareRecord`
- [ ] 3.3 重构 chat 域 4 个实体：`ChatConversation` / `ChatMessage` / `ChatFile` / `ChatPhoto`
- [ ] 3.4 重构 conversation 域 4 个实体：`ConversationSession` / `CollectedParam` / `QaRecord` / `RecommendationResult`

## 4. Mapper 接口创建（16 个 mapper/*.java）

- [ ] 4.1 创建 user/social 域 4 个 Mapper：`UserMapper` / `UserBloomFilterMapper` / `UserFollowMapper` / `BloomSyncLogMapper`（extends `BaseMapper<T>`，@Select/@Update 注解翻译 100+ 条 JPQL）
- [ ] 4.2 创建 content 域 4 个 Mapper：`PhotoMapper` / `FeedPostMapper` / `FeedCommentMapper` / `ShareRecordMapper`
- [ ] 4.3 创建 chat 域 4 个 Mapper：`ChatConversationMapper` / `ChatMessageMapper` / `ChatFileMapper` / `ChatPhotoMapper`
- [ ] 4.4 创建 conversation 域 4 个 Mapper：`ConversationSessionMapper` / `CollectedParamMapper` / `QaRecordMapper` / `RecommendationResultMapper`
- [ ] 4.5 删除旧 `repository/` 目录（16 个文件全部迁移到 `mapper/` 后）

## 5. Service 层继承 IService 抽象（14 个 service/**/*.java）

- [ ] 5.1 重构 auth/user/upload/share/follow/like/match 域 service（7 个）：extends `ServiceImpl<Mapper, Entity>`，改 repository 注入为 mapper 注入
- [ ] 5.2 重构 feed/conversation/chat/record/notification/bloom 域 service（7 个）：同上

## 6. 测试改造（4 个 test 文件 Mockito mock 重写）

- [ ] 6.1 `BloomFilterServiceImplTest.java`：mock 目标从 Repository 改为 Mapper，方法名 `save`→`insert`、`findById`→`selectById`
- [ ] 6.2 `ConversationServiceTest.java`：同上
- [ ] 6.3 `ChatServiceTest.java`：同上
- [ ] 6.4 `RecordServiceTest.java`：同上

## 7. 验证（必须全绿才能进入 verify 阶段）

- [ ] 7.1 `mvn clean compile -DskipTests` 零错误零警告
- [ ] 7.2 `mvn test` 所有测试通过
- [ ] 7.3 应用启动 + Flyway 自动执行 V1 + V2 成功，`flyway_schema_history` 表存在且 V1/V2 状态为 success
- [ ] 7.4 5 个核心接口冒烟测试：用户注册/登录、发 feed、聊天发送、关注、用户搜索
- [ ] 7.5 检查 actuator 健康端点 `/actuator/health` 返回 `status: UP` 且 `db` 子项无 `error`
```

## openspec/changes/migrate-jpa-to-mybatis-plus/specs/data-access-layer/spec.md

- Source: openspec/changes/migrate-jpa-to-mybatis-plus/specs/data-access-layer/spec.md
- Lines: 1-120
- SHA256: 4fb154cdf6d2b1c8f538c746d06202edc241fc2ed54f060af70e3ca0722ea10c

[TRUNCATED]

```md
# 数据访问层规范 (data-access-layer)

本规范定义本项目持久层在迁移到 MyBatis-Plus 后的行为契约。

## ADDED Requirements

### Requirement: 实体-表映射

系统 MUST 使用 MyBatis-Plus 注解将 POJO 实体映射到 MySQL 数据表。每个实体 MUST 标注 `@TableName` 显式声明表名（即使表名与类名转换规则一致），并通过 `@TableId` 声明主键策略（`IdType.ASSIGN_ID` 雪花算法），通过 `@TableField` 显式声明非主键字段映射（特别是 snake_case 字段）。

#### Scenario: 实体启动时映射成功
- **WHEN** Spring Boot 应用启动并初始化 MyBatis-Plus 上下文
- **THEN** 16 个实体全部成功注册到 `SqlSessionFactory`，无 `BindingException`
- **AND** 实体类的 `getDeclaredFields` 与数据库表字段 1:1 对应（按 `@TableField` 映射）

#### Scenario: snake_case 字段名映射
- **WHEN** 实体字段为 `createdAt`（驼峰）且表字段为 `created_at`（下划线）
- **THEN** MyBatis-Plus 必须在生成的 SQL 中正确引用 `created_at` 列

### Requirement: 软删除自动化

所有包含 `is_deleted` 列的实体 MUST 标注 `@TableLogic(value = "0", delval = "1")`。系统 MUST 在所有 `BaseMapper.selectXxx` 调用中自动追加 `WHERE is_deleted = 0` 条件；`deleteById` MUST 被翻译为 `UPDATE ... SET is_deleted = 1 WHERE id = ?` 而非物理删除。

#### Scenario: selectById 过滤已删除
- **WHEN** 调用 `mapper.selectById(1)` 且该 id 对应记录 `is_deleted = 1`
- **THEN** 返回 `null`（不是该记录）

#### Scenario: deleteById 软删除
- **WHEN** 调用 `mapper.deleteById(1)`
- **THEN** 数据库执行 `UPDATE xxx SET is_deleted = 1 WHERE id = 1`
- **AND** 数据库中物理行仍然存在

#### Scenario: list 查询自动过滤
- **WHEN** 调用 `mapper.selectList(null)` 查询表 T
- **THEN** 生成的 SQL 为 `SELECT ... FROM T WHERE is_deleted = 0`
- **AND** 已删除行不出现在结果中

### Requirement: 乐观锁

所有 16 个实体 MUST 包含 `version INT NOT NULL DEFAULT 0` 列。`@Version` 注解 MUST 标注在 `version` 字段上。`OptimisticLockerInnerInterceptor` MUST 拦截所有 `updateById(entity)` 调用，生成的 SQL MUST 包含 `WHERE id = ? AND version = ?` 且 `SET version = version + 1`，更新成功返回 1，冲突返回 0。

#### Scenario: 首次更新成功
- **WHEN** 调用 `mapper.updateById(entity)`，entity.version = 0
- **THEN** SQL 为 `UPDATE T SET ..., version = 1 WHERE id = ? AND version = 0`
- **AND** 返回 1（成功）

#### Scenario: 并发更新冲突
- **WHEN** 实体当前 version = 1，调用方传入 entity.version = 0
- **THEN** SQL 命中 0 行，返回 0
- **AND** 调用方可通过返回值判断需要重试或放弃

### Requirement: Flyway Schema 版本化

系统 MUST 使用 Flyway 10 接管数据库 Schema 管理。`backend/src/main/resources/db/migration/` 目录下 MUST 存在 `V1__init.sql`（首次初始化脚本，来自旧 `db/schema.sql`）和 `V2__add_version.sql`（为 16 张表添加 `version` 列）。应用启动时 Flyway MUST 自动执行未应用的 migration；`spring.sql.init.mode` MUST 配置为 `never`（由 Flyway 接管）。

#### Scenario: 首次启动执行 V1+V2
- **WHEN** 数据库为全新库（无 `flyway_schema_history` 表）
- **THEN** Flyway baseline 后依次执行 V1 和 V2
- **AND** 16 张业务表全部创建
- **AND** 所有表都包含 `version INT NOT NULL DEFAULT 0` 列

#### Scenario: 增量启动跳过已应用版本
- **WHEN** 数据库已存在 `flyway_schema_history` 且 V1、V2 已应用
- **THEN** Flyway 跳过 V1、V2
- **AND** 应用正常启动无错误

#### Scenario: V2 迁移给存量表加 version 列
- **WHEN** V1 已应用、V2 未应用
- **THEN** Flyway 执行 V2，16 张表增加 `version` 列，默认值 0
- **AND** 存量数据 `version` 自动填充 0

### Requirement: 分页插件

`MybatisPlusConfig` MUST 注册 `PaginationInnerInterceptor` Bean。`BaseMapper.selectPage(IPage<T>, Wrapper<T>)` MUST 自动添加 `LIMIT ?, ?` 并执行 `SELECT COUNT(*)` 查询总记录数。`@Select` 注解中的分页查询 MUST 通过 `PageHelper.startPage(...)` 或改写为 `selectPage` 实现。

#### Scenario: IService.page 调用
- **WHEN** service 调用 `service.page(new Page<>(1, 10))`
- **THEN** MP 生成 `SELECT ... LIMIT 0, 10` + `SELECT COUNT(*) FROM T`
- **AND** 返回 `IPage<T>` 对象包含 `records`、`total`、`size`、`current` 等字段

```

Full source: openspec/changes/migrate-jpa-to-mybatis-plus/specs/data-access-layer/spec.md


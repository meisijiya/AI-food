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

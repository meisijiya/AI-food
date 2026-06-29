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

### Requirement: 防全表更新与删除

`MybatisPlusConfig` MUST 注册 `BlockAttackInnerInterceptor` Bean。任何未指定 `WHERE` 条件的 `update` 或 `delete` 语句 MUST 抛出异常 `MyBatisSystemException`，防止误操作清空整张表。

#### Scenario: 全表 update 报错
- **WHEN** 调用 `mapper.update(null, new LambdaUpdateWrapper<>())`（无 WHERE）
- **THEN** 抛出 `BlockAttackInnerInterceptor` 异常
- **AND** 数据库无任何更新发生

#### Scenario: 全表 delete 报错
- **WHEN** 调用 `mapper.delete(new LambdaQueryWrapper<>())`（无 WHERE）
- **THEN** 抛出异常
- **AND** 数据库无任何删除发生

### Requirement: IService 抽象

所有 14 个 Service 类 MUST 继承 `ServiceImpl<Mapper, Entity>` 以获得 `IService<T>` 默认方法。Service 的默认 CRUD 调用 MUST 优先使用 `IService` 提供的方法（`save` / `saveBatch` / `removeById` / `getById` / `list` / `page` 等），自定义查询 MUST 通过注入的 `BaseMapper` 调用。

#### Scenario: IService.save 调用
- **WHEN** service 调用 `this.save(entity)`
- **THEN** 调用 `mapper.insert(entity)`（新增场景）

#### Scenario: IService.getById 返回 Optional 包装
- **WHEN** service 调用 `this.getById(id)` 且记录不存在
- **THEN** 返回 `null`
- **AND** 调用方应使用 `Optional.ofNullable(this.getById(id))` 包装

### Requirement: 时间戳自动填充

`MybatisPlusConfig` MUST 注册 `MetaObjectHandler` Bean，监听实体 `insert` 和 `update` 操作，自动填充 `createdAt` 和 `updatedAt` 字段。插入时 `createdAt = LocalDateTime.now()`，更新时 `updatedAt = LocalDateTime.now()`，与原 JPA `@PrePersist` / `@PreUpdate` 行为等价。

#### Scenario: 插入自动填 createdAt
- **WHEN** 调用 `mapper.insert(entity)` 且 entity.createdAt 为 null
- **THEN** MetaObjectHandler 自动设置 `createdAt = LocalDateTime.now()`
- **AND** 数据库中该字段非空

#### Scenario: 更新自动填 updatedAt
- **WHEN** 调用 `mapper.updateById(entity)`
- **THEN** MetaObjectHandler 自动设置 `updatedAt = LocalDateTime.now()`
- **AND** 数据库中 `updatedAt` 字段被更新

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
- MyBatis 生态中 mapper 是标准命名；保留 `repository` 会让项目看起来"半 JPA 半 MyBatis"
- 14 个 Service 的 import 改起来是机械操作，~1 小时工作量
- 与新建的 `MybatisPlusConfig` 等配置类风格一致

### Decision 6: Flyway 10 + flyway-mysql

**理由:**
- Spring Boot 3.4 默认管理 Flyway 10，对 MySQL 8 需要单独的 `flyway-mysql` 适配
- V1__init.sql 直接来自现有 `db/schema.sql`（保留 `IF NOT EXISTS` 兼容）
- V2__add_version.sql 一次性给 16 张表加 `version INT DEFAULT 0`
- 关闭 `spring.sql.init.mode`（Flyway 接管），避免重复执行

## Risks / Trade-offs

| 风险 | 影响 | 缓解 |
|---|---|---|
| **MyBatis-Plus 注解 vs 字段名不一致** | 编译/启动失败 | 每个实体用 `@TableField` 显式映射 snake_case 字段 |
| **`selectById` 返回 null 而非 Optional** | 14 个 Service 40+ 处 `.orElse(...)` 改写 | 统一封装为 `Optional.ofNullable(mapper.selectById(id))` |
| **`save()` 行为差异** | `repo.save(x)` 是 merge 语义，MP `save` 是 select-then-insert/update | 业务层用 `mapper.insert(x)` 新增、`updateById(x)` 更新（强制显式） |
| **分页插件拦截条件** | `PaginationInnerInterceptor` 仅对 `BaseMapper.selectPage(IPage, Wrapper)` 生效 | 自定义 `@Select` 分页查询需用 `PageHelper` 或改为 `selectPage` |
| **Mockito mock 接口名变更** | 4 个测试需要重写 | 同步重命名 mock 目标：`XxxRepository` → `XxxMapper`，方法名 `save` → `insert` |
| **Flyway V1 vs 旧 schema.sql 冲突** | 启动时 FK/索引已存在报错 | `CREATE TABLE IF NOT EXISTS` 保留；Flyway baseline-on-migrate=true 容错 |
| **`@PrePersist` / `@PreUpdate` 替换** | 时间戳自动填充逻辑丢失 | MyBatis-Plus `MetaObjectHandler` 在 config 中实现 |
| **生产环境存量数据无 `version` 列** | V2 迁移 ALTER 16 张表，大库耗时 | 接受一次停机窗口（V2 是简单 ADD COLUMN DEFAULT 0，毫秒级） |

## Migration Plan

### 部署步骤（单次冷切换）

1. **代码预热:** PR 合入 main 后，CI 跑 `mvn clean test` 全绿
2. **数据库准备:** 预跑 `openspec` 验证（无），直接走部署
3. **应用启动:** 容器拉起后 Spring Boot 自动执行：
   - Flyway 检测 `flyway_schema_history` 表 → 不存在则 baseline → 执行 V1 → 执行 V2
   - 16 张表获得 `version` 列
   - MyBatis-Plus 上下文初始化
   - 14 个 Service 通过 `IService<T>` 注入 Mapper
4. **冒烟测试:** 启动后跑 5 个核心接口：登录 / 发 feed / 聊天发送 / 关注 / 搜索
5. **观察:** 5 分钟内无 ERROR 日志 → 切换完成

### 回滚策略

- **代码回滚:** `git revert <merge-commit>` 后重新部署
- **Schema 回滚:** Flyway 不支持自动 undo，需手动：
  ```sql
  -- V2 undo
  ALTER TABLE sys_user DROP COLUMN version;
  -- ... 对其他 15 张表重复
  ```
  或从备份还原 DB。

## Open Questions

1. **存量数据 `is_deleted` 默认值:** schema.sql 是否已经显式设置？迁移后由 `@TableLogic(value="0", delval="1")` 注解处理，但需要确保 schema 列默认值一致。
2. **MyBatis-Plus 是否使用 `tenant_id` 多租户:** 否，本项目为单租户。
3. **是否启用 MP 性能分析插件:** 否（dev 环境 sql 打印已通过 `org.hibernate.SQL` 改为 `org.mybatis.spring.SqlSessionUtils`）；生产环境有 actuator metrics 即可。

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

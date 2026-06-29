---
comet_change: migrate-jpa-to-mybatis-plus
role: technical-design
canonical_spec: openspec
---

# 技术设计: JPA → MyBatis-Plus 迁移

> 对应 OpenSpec change: `migrate-jpa-to-mybatis-plus`
> 上游权威: `openspec/changes/migrate-jpa-to-mybatis-plus/{proposal,design,specs/data-access-layer/spec,tasks}.md`

## 1. 背景与上下文

项目当前 16 个实体全部基于 Spring Data JPA + Hibernate，100+ 条 `@Query` JPQL 散落在 Repository 层，Schema 由 `spring.sql.init` + `ddl-auto` 兜底管理。本次目标：完整切换到 MyBatis-Plus 3.5.9 + Flyway 10，保留 MySQL 8 存储，业务行为 100% 等价。

完整背景与高层决策见 OpenSpec `proposal.md` / `design.md`。本设计文档仅补充**实施细节**与**测试策略**。

## 2. 实施路径（6 个关键点）

### 2.1 POM 依赖替换

**删除：**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
```

**新增：**
```xml
<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
    <version>3.5.9</version>
</dependency>
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-mysql</artifactId>
</dependency>
```

### 2.2 application*.yml 重构

**删除（dev + prod）：**
```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: ...
    show-sql: false
    open-in-view: false
    properties:
      hibernate:
        dialect: ...
  sql:
    init:
      mode: always
      schema-locations: classpath:db/schema.sql
      continue-on-error: true
```

**新增（dev + prod）：**
```yaml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true  # 兼容已有库
  sql:
    init:
      mode: never  # Flyway 接管

mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.nologging.NoLoggingImpl
  global-config:
    db-config:
      id-type: ASSIGN_ID
      logic-delete-field: isDeleted
      logic-delete-value: 1
      logic-not-delete-value: 0
      version-field: version
```

### 2.3 MybatisPlusConfig（新建）

```java
@Configuration
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        interceptor.addInnerInterceptor(new BlockAttackInnerInterceptor());
        return interceptor;
    }

    @Bean
    public MetaObjectHandler metaObjectHandler() {
        return new TimeFieldMetaObjectHandler();
    }
}

@Component
class TimeFieldMetaObjectHandler implements MetaObjectHandler {
    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        strictInsertFill(metaObject, "createdAt", LocalDateTime.class, now);
        strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, now);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
    }
}
```

### 2.4 实体类注解重构模式（统一模板）

```java
@Data
@TableName("sys_user")
public class SysUser {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField(value = "username", condition = "%s LIKE #{%s}")
    private String username;

    @TableField("is_deleted")
    @TableLogic(value = "0", delval = "1")
    private Integer isDeleted;

    @Version
    @TableField("version")
    private Integer version;

    // createdAt / updatedAt 由 MetaObjectHandler 自动填充
}
```

### 2.5 Mapper 接口模式（统一模板）

```java
@Mapper
public interface UserMapper extends BaseMapper<SysUser> {

    Optional<SysUser> findByUsername(@Param("username") String username);

    @Select("SELECT * FROM sys_user WHERE username = #{username} AND is_deleted = 0 LIMIT 1")
    SysUser selectByUsernameRaw(@Param("username") String username);

    @Update("UPDATE sys_user SET is_deleted = 1, version = version + 1 WHERE id = #{id} AND version = #{version}")
    int softDeleteById(@Param("id") Long id, @Param("version") Integer version);
}
```

### 2.6 Service 继承模式

```java
@Service
public class UserService extends ServiceImpl<UserMapper, SysUser> {

    public Optional<SysUser> findByUsername(String username) {
        return Optional.ofNullable(baseMapper.selectByUsernameRaw(username));
    }

    public boolean existsByUsername(String username) {
        return baseMapper.selectCount(new LambdaQueryWrapper<SysUser>()
            .eq(SysUser::getUsername, username)) > 0;
    }
}
```

## 3. JPQL → @Select/@Update 翻译规则

| JPQL 模式 | MyBatis-Plus 翻译 |
|---|---|
| `findByXxx(xxx)` 派生方法 | `baseMapper.selectOne(new LambdaQueryWrapper<T>().eq(T::getXxx, xxx))` |
| `@Query("SELECT x FROM T WHERE ...") List<T> find(...)` | `@Select("SELECT * FROM t WHERE ...") List<T> find(...)` |
| `@Modifying @Query("UPDATE T SET ...") void update(...)` | `@Update("UPDATE t SET ...") int update(...)` |
| `@Query(value = "..." , nativeQuery = true)` | `@Select/@Update(value = "${...}")` （同样可原生） |
| `Page<X> findByFilters(...)` with `Pageable` | 改方法签名为 `IPage<X> selectXxxPage(IPage<X> page, @Param("...") String x)` + `Page<X>` 调用方构造 |

**注：** `@TableLogic` 自动处理 `WHERE is_deleted = 0` 和 `deleteById` 翻译，所以**所有显式写 `is_deleted = 0` 的 JPQL 可以删掉**。但**显式 UPDATE `is_deleted = 1` 的方法（如 `softDeleteBySessionId`）需要保留**，因为业务要求显式更新。

## 4. 测试改造要点

### 4.1 Mockito 同步

| JPA 时代 | MyBatis-Plus 时代 |
|---|---|
| `private UserRepository userRepository;` | `private UserMapper userMapper;` |
| `when(userRepository.save(any())).thenReturn(entity);` | `when(userMapper.insert(any())).thenReturn(1);` |
| `when(userRepository.findById(id)).thenReturn(Optional.of(x));` | `when(userMapper.selectById(id)).thenReturn(x);` |
| `when(userRepository.findAll()).thenReturn(list);` | `when(userMapper.selectList(null)).thenReturn(list);` |
| `when(userRepository.existsByUsername(name)).thenReturn(true);` | `when(userMapper.exists(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, name))).thenReturn(true);` |
| `verify(repo).save(entity);` | `verify(mapper).insert(entity);` |

### 4.2 service 构造器改造

每个 service 的 `@Autowired` 改为构造器注入 Mapper（MP 建议方式）：

```java
// 旧
private final UserRepository userRepository;
public UserService(UserRepository userRepository) { this.userRepository = userRepository; }

// 新（注意：extends ServiceImpl 自动注入 baseMapper，构造器仅接收额外依赖）
private final UserMapper userMapper;
public UserService(UserMapper userMapper) { this.userMapper = userMapper; }
// 或
public class UserService extends ServiceImpl<UserMapper, SysUser> {
    // baseMapper 已通过 ServiceImpl 注入，无需构造器
}
```

## 5. 验证清单

| # | 验证项 | 命令 | 通过标准 |
|---|---|---|---|
| 1 | 编译 | `mvn clean compile -DskipTests` | 0 error, 0 warning |
| 2 | 单元测试 | `mvn test` | 100% pass |
| 3 | Flyway 迁移 | 启动应用观察日志 | `flyway_schema_history` 写入 V1, V2 success |
| 4 | DB schema | `SHOW CREATE TABLE sys_user` | 含 `version int NOT NULL DEFAULT 0` |
| 5 | 健康检查 | `curl /actuator/health` | `db.status: UP`, 整体 `status: UP` |
| 6 | 软删行为 | `mapper.deleteById(1)` | `is_deleted` 变为 1，物理行仍在 |
| 7 | 软删过滤 | `mapper.selectById(1)` (删后) | 返回 `null` |
| 8 | 乐观锁 | `updateById(entity.version=0)` (DB version=1) | 返回 0 |
| 9 | 分页 | `service.page(new Page<>(1, 10))` | `total` 正确，`records.size() <= 10` |
| 10 | 冒烟 5 接口 | 手动 / Postman | 注册/登录/发 feed/聊天/关注 全通 |

## 6. 风险登记表（与 OpenSpec design.md 同步）

| ID | 风险 | 等级 | 缓解 |
|---|---|---|---|
| R1 | `@TableLogic` 与显式 `WHERE is_deleted` 冲突 | 中 | 显式 `UPDATE` 保留，自动过滤仅对 select/delete 生效 |
| R2 | MP `save` 是 select-then-insert/update，可能误用 | 中 | 业务层强制用 `insert`/`updateById`，禁用 `save` |
| R3 | `selectById` 返回 null（vs JPA Optional） | 中 | 统一封装 `Optional.ofNullable` |
| R4 | Mockito 重写测试可能漏 | 中 | 跑 `mvn test` 全套验证 |
| R5 | Flyway V2 ALTER 16 表耗时 | 低 | `ADD COLUMN DEFAULT 0` 是 instant 操作（MySQL 8） |
| R6 | `@Version` 字段缺失导致乐观锁插件报错 | 低 | 16 个实体全部加 `version INT DEFAULT 0` |

## 7. 实施顺序（与 OpenSpec tasks.md 一致）

```
1. 基础设施 (pom + 3 yml + MybatisPlusConfig)        ← 串行
2. Schema 迁移 (V1__init.sql + V2__add_version.sql)  ← 串行
3. 实体类重构 (16 个 model/*.java)                    ← 4 域并行
4. Mapper 创建 (16 个 mapper/*.java)                  ← 4 域并行（依赖 3）
5. Service 继承 (14 个 service/**/*.java)             ← 2 域并行（依赖 4）
6. 测试改造 (4 个 test/*.java)                        ← 4 文件并行（依赖 5）
7. 验证 (compile + test + 启动 + 冒烟)                ← 串行
```

**并发机会：** 任务 3、4、5、6 内部均可按域拆分给多个 @fixer 并行（互不重叠写权限）。

## 8. 退出条件

- [ ] 50 个文件全部按 OpenSpec tasks.md 任务清单修改完成
- [ ] `mvn clean compile -DskipTests` 零错
- [ ] `mvn test` 全绿
- [ ] 应用启动 + Flyway V1/V2 success + actuator/health UP
- [ ] 5 个核心接口冒烟测试通过
- [ ] 至少 1 个全表 delete 防护验证（抛 BlockAttackInnerInterceptor 异常）

## 9. Spec Patches

**无**。OpenSpec `specs/data-access-layer/spec.md` 8 个 Requirements + 18 个 Scenarios 已覆盖所有 6 项技术决策。无需补充。

## 10. 参考资料

- MyBatis-Plus 3.5.9 文档: <https://baomidou.com/>
- Flyway 10 + MySQL: <https://documentation.red-gate.com/fd/mysql-184127604.html>
- Spring Boot 3.4 配置: <https://docs.spring.io/spring-boot/docs/3.4.x/reference/htmlsingle/>

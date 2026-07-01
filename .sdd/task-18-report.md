# Task 18 Report — 配置 SBA 和 Druid 安全

## 状态

✅ 完成

## 改动

### 1. 新建 `backend/admin-server/src/main/java/com/aifood/admin/config/AdminSecurityConfig.java`

按 brief 在 `config/` 包下新建 `AdminSecurityConfig`,用 `SecurityFilterChain` + `formLogin` + `httpBasic` + `csrf.disable` 给 `/admin/sba/**` 加 ADMIN 角色保护,其余路径放行(交给现有 `common.AdminSecurityConfig` 处理 `/admin/api/**`、Druid 自带校验 `/admin/druid/**`)。

**修正**:brief 写的 `de.codecentric.spring.boot.admin.server.config.AdminServerProperties` 是 SBA 2.x 的旧包名;实际项目用 SBA 3.4.0,正确包名是 `de.codecentric.boot.admin.server.config`(已在 `spring-boot-admin-server-3.4.0.jar` 中确认)。

### 2. 修改 `backend/admin-server/src/main/resources/application.yml`

在 `spring:` 节点下新增 `security.user` 块,口令和用户名走环境变量(`SBA_USER` / `SBA_PASSWORD`),默认 `admin` / `admin123`,角色 `ADMIN`。这些值给 `Spring Security` 的 `InMemoryUserDetailsManager`,搭配新加的 `SecurityFilterChain` 完成 SBA 的 Basic/Form 登录。

## 既有文件说明

⚠️ `backend/admin-server/src/main/java/com/aifood/admin/common/AdminSecurityConfig.java` 已存在,但**不符合 brief 模式**:
- 用了 `WebSecurityCustomizer.ignoring()` 跳过 `/admin/api/**`(旧 lazy 方案,无认证),
- 没有 `SecurityFilterChain`,没有 `hasRole("ADMIN")`,没有 httpBasic,没有 csrf.disable。

按 brief 字面要求("`config/AdminSecurityConfig.java` 不存在则新建")在 `config/` 下新建,与 `common/` 那份并存:
- `common.AdminSecurityConfig`:`web.ignoring()` 让 AdminInterceptor 接管 `/admin/api/**`,不被 Security 提前 401。
- `config.AdminSecurityConfig`:`SecurityFilterChain` 给 `/admin/sba/**` 加 ADMIN 角色 + Basic/Form 登录。
两者职责不冲突,前者只 ignore 特定路径,后者接管其它路径的 Security 流程。

## 验证

- `mvn -f admin-server/pom.xml clean compile`: ✅ BUILD SUCCESS
- 仅编译验证,按 brief 要求不做应用启动测试

## 提交

- Commit: `1dc326760cac4a61f68df67fe1d3777ac3b9e47f`
- Message: `feat(admin): ensure SBA basic auth + spring.security.user`
- Files: `backend/admin-server/` (2 files: 1 new, 1 modified, +77 lines)

## 关注点

1. brief 的 `AdminServerProperties` 包名错了(旧 SBA 2.x 用法),已修正为 3.x 包名。
2. 旧 `common.AdminSecurityConfig` 未删除,与新 `config.AdminSecurityConfig` 并存(分别处理不同路径)。
3. `/admin/druid/**` 由 Druid StatViewServlet 自身校验 `login-username`/`login-password`(`application.yml` 已有配置),不受新 `SecurityFilterChain` 影响(`anyRequest().permitAll()`)。
4. 默认口令 `admin/admin123` 仅 dev 友好,生产环境务必设置 `SBA_PASSWORD` / `DRUID_PASSWORD` 环境变量,见 `🔐 敏感信息处理` 全局规则。
# Task 12 Report: AuthController 登录/登出/me

**Status:** DONE_WITH_CONCERNS
**Branch:** feature/admin-backend
**Commit:** d96bd92 — `feat(admin): add AuthController (login/me/logout) + disable common JwtAuthFilter in admin-server`

## What was built

4 new files under `backend/admin-server/`:

1. `dto/LoginReq.java` — `{username, password}`, `@NotBlank`. username 字段同时接受用户名或邮箱。
2. `dto/AdminUserVO.java` — 脱敏视图 (id/username/nickname/avatar/role)，`from(SysUser)` 工厂。
3. `controller/AuthController.java` — `POST /admin/api/auth/login`、`GET /me`、`POST /logout`。
4. `common/AdminFilterConfig.java` — **新增（计划外，见 Concern 1）**：取消 common 模块 `JwtAuthenticationFilter` 在 admin-server 的 servlet 注册。

## 与 brief 的差异（实际代码与 brief 模板不一致）

- **Mapper 名不同**：common 里是 `UserMapper`（注释「实为 sys_user 的 mapper」），方法为 `findByUsername` / `findByEmail`，**不是** brief 写的 `SysUserMapper.selectByUsername/selectByEmail`。已按真实 API 适配，无需新增 mapper 方法。
- **AdminSecurityConfig 未改**：Task 10 实际实现用的是 `web.ignoring().requestMatchers("/admin/api/**","/admin/api/auth/**")`，login 端点**已被放行**。brief 给的 `permitAll` 改写版本反而会触发 Basic 401（Task 10 concern 3 描述的正是 permitAll 的坑），故**保持原样不动**。

## Concern 1（关键，计划外修复）

admin-server 通过 `scanBasePackages` 扫描 `com.ai.food.common`，把 ai-food-app 的 `JwtAuthenticationFilter`（`@Component` 的 `OncePerRequestFilter`）当作 Filter Bean 被 Spring Boot **自动注册到 servlet 容器**，对所有 `/admin/api/**` 生效。该 filter 强制校验 Redis token 白名单（`token:{userId}`，app 登录时写入），而管理后台 token 由 AuthController 颁发、从不写 Redis → 携带 Bearer token 的 `/me`、`/logout` 全被该 filter 拦为 `401 Token已失效`。

`web.ignoring()` 只作用于 Spring Security 过滤链，对这个独立 servlet filter 无效。

**修复**：新增 `AdminFilterConfig`，用 `FilterRegistrationBean.setEnabled(false)` 取消该 filter 的 servlet 注册（不删 Bean，不影响 common 在 ai-food-app 的行为）。admin 鉴权完全由 `AdminInterceptor`（JWT + role，不依赖 Redis）完成。

> 这超出了 brief 范围但是登录链路跑通的必要条件。后续若 common 抽出 admin 专用 starter，应把这类「app-only filter」与 admin 上下文隔离，避免再次靠 scanBasePackages 误装配。

## Concern 2（次要）

`@Valid` 校验失败（空 username/password）当前由 `AdminExceptionHandler` 的兜底 `Exception` 分支捕获，返回 **500** + 冗长的 `MethodArgumentNotValidException` 文本，而非 400。建议后续给 `AdminExceptionHandler` 加一条 `@ExceptionHandler(MethodArgumentNotValidException.class)` 返回 400。未在本 Task 处理（不属 brief 范围，且不影响正常登录链路）。

## 验证（smoke test，prod profile，真实 MySQL+Redis）

admin-server 启动成功（Tomcat 8081）。测试结果：

| 用例 | 结果 |
|------|------|
| login via email (smoke@aifood.local) | 200, token + role=ADMIN ✓ |
| login via username (smokeuser) | 200, token + role=ADMIN ✓ |
| GET /me with Bearer token | 200, AdminUserVO role=ADMIN ✓ |
| POST /logout with token | 200 ✓ |
| login wrong password | 401 用户名或密码错误 ✓ |
| GET /me without token | 401 ✓ |

编译：`mvn -f admin-server/pom.xml compile` → BUILD SUCCESS。
DB 现有 ADMIN：id=1 smokeuser / smoke@aifood.local。

## 注意

- 仓库里存在一个陈旧的 `backend/src/...` 树（非 maven 模块），LSP 报错与本 Task 无关，未触碰。
- 只 `git add backend/admin-server/`，未提交 `.sdd/` 报告与 `backend.bak.*`。

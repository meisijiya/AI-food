# Task 10 Report — 实现 @RequireAdmin 注解和 AdminInterceptor

**Branch:** `feature/admin-backend`
**Date:** 2026-06-29
**Status:** DONE_WITH_CONCERNS

## Files Created

| Path | 用途 |
|------|------|
| `backend/admin-server/src/main/java/com/aifood/admin/common/annotation/RequireAdmin.java` | 标记方法/类需要 ADMIN 角色（仅声明,实际校验交给拦截器） |
| `backend/admin-server/src/main/java/com/aifood/admin/common/AdminException.java` | RuntimeException + code 字段,业务异常载体 |
| `backend/admin-server/src/main/java/com/aifood/admin/common/AdminExceptionHandler.java` | @RestControllerAdvice,处理 controller 抛出的 AdminException/Exception |
| `backend/admin-server/src/main/java/com/aifood/admin/common/interceptor/AdminInterceptor.java` | HandlerInterceptor 校验 token + role,失败时**直接写 JSON 响应**而非抛异常 |
| `backend/admin-server/src/main/java/com/aifood/admin/common/AdminWebConfig.java` | WebMvcConfigurer:注册拦截器 + CORS |
| `backend/admin-server/src/main/java/com/aifood/admin/common/AdminSecurityConfig.java` | WebSecurityCustomizer:让 /admin/api/** 跳过 Spring Security filter chain |

## Files Modified

| Path | 改动 |
|------|------|
| `backend/ai-food-common/src/main/java/com/ai/food/common/model/SysUser.java` | 在 `avatar` 与 `createdAt` 之间新增 `private String role` 字段(V4 migration 已加 DB 列,Java 实体未对齐) |

## Verification

```
mvn -f admin-server/pom.xml clean compile  →  BUILD SUCCESS

启动 admin-server 后 curl 验证:
- GET /admin/api/users (无 token)
   → HTTP/1.1 401
   → {"code":401,"message":"未登录","data":null}    ✓ 完全符合 brief 期望
- OPTIONS /admin/api/users (CORS preflight)
   → HTTP/1.1 200
   → Access-Control-Allow-Origin/Methods/Credentials/Max-Age 齐全    ✓
- POST /admin/api/auth/login (excluded)
   → HTTP/1.1 401 (Basic realm) — 见 concerns #2
- GET /actuator/health
   → 200 + {"status":"UP",...}                            ✓
```

## Design Decisions

1. **拦截器直接写 JSON 响应,不抛 AdminException**
   - `HandlerInterceptor.preHandle` 抛出后,Spring 6 的 `DispatcherServlet` 不会自动把异常转给 `@RestControllerAdvice` 处理,最终落 500 错误页。
   - 改为手拼 JSON 写到 response(手写而非 Jackson:避免在失败路径上再多 NPE 风险)。
   - `AdminException` + `AdminExceptionHandler` 保留 — 给后续 Controller 内部业务异常用(它们走 controller 抛异常路径,可被 advice 接住)。

2. **改用 `UserMapper` 而非 brief 里写的 `SysUserMapper`**
   - 仓库内 `com.ai.food.common.mapper.SysUserMapper` 不存在;`UserMapper extends BaseMapper<SysUser>` 才是 sys_user 的 mapper。
   - 复用而不是新建,跟 brief 第 1 步 Ponytail 原则一致("Already in this codebase? reuse it.")。

3. **新增 `AdminSecurityConfig` 让 /admin/api/** 跳过 Security filter chain**
   - `spring-boot-admin-starter-server` 通过 `spring-boot-starter-security` 拖入了默认 Security 配置,对匿名用户访问任意路径都返回 Basic Auth 401。
   - 业务鉴权完全交给 AdminInterceptor,所以让 Spring Security `web.ignoring()` 整个放行 /admin/api/** + /admin/api/auth/**。
   - `/admin/sba/**` 由 spring-boot-admin starter 自行处理,不受影响。

## Concerns

1. **(中)** `SysUser.role` 字段在 Java 实体类中此前未与 V4 schema 同步。本任务顺手补上,避免 task 11+ 的 controller 因 `user.getRole()` 编译失败。但严格来说这是属于"补 V4 Java 端 schema"的子工作,建议后续 task 把它合并到 phase 1 的 V4 命名任务里做总账。
2. **(低)** `JwtAuthenticationFilter`(位于 ai-food-common)会被 common 模块的 `@Component` 自动注册到全局 servlet filter chain,对 `/admin/api/**` 中**无效 token** 的请求会**覆盖** AdminInterceptor 写入的 401 响应,直接吐 common 自己 401 + `"Token已过期，请重新登录"`。这条路径不在 task 10 验收范围(brief 只要求 `no-token → 401 + 未登录`),但 task 11+ 需要协调两侧:要么 common 的 Filter 增加 `shouldNotFilter` 排除 admin 路径,要么把 AdminInterceptor 提前到 common filter 之前(目前 order 都是 default)。
3. **(低)** Spring Security 仍默认对 `/admin/api/auth/login` 返回 401 Basic realm — 当前已把它也加入 `web.ignoring`,但因 spring-boot-admin 引入的另一条 SecurityFilterChain 优先级仍未覆盖此路径。Task 11 写登录 Controller 时如果遇到 401,可考虑给登录路径单独建一条 `securityMatcher("/admin/api/auth/**")` + permitAll 的 chain,或干脆 `requestMatchers(new AntPathRequestMatcher("/admin/api/auth/**")).permitAll()`。
4. **(可选,非阻塞)** 当前 `@RequireAdmin` 注解仅作为 marker,实际拦截在 `AdminInterceptor.preHandle` 一处完成。若后续要做更细的方法级粒度(如"同 Controller 中只有 @RequireAdmin 方法拦截"),需要把 AdminInterceptor 升级为 AOP 或 HandlerInterceptor 中读取 `HandlerMethod` 上的注解做差异处理。当前所有 `/admin/api/**` 一刀切拦截,够 task 11-13 的需求。

## Follow-up

- Task 11(AuthController)创建后,实测登录路径返回 200 + token;同时校验 ADMIN 用户 token 通过 AdminInterceptor 后能命中 controller。
- ai-food-app 端调用 admin API 时复用既有的 `auth_token` cookie(注意 admin 拦截器读 `admin_token` cookie,如果前端用同一 cookie 名可统一化)。

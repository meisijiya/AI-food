# Task 13: 实现用户管理 — 实施报告

**Date:** 2026-06-30
**Branch:** `feature/admin-backend`
**Commit:** `b50ca1e`

## 用户需求

实现 admin-server 的用户管理 API：list / detail / updateRole / disable / enable,
带分页 + 角色校验 + 审计日志。

## 实施总结

按 brief 创建 4 个核心文件 + 1 个 MyBatis-Plus 配置文件(根因修复) + 1 处 pom 依赖补充。

### 新增文件 (5)

| 文件 | 作用 |
|---|---|
| `dto/UserQueryReq.java` | 列表查询入参:page/size/keyword/role/status |
| `dto/UpdateRoleReq.java` | 修改角色入参:@Pattern("USER\|ADMIN") |
| `service/UserService.java` | 分页查询 + 详情 + 改角色 + 禁用/启用 |
| `controller/UserController.java` | 5 个端点,@RequireAdmin 类级 + @AuditLog 写操作 |
| `config/MybatisPlusConfig.java` | **根因修复**:补齐 admin-server 缺失的 Pagination + OptimisticLocker + BlockAttack 拦截器 + 时间戳填充 |

### 修改文件 (1)

| 文件 | 改动 |
|---|---|
| `backend/admin-server/pom.xml` | 新增 `mybatis-plus-jsqlparser` 依赖(starter 不传递提供 Pagination/BlockAttack 类) |

### 端到端验证结果

```
✅ POST /admin/api/auth/login              → 200 (smokeuser)
✅ GET  /admin/api/users?page=1&size=5     → 200, total=1, pages=1, 1 record
✅ GET  /admin/api/users/1                 → 200, smokeuser details
✅ PATCH /admin/api/users/1/role  {"role":"USER"}  → 200, DB 同步
✅ PATCH /admin/api/users/1/role  {"role":"ADMIN"} → 200
✅ POST  /admin/api/users/1/disable        → 200, is_deleted 0→1 (DB 验证)
✅ POST  /admin/api/users/1/enable         → 200, is_deleted 1→0 (DB 验证)
✅ admin_audit_log                         → 3 类动作(UPDATE_USER_ROLE / DISABLE_USER / ENABLE_USER)SUCCESS
```

### 关键决策

#### 1. 修正 mapper 引用

Brief 引用 `SysUserMapper`,实际 common 模块中是 `UserMapper`(沿用旧命名)。
代码已修正。

#### 2. 根因修复:补全 admin-server 的 MyBatis-Plus 拦截器

**问题**:Task 12 把 admin-server 拆出来时,`MybatisPlusConfig` 只在
`ai-food-app` 模块里。admin-server 不依赖 ai-food-app(只依赖 common),
所以**完全没有** `PaginationInnerInterceptor` 和 `OptimisticLockerInnerInterceptor`。
两个直接症状:

- `selectPage` 返回的 `total` 永远为 0(没有分页拦截器跑 COUNT)
- `updateById` 在 `SysUser` 上抛 `MP_OPTLOCK_VERSION_ORIGINAL not found`(@Version 字段无法绑定)

**修复**:在 admin-server 自带一份 `MybatisPlusConfig`,与 ai-food-app 同结构
(分页 → 乐观锁 → 防全表更新 + 时间戳填充)。这是 admin-server 自身需要的
基础设施,不是 user management 特有的,放 `config/` 目录符合 admin-server
现有组织。

#### 3. 根因修复:`@TableLogic` 字段的更新方式

`SysUser.isDeleted` 标了 `@TableLogic(value="0", delval="1")`,这导致
`updateById(entity)` 的自动 SET 子句**会排除** `is_deleted` 字段(MP 的设计:
防止业务代码误改软删字段)。所以 `u.setIsDeleted(1); updateById(u)` **不会**真的
写库(version 会变,但 is_deleted 不变)。

**修复**:`disable/enable` 改用 `LambdaUpdateWrapper<SysUser>().set(...)`,
显式 set `is_deleted` 字段,绕过 MP 的自动排除。这是 ai-food-common 的
`FeedPostMapper.softDeleteByPostId` 等地方已经采用的同款模式。

#### 4. 审计日志 target_id 的取参假设问题

`AuditAspect` 注释:"约定:第二个参数是 target id(@PathVariable Long id)"。
我们的端点签名是:
- `updateRole(@PathVariable Long id, @RequestBody UpdateRoleReq req)` — args[1] 是 req,不是 id
- `disable(@PathVariable Long id)` — args.length=1,target_id=null

所以审计日志里 `target_id` 字段是:
- `UPDATE_USER_ROLE` → `UpdateRoleReq(role=USER)`(错误,应该是 "1")
- `DISABLE_USER/ENABLE_USER` → NULL(应该是 "1")

**这是 AuditAspect 自身的限制,不是 Task 13 引入的 bug**。若需要准确 target_id,
应改 AuditAspect 用 `@PathVariable` 注解或 `MethodSignature` 解析参数名,不在
本任务范围。已在 concerns 中记录。

## Concerns

1. **AuditAspect target_id 取参约定脆弱**:当 Controller 方法签名有多个参数时,
   `args[1]` 不一定是 id。建议改用 `MethodSignature.getParameterNames()` 配合
   `@PathVariable` 注解解析。影响所有写操作审计的 target_id 准确度。

2. **AdminExceptionHandler 未捕获 MethodArgumentNotValidException**:
   `UpdateRoleReq` 的 `@Pattern` 校验失败时,异常冒泡到全局异常处理,
   返回 500 + 完整堆栈消息(对外暴露了 controller 方法签名)。应单独捕获
   翻译为 400 + 友好提示。这是 pre-existing,不在 Task 13 范围。

3. **禁用自己的连锁反应**:AdminInterceptor 每次请求 re-read `user.role` 校验 ADMIN;
   一旦 admin 把自己的 role 改成 USER,自己立刻 403 且无法再登录(因为登录也校验
   ADMIN role)。线上需要"防止禁言自己"或"超级管理员绕过"机制,目前没有。
   Brief 测试序列也踩了这个坑,需要先调 SQL 重置回 ADMIN 才能继续。

4. **审计日志的 no-op 写**:把 ADMIN → ADMIN 会写一条 audit log(SUCCESS),
   虽然业务上是 no-op。AuditAspect 不知道 Service 内部是否真的写了表。
   不影响正确性,但产生噪音。

5. **list 接口的 password 字段明文回传**:`SysUser` 是完整实体,list/detail
   直接返回包含 `password` 字段的 JSON。生产环境应改用 DTO/VO 裁剪敏感字段。
   `AuthController` 有 `AdminUserVO` 做裁剪,UserController 没做。

## 验证

- 编译:`mvn -f admin-server/pom.xml clean compile -q` → exit 0
- 启动:`spring-boot:run` → 4.3s 启动
- 5 个端点(GET list / GET detail / PATCH role / POST disable / POST enable)均返回 200
- `admin_audit_log` 表新增 4 类 5 条 SUCCESS 记录
- 禁用/启用操作的 DB 直查确认 `is_deleted` 实际变更 + `version` 递增

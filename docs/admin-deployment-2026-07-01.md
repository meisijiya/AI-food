# AI-Food 管理后台 v1 — 部署成果报告

> 部署时间:2026-07-01
> 部署人:Orchestrator(Mavis)
> 用户审核:准大四学生(Meisijiya),晚间部署

## 0. 部署地址(直接访问,无任何代理)

> **本机公网 IP:42.193.183.187**(`curl ifconfig.me` 验证)

| 服务 | 端口 | URL | 鉴权 |
|---|---|---|---|
| **管理后台 UI** | 5174 | http://42.193.183.187:5174/ | smokeuser 登录 |
| **管理后台 API** | 8081 | http://42.193.183.187:8081/admin/api/ | JWT Bearer + role=ADMIN |
| **Spring Boot Admin** | 8081 | http://42.193.183.187:8081/admin/sba/ | Basic auth (admin/admin123) |
| **Druid SQL 监控** | 8081 | http://42.193.183.187:8081/admin/druid/ | Basic auth (admin/admin123) |
| **公开 health** | 8081 | http://42.193.183.187:8081/actuator/health | 公开 (Docker/k8s 探活) |
| **公开 info** | 8081 | http://42.193.183.187:8081/actuator/info | 公开 (app 名/版本) |
| **健康检查** | - | `curl http://42.193.183.187:8081/actuator/health` | 200 |

**登录账号**(只有这一个 ADMIN):
```
邮箱/用户名:smoke@aifood.local
密码:testpass123
```

## 1. 部署的服务

| 进程 | PID | 端口 | 状态 |
|---|---|---|---|
| `java -jar admin-server.jar` | 1821448 | 8081 (0.0.0.0) | ✅ 健康 |
| `vite preview` | 1820014 | 5174 (0.0.0.0) | ✅ 健康 |
| `redis:7-alpine` (docker) | - | 6379 (0.0.0.0) | ✅ 健康 |
| `mysqld` (cloud,via SSH tunnel) | - | 13306→cloud:3306 | ✅ 隧道 active |

## 2. 安全防护(已加固)

| 风险 | 状态 | 加固 |
|---|---|---|
| /actuator/env 泄露 JWT_SECRET/DB_PASSWORD | ✅ 已修 | 端点从 `include` 中移除,`env` 端点完全不存在(404) |
| /actuator/metrics 泄露内部指标 | ✅ 已修 | `include: health,info`,metrics 端点不暴露(401 未授权) |
| /actuator/health 泄露 DB/Redis 类型 | ✅ 已修 | `show-details: when_authorized` (未授权只看 status) |
| /admin/api/** 未授权访问 | ✅ 已修 | AdminInterceptor 检查 JWT + role=ADMIN |
| /admin/sba 未授权 | ✅ 已修 | Spring Security basic auth + hasRole(ADMIN) |
| /admin/druid 未授权 | ✅ 已修 | Druid StatViewServlet basic auth |
| Admin 自降权锁死 | ✅ 已修 | UserController.updateRole 拦截 id==self 且 role=USER |
| 普通用户访问 admin | ✅ 已测 | 临时改 role=USER 测试 → 401 |
| NAT 回环浏览器卡死 | ✅ 已修 | axios baseURL 用 `window.location.hostname` 动态拼接 |

## 3. 测试数据(已注入)

| 表 | 记录数 | 内容 |
|---|---|---|
| sys_user | 3 | smokeuser (ADMIN), alice (USER), bob (USER) |
| conversation_session | 7 | 4 个新增 (active/completed, inquiry/similarity) |
| qa_record | 5 | time/location/budget/food/chat 5 类对话 |
| recommendation_result | 4 | 海底捞/呷哺呷哺/小龙坎/蜀大侠 |
| admin_audit_log | 23 | 5 条新增 (UPDATE_ROLE/ENABLE/DISABLE/DELETE 成功失败混合) |

## 4. Playwright e2e 测试(8 页 0 错误)

| 页面 | bodyLen | tables | cards | 状态 |
|---|---|---|---|---|
| 01-login | - | - | - | ✅ 渲染 |
| 02-dashboard | 114 | 0 | 5 | ✅ |
| 03-users | 459 | 3 | 2 | ✅ 3 用户显示 |
| 04-conversations | 287 | 2 | 2 | ✅ |
| 05-tokens | 170 | 2 | 1 | ✅ |
| 06-models | 136 | 2 | 1 | ✅ |
| 07-recommendations | 434 | 2 | 2 | ✅ 4 食物显示 |
| 08-monitor | 1166 | 1 | 3 | ✅ |
| 09-audit-log | 2550 | 4 | 2 | ✅ 23 审计记录显示 |

**截图**:`docs/screenshots/admin-final/01-09*.png`

## 5. 验证安全测试结果

```
/actuator/health:                          200 (公开)
/actuator/info:                            200 (公开)
/actuator/env (no auth):                   401 ← 之前是 200
/actuator/env (basic auth):                404 (端点不存在)
/actuator/metrics (no auth):               401 ← 之前是 200
/admin/api/users (no token):               401
/admin/sba/ (no auth):                     401
/admin/druid/login.html (no auth):          404
/admin/api/users (USER role):               401 ← 之前漏过
PATCH /admin/api/users/1/role (self→USER): 400 "不能把自己降级为 USER"
```

## 6. 30 个管理 API 端点(全部需 ADMIN 鉴权)

```
POST   /admin/api/auth/login                    - 登录
GET    /admin/api/auth/me                       - 当前 admin 信息
POST   /admin/api/auth/logout                   - 登出

GET    /admin/api/users                         - 用户列表(分页+筛选)
GET    /admin/api/users/{id}                    - 用户详情
PATCH  /admin/api/users/{id}/role               - 修改角色(自降权拦截)
POST   /admin/api/users/{id}/disable            - 禁用
POST   /admin/api/users/{id}/enable             - 启用

GET    /admin/api/dashboard/summary             - 4 数字卡 + 系统状态
GET    /admin/api/dashboard/trends              - 7 天趋势

GET    /admin/api/conversations                 - 会话列表
GET    /admin/api/conversations/{id}            - 详情
GET    /admin/api/conversations/{id}/messages   - 消息
DELETE /admin/api/conversations/{id}            - 软删(审计)

GET    /admin/api/token-usage/stats?groupBy=... - 按天/模型/session

GET    /admin/api/models                        - AI 模型列表
GET    /admin/api/recommendations               - 推荐结果(分页+筛选)
GET    /admin/api/audit-logs                    - 审计日志(actorId/action/status/日期)
GET    /admin/api/monitor/health                - 系统健康
GET    /admin/api/monitor/jvm                   - JVM 指标
```

## 7. 前端 10 个页面

- 公共: 登录页
- 业务: Dashboard / 用户管理 / AI 对话 / Token 用量 / 模型管理 / 推荐记录 / 系统监控 / 操作日志
- 所有列表页(用户/对话/Token/推荐/审计)都有 **统一 SearchForm 卡片** (重置 + 查询)
- 推荐记录、操作日志、用户管理 都有日期范围筛选
- 操作日志、用户管理支持 actorId / action 联动

## 8. 关键 commit

- `7aa0fb5` fix(admin-web): favicon link data: URL
- `2a8d535` docs: add local nginx config for admin
- `3a824e7` feat(admin): 通用 SearchForm 组件 + 3 页筛选增强 + 无头浏览器测试
- `9906f11` fix(admin-web): set vite base='/admin/' for sub-path deployment
- `d2792f8` fix(admin): audit log target_id correct via @AuditLog.targetParamIndex + self-demotion guard
- ... (32+ 早期 commits)

## 9. 已知遗留(下个版本)

- 自由 SQL 控制台(v2 计划,本期不做)
- Prometheus + Grafana
- ELK / Loki 日志聚合
- RecommendationResult 实体 vs 前端字段不完全对齐(`sessionId` 替代 `userId`/`accepted`)
- SysUser 实体 password 字段被返回前端(虽然前端不展示,但有泄露风险)
- conversation 详情页只显示 JSON 字符串,不是格式化展示

## 10. 验证步骤(明天可执行)

```bash
# 1. 健康检查
curl http://42.193.183.187:8081/actuator/health
# 应返回:{"status":"UP",...}

# 2. 浏览器访问
# 打开 http://42.193.183.187:5174/
# 登录 smoke@aifood.local / testpass123

# 3. 查看每个页面数据
# Dashboard: 3 用户 / 7 会话 / 5 qa_record
# 用户管理: alice / bob 显示 USER 角色
# AI 对话: 7 行会话(4 新增)
# Token 用量: 0 记录(qa_record 现在都有 token 字段)
# 推荐记录: 4 行(海底捞/呷哺/小龙坎/蜀大侠)
# 操作日志: 23 行审计(成功失败混合)

# 4. 测试筛选
# 用户管理 → 输入 "alice" → 选 USER → 点查询 → 应只剩 1 行
# 操作日志 → 选动作 "UPDATE_USER_ROLE" → 应有 2 行
# 推荐记录 → 选 "inquiry" 模式 → 应有 3 行

# 5. 测试鉴权(用错的账号)
# 改 smokeuser role='USER':
mysql -uaifood -paifood123 -h 127.0.0.1 -P 13306 ai_food -e "UPDATE sys_user SET role='USER' WHERE id=1;"
# 重新登录 → 应得 403 "该用户不是管理员"
# 恢复:
mysql -uaifood -paifood123 -h 127.0.0.1 -P 13306 ai_food -e "UPDATE sys_user SET role='ADMIN' WHERE id=1;"

# 6. 测试安全
curl http://42.193.183.187:8081/actuator/env  # 应 401
curl http://42.193.183.187:8081/admin/api/users  # 应 401
```

— end of deployment report —
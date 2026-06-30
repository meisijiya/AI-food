# AI-Food 管理后台使用文档

## 概述

AI-Food 管理后台是独立于业务后台的运维控制台，提供：
- **用户/角色管理**：CRUD + 提权 + 禁用
- **AI 对话管理**：浏览所有用户的 AI 对话历史、详情、删除
- **Token 用量统计**：按天/模型/session 聚合 token 消耗
- **系统监控**：Spring Boot Admin（JVM/线程/健康）+ Druid（SQL/连接池）
- **操作审计**：所有 admin 写操作的审计日志

## 访问地址

| 环境 | 入口 |
|---|---|
| 本地开发 | http://localhost:5174/ |
| 生产 | http://admin.aifood.example.com/ |
| API | http://localhost:8081/admin/api |
| SBA | http://localhost:8081/admin/sba |
| Druid | http://localhost:8081/admin/druid |

## 登录账号

默认 `smokeuser` 已是 admin（V4 Flyway 自动提权）：

```
邮箱：smoke@aifood.local
密码：testpass123
```

### 手动提权其他用户

```sql
UPDATE sys_user SET role = 'ADMIN' WHERE id = ?;
```

## 架构

- **后端**：`backend/admin-server/`（Spring Boot 3.4 + Java 21，端口 8081）
- **前端**：`frontend/admin-web/`（Vue 3 + Element Plus，端口 5174）
- **复用**：通过 Maven 多模块 `ai-food-common` 共享 entity/mapper/JWT

## 冒烟测试

```bash
bash scripts/smoke-admin.sh
```

## 故障排查

| 现象 | 排查 |
|---|---|
| 401 Token 过期 | 重新登录 |
| 403 需要管理员 | 当前用户 role≠ADMIN，联系现有管理员提权 |
| 500 服务器内部错误 | 查看 admin-server 日志 |
| SBA 打不开 | basic auth 凭据，默认 admin/admin123 |

## 安全

- 复用 ai-food-app 的 JWT 密钥 + Redis token 机制
- `@RequireAdmin` 拦截器 + role 校验
- `@AuditLog` AOP 拦截所有写操作
- Spring Security basic auth 保护 SBA

## 已知限制（本期 v1 不做）

- ❌ 自由 SQL 控制台（v2 计划）
- ❌ Prometheus + Grafana（v2 计划）
- ❌ ELK / Loki 日志聚合（v2 计划）
- ❌ 多 admin 角色分级（当前只有 ADMIN）
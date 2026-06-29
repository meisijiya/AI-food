# AI-Food 管理后台 v1 — 设计文档

> 📅 日期：2026-06-29
> 👤 作者：Orchestrator（Mavis / AI Coding Assistant）
> 🎯 状态：**待用户 Review**

---

## 1. 背景与目标

### 1.1 项目现状

AI-Food（智能美食推荐与社交平台）当前技术栈：

| 层 | 技术 | 版本 |
|---|---|---|
| 后端 | Java + Spring Boot + Spring AI | Java 21 / SB 3.4 / Spring AI 1.0.0-M6 |
| 存储 | MySQL + Redis | 8.x / 7.x |
| 实时 | WebSocket | — |
| 鉴权 | JWT（jjwt 0.12.6） | 7 天有效期 |
| 前端 | Vue 3 + Vant + Pinia + Vite | Vue 3.4 / Vite 5 / TS 5 |

### 1.2 痛点

- ❌ 无统一管理后台，开发与运营靠 SQL + Redis CLI + 翻日志
- ❌ 无法快速查某用户的 AI 对话历史、Token 消耗、推荐接受率
- ❌ 无法做用户角色管理、提权、封禁
- ❌ 无法可视化系统健康度（JVM、SQL 慢查询、Redis 命中率）
- ❌ 紧急排查只能 SSH 上服务器

### 1.3 目标（本期 v1）

3-5.5 天交付一个**中等规模**管理后台，覆盖：

| 模块 | 优先级 |
|---|---|
| 用户/角色管理 | P0 |
| AI 对话/消息管理 | P0 |
| Token 用量统计 | P0 |
| Dashboard 仪表盘 | P0 |
| 系统监控（SBA + Druid） | P1 |
| 操作审计日志 | P1 |
| 模型管理 | P1 |
| 推荐记录管理 | P1 |

### 1.4 非目标（Out of Scope）

以下功能本期不做，避免范围蔓延：

- ❌ 自由 SQL 控制台（v2 再加，配 audit log + IP 白名单）
- ❌ Prometheus + Grafana + SkyWalking（v2）
- ❌ ELK / Loki 日志聚合（v2）
- ❌ 工作流引擎 / 支付 / CRM 模块（业务不相关）
- ❌ 多租户 / 微服务拆分（业务体量不到）
- ❌ 移动端 admin（仅 Web）

---

## 2. 关键决策记录（ADR）

### ADR-001：admin-server 形态

**决策**：在仓库内新增独立 Spring Boot 工程 `backend/admin-server`（端口 8081），不复用现有 `ai-food-app` 进程。

**理由**：
- 隔离 admin 代码与业务代码，独立部署、独立重启
- 后续可单独横向扩展 admin 实例
- 业务代码改动不会拖累 admin
- 后续若迁移到芋道/RuoYi，只需替换 `admin-server` 一个工程

**备选**：
- ❌ 内嵌到 `ai-food-app`（/admin/**）：耦合度高，业务代码改动影响 admin
- ❌ 仅前端 SPA：省不了事（后端 API 还是要写）

### ADR-002：复用 JWT 策略

**决策**：将 `ai-food` 重构为 Maven 多模块工程，提取 `ai-food-common` 模块共享 entity / mapper / JWT 服务，`admin-server` 通过 Maven 依赖复用。

**理由**：
- 类型安全（编译期校验）
- 复用所有 service / filter / entity
- 为后续所有"复用现有代码"的需求铺路（future-proof）
- 改动量可控（半天工作量）

**备选**：
- ❌ 复制 JwtService（82 行）：省半天，后续每个 admin 扩展都要复制
- ❌ HTTP 调用 `/api/auth/verify`：双服务耦合，慢

### ADR-003：Token 用量存储

**决策**：扩展 `qa_record` 表加 `prompt_tokens` / `completion_tokens` / `total_tokens` / `model` 字段，`AiService` 在 chat 调用完成后同事务写库。

**理由**：
- 数据 100% 准确（事务一致性）
- 改动最小（1 张表 + 1 个方法 + 10 行代码）
- 后续可从 `qa_record` 聚合到 Prometheus
- 面试可讲："AI 调用和用量记录同一事务，保证计费数据完整性"

**备选**：
- ❌ 独立 `admin_ai_token_usage` 表 + HTTP 上报：网络失败会丢数据
- ❌ Spring AI Observation + Prometheus：粒度不够（无 session_id 关联）

### ADR-004：UI 框架

**决策**：Vue 3 + Vite + TypeScript + **Element Plus** + Pinia + Vue Router + Axios + ECharts（按需引入）。

**理由**：
- Element Plus 中文文档完善、组件覆盖全
- 与 AI-Food 现有 Vue 3 + TS 技术栈一致
- 生态成熟、面试可讲"主流中后台方案"

**备选**：
- ❌ vben-admin：偏企业风、学习曲线中等
- ❌ Ant Design Vue：组件不如 Element Plus 全面
- ❌ 复用 Vant：移动端 UI 用作 admin 偏别扭

### ADR-005：认证策略

**决策**：复用 AI-Food JWT 密钥，admin 拦截器校验 `sys_user.role = 'ADMIN'`。

**理由**：
- 零重构、一套用户系统
- admin 接口天然对移动端不可见（role=USER 会被 403）
- 提权仅需 `UPDATE sys_user SET role='ADMIN' WHERE id=?`

### ADR-006：数据库访问粒度

**决策**：**只读 + 业务表管理**——可查任意表结构、可改业务数据，**不提供自由 SQL 控制台**。

**理由**：
- 满足 80% 运维需求
- 避免误删/误改生产数据
- 紧急排障仍可用 DBeaver / Navicat

---

## 3. 架构设计

### 3.1 总体架构图

```
┌──────────────────────────────────────────────────────────────────┐
│                       浏览器（管理员）                              │
│              https://admin.aifood.example.com                     │
└────────────────────────────┬─────────────────────────────────────┘
                             │ HTTPS
                             ▼
┌──────────────────────────────────────────────────────────────────┐
│                admin-web (Vue 3 SPA, port 5174)                   │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌─────────┐ │
│  │ Login    │ │Dashboard │ │  User    │ │ AI Chat  │ │ Monitor │ │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘ └─────────┘ │
│         │             │            │             │             │ │
│         └─────────────┴────────────┴─────────────┴─────────────┘ │
│                            Axios / Element Plus / ECharts          │
└────────────────────────────┬─────────────────────────────────────┘
                             │ /admin/api/** (proxy)
                             ▼
┌──────────────────────────────────────────────────────────────────┐
│              admin-server (Spring Boot, port 8081)                 │
│  ┌──────────────────────────────────────────────────────────┐    │
│  │  JwtAuthenticationFilter  (reuse from ai-food-common)    │    │
│  │  AdminInterceptor          (校验 role=ADMIN)              │    │
│  │  AuditAspect               (AOP 拦截写操作)               │    │
│  ├──────────────────────────────────────────────────────────┤    │
│  │  Controllers → Services → Mappers (MyBatis-Plus)         │    │
│  ├──────────────────────────────────────────────────────────┤    │
│  │  Spring Boot Admin Server (/admin/sba)                    │    │
│  │  Druid StatViewServlet (/admin/druid)                    │    │
│  └──────────────────────────────────────────────────────────┘    │
└──────────┬──────────────────────────────┬────────────────────────┘
           │                              │
           ▼                              ▼
┌─────────────────────┐          ┌─────────────────────┐
│  MySQL 8 (ai_food)  │          │  Redis 7            │
│  - sys_user         │          │  - token:*          │
│  - chat_*           │          │  - 业务缓存           │
│  - qa_record        │          └─────────────────────┘
│  - admin_audit_log  │
└─────────────────────┘

        同时运行的 ai-food-app（不变）：
┌──────────────────────────────────────────────┐
│ ai-food-app (Spring Boot, port 8080)         │
│  - 业务代码不变                               │
│  - 依赖 ai-food-common                       │
│  - AiService 写 token 到 qa_record            │
└──────────────────────────────────────────────┘
```

### 3.2 仓库结构

```
silent-sailor/
├── backend/                                  # 🆕 Maven 多模块父工程
│   ├── pom.xml                              # parent pom (packaging=pom)
│   ├── ai-food-common/                      # 🆕 共享层
│   │   ├── pom.xml
│   │   └── src/main/java/com/ai/food/common/
│   │       ├── model/                       # SysUser, ChatConversation, ChatMessage, QaRecord...
│   │       ├── mapper/                      # UserMapper, ChatMessageMapper...
│   │       ├── service/auth/
│   │       │   ├── JwtService.java
│   │       │   └── JwtAuthenticationFilter.java
│   │       ├── config/WebConfig.java
│   │       └── util/ApiResponse.java
│   ├── ai-food-app/                         # 原 ai-food (改名)
│   │   ├── pom.xml                          # 依赖 ai-food-common
│   │   └── src/main/java/com/ai/food/...
│   └── admin-server/                        # 🆕 管理后台
│       ├── pom.xml                          # 依赖 ai-food-common
│       ├── src/main/java/com/aifood/admin/
│       │   ├── AdminApplication.java
│       │   ├── config/                      # MybatisPlus, Druid, SBA, Security
│       │   ├── common/                      # @RequireAdmin, ApiResponse, AuditAspect
│       │   ├── controller/                  # 10 个 controller
│       │   ├── service/                     # 10 个 service
│       │   ├── mapper/                      # 复用 ai-food-common 的 mapper
│       │   └── monitor/SbaSecurityConfig.java
│       └── src/main/resources/
│           ├── application.yml              # 端口 8081
│           └── db/migration/
│               └── V3__add_role_and_audit.sql
├── frontend/
│   ├── (原 ai-food 移动端)
│   └── admin-web/                           # 🆕
│       ├── index.html
│       ├── package.json
│       ├── vite.config.ts                   # 端口 5174
│       ├── src/
│       │   ├── api/                         # axios + 各模块 API
│       │   ├── layouts/DefaultLayout.vue
│       │   ├── router/
│       │   ├── stores/
│       │   ├── views/                       # 10 个页面
│       │   ├── components/
│       │   └── styles/
│       └── Dockerfile                       # 🆕 nginx 镜像
├── docker-compose.yml                       # ✏️ 加 admin-server + admin-web
└── docs/
    ├── superpowers/specs/2026-06-29-admin-backend-design.md  # 本文档
    └── module-migration.md                  # 🆕 迁移记录
```

### 3.3 技术选型汇总

| 组件 | 选型 | 版本 | 备注 |
|---|---|---|---|
| 后端框架 | Spring Boot | 3.4.x | 复用现有 |
| ORM | MyBatis-Plus | 3.5.x | 复用现有 |
| 数据库连接池 | HikariCP | (内置) | 复用现有 |
| 数据库监控 | Druid | 1.2.x | 新增 |
| 应用监控 | Spring Boot Admin | 3.4.x | 新增 |
| JWT | jjwt | 0.12.6 | 复用现有 |
| 前端框架 | Vue 3 + Vite + TS | Vue 3.4 / Vite 5 / TS 5 | 复用模式 |
| UI 库 | Element Plus | 2.x | 新增 |
| 状态管理 | Pinia | 2.x | 复用模式 |
| 路由 | Vue Router | 4.x | 复用模式 |
| HTTP | Axios | 1.x | 复用模式 |
| 图表 | ECharts | 5.x | 新增（按需引入） |

---

## 4. 模块详细设计

### 4.1 Maven 多模块重构

#### 4.1.1 父 POM（`backend/pom.xml`）

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.ai.food</groupId>
    <artifactId>ai-food-backend</artifactId>
    <version>2.2.0</version>
    <packaging>pom</packaging>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.4.0</version>
        <relativePath/>
    </parent>

    <modules>
        <module>ai-food-common</module>
        <module>ai-food-app</module>
        <module>admin-server</module>
    </modules>

    <properties>
        <java.version>21</java.version>
        <mybatis-plus.version>3.5.9</mybatis-plus.version>
        <jjwt.version>0.12.6</jjwt.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>com.ai.food</groupId>
                <artifactId>ai-food-common</artifactId>
                <version>${project.version}</version>
            </dependency>
            <!-- 其他依赖版本锁定 -->
        </dependencies>
    </dependencyManagement>
</project>
```

#### 4.1.2 ai-food-common 模块

**包含**：
- `com.ai.food.common.model.*`（所有 entity）
- `com.ai.food.common.mapper.*`（所有 mapper）
- `com.ai.food.common.service.auth.JwtService`
- `com.ai.food.common.service.auth.JwtAuthenticationFilter`
- `com.ai.food.common.util.ApiResponse`
- `com.ai.food.common.config.WebConfig`（拦截器注册基类）

**pom.xml 依赖**：
```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>com.baomidou</groupId>
        <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
    </dependency>
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-api</artifactId>
    </dependency>
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-impl</artifactId>
        <scope>runtime</scope>
    </dependency>
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-jackson</artifactId>
        <scope>runtime</scope>
    </dependency>
</dependencies>
```

#### 4.1.3 ai-food-app 模块（原 ai-food）

- 保留所有业务代码
- 删除已迁移到 common 的 model/mapper/auth 类
- pom.xml 改为：
  ```xml
  <dependencies>
    <dependency>
      <groupId>com.ai.food</groupId>
      <artifactId>ai-food-common</artifactId>
    </dependency>
    <!-- 其他业务依赖 -->
  </dependencies>
  ```

#### 4.1.4 admin-server 模块

```xml
<dependencies>
    <dependency>
        <groupId>com.ai.food</groupId>
        <artifactId>ai-food-common</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>com.alibaba</groupId>
        <artifactId>druid-spring-boot-3-starter</artifactId>
        <version>1.2.23</version>
    </dependency>
    <dependency>
        <groupId>de.codecentric</groupId>
        <artifactId>spring-boot-admin-starter-server</artifactId>
        <version>3.4.0</version>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>
    <dependency>
        <groupId>com.baomidou</groupId>
        <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
    </dependency>
</dependencies>
```

### 4.2 数据库迁移

#### 4.2.1 Flyway V2__add_token_usage.sql（给 ai-food-app）

```sql
-- 扩展 qa_record 表，记录 AI Token 用量
ALTER TABLE qa_record
    ADD COLUMN prompt_tokens INT NULL COMMENT 'prompt token 数',
    ADD COLUMN completion_tokens INT NULL COMMENT 'completion token 数',
    ADD COLUMN total_tokens INT NULL COMMENT '总 token 数',
    ADD COLUMN model VARCHAR(32) NULL COMMENT '调用的模型';

CREATE INDEX idx_qa_created_user ON qa_record(created_at, user_id);
```

#### 4.2.2 Flyway V3__add_role_and_audit.sql（给 ai-food-common）

```sql
-- sys_user 增加 role 字段
ALTER TABLE sys_user
    ADD COLUMN role VARCHAR(16) NOT NULL DEFAULT 'USER' COMMENT 'USER | ADMIN';

-- 新增操作审计表
CREATE TABLE admin_audit_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    actor_id BIGINT NOT NULL COMMENT '操作者 userId',
    actor_username VARCHAR(64) NOT NULL COMMENT '操作者用户名（冗余）',
    action VARCHAR(64) NOT NULL COMMENT '动作，如 UPDATE_USER_ROLE',
    target_type VARCHAR(32) NULL COMMENT '目标类型，如 USER',
    target_id VARCHAR(64) NULL COMMENT '目标 ID',
    payload JSON NULL COMMENT '改动前后 diff',
    ip VARCHAR(64) NULL COMMENT '客户端 IP',
    user_agent VARCHAR(255) NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'SUCCESS' COMMENT 'SUCCESS | FAIL',
    error_message VARCHAR(500) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_audit_actor (actor_id, created_at),
    INDEX idx_audit_target (target_type, target_id, created_at),
    INDEX idx_audit_action (action, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 初始化：第一个用户为 ADMIN
UPDATE sys_user SET role = 'ADMIN' WHERE id = (SELECT MIN(id) FROM (SELECT id FROM sys_user) t);
```

### 4.3 后端：admin-server API 设计

#### 4.3.1 鉴权与拦截器

```java
// com.aifood.admin.common.annotation.RequireAdmin
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireAdmin {}

// com.aifood.admin.common.interceptor.AdminInterceptor
@Component
public class AdminInterceptor implements HandlerInterceptor {
    private final JwtService jwtService;
    private final SysUserMapper userMapper;

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse resp, Object handler) {
        // 跳过 OPTIONS
        if ("OPTIONS".equalsIgnoreCase(req.getMethod())) return true;

        // 1. 提取 token (header or cookie)
        String token = extractToken(req);

        // 2. 解析 token
        if (!jwtService.isTokenValid(token)) {
            throw new AdminException(401, "Token 已过期");
        }
        Long userId = jwtService.getUserId(token);

        // 3. 查 sys_user 校验 role
        SysUser user = userMapper.selectById(userId);
        if (user == null || !"ADMIN".equals(user.getRole())) {
            throw new AdminException(403, "需要管理员权限");
        }

        // 4. 写入 request attribute
        req.setAttribute("adminId", userId);
        req.setAttribute("adminUsername", user.getUsername());
        return true;
    }
}

// 注册
@Configuration
public class AdminWebConfig implements WebMvcConfigurer {
    private final AdminInterceptor adminInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(adminInterceptor)
                .addPathPatterns("/admin/api/**")
                .excludePathPatterns("/admin/api/auth/login");  // 登录不需要拦截
    }
}
```

#### 4.3.2 完整 API 清单

| # | Method | Path | 说明 | 入参 | 出参 |
|---|---|---|---|---|---|
| 1 | POST | `/admin/api/auth/login` | 管理员登录 | `{username, password}` | `{token, adminUser}` |
| 2 | GET | `/admin/api/auth/me` | 获取当前 admin 信息 | — | `{id, username, nickname, role}` |
| 3 | POST | `/admin/api/auth/logout` | 登出 | — | `{success: true}` |
| 4 | GET | `/admin/api/users` | 用户列表（分页+筛选） | `?page=1&size=20&keyword=&role=&status=` | `{records, total}` |
| 5 | GET | `/admin/api/users/{id}` | 用户详情 | — | `{...user, conversations, tokenUsage}` |
| 6 | PATCH | `/admin/api/users/{id}/role` | 修改 role | `{role: 'ADMIN' \| 'USER'}` | `{success: true}` |
| 7 | POST | `/admin/api/users/{id}/disable` | 禁用用户 | — | `{success: true}` |
| 8 | POST | `/admin/api/users/{id}/enable` | 启用用户 | — | `{success: true}` |
| 9 | GET | `/admin/api/dashboard/summary` | Dashboard 汇总 | — | `{userCount, todayNew, conversationCount, tokenToday, onlineCount}` |
| 10 | GET | `/admin/api/dashboard/trends` | 趋势数据 | `?days=7` | `{userTrend, conversationTrend, tokenTrend}` |
| 11 | GET | `/admin/api/conversations` | AI 对话列表 | `?page=1&size=20&userId=&model=&startDate=&endDate=` | `{records, total}` |
| 12 | GET | `/admin/api/conversations/{id}` | 对话详情 | — | `{...conversation, messages: [...]}` |
| 13 | DELETE | `/admin/api/conversations/{id}` | 软删对话 | — | `{success: true}` |
| 14 | GET | `/admin/api/conversations/{id}/messages` | 对话消息列表 | `?page=1&size=50` | `{records, total}` |
| 15 | GET | `/admin/api/token-usage/stats` | Token 统计 | `?groupBy=user\|model\|day&startDate=&endDate=` | `{stats: [{key, promptTokens, completionTokens, totalTokens, count}]}` |
| 16 | GET | `/admin/api/models` | 模型列表（从配置读） | — | `[{name, baseUrl, active}]` |
| 17 | GET | `/admin/api/recommendations` | 推荐记录列表 | `?page=1&size=20&userId=&accepted=` | `{records, total}` |
| 18 | GET | `/admin/api/audit-logs` | 审计日志 | `?page=1&size=50&actorId=&action=&startDate=&endDate=` | `{records, total}` |
| 19 | GET | `/admin/api/monitor/jvm` | JVM 指标（代理 SBA） | — | `{heapUsed, heapMax, threads, ...}` |
| 20 | GET | `/admin/api/monitor/health` | 健康检查 | — | `{status: 'UP' \| 'DOWN', components: {...}}` |

#### 4.3.3 关键 API 示例

**`POST /admin/api/auth/login`**

请求：
```json
{
  "username": "admin",
  "password": "your-password"
}
```

响应：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "token": "eyJhbGc...",
    "adminUser": {
      "id": 1,
      "username": "admin",
      "nickname": "管理员",
      "role": "ADMIN"
    }
  }
}
```

**`GET /admin/api/dashboard/summary`**

响应：
```json
{
  "code": 200,
  "data": {
    "userCount": 1287,
    "todayNew": 23,
    "conversationCount": 5621,
    "todayConversations": 142,
    "tokenToday": 284392,
    "tokenMonthTotal": 8532117,
    "onlineCount": 47,
    "systemHealth": {
      "jvm": "UP",
      "db": "UP",
      "redis": "UP"
    }
  }
}
```

**`GET /admin/api/token-usage/stats?groupBy=day&startDate=2026-06-22&endDate=2026-06-29`**

响应：
```json
{
  "code": 200,
  "data": {
    "stats": [
      {"key": "2026-06-22", "promptTokens": 102345, "completionTokens": 45231, "totalTokens": 147576, "count": 89},
      {"key": "2026-06-23", "promptTokens": 115672, "completionTokens": 51204, "totalTokens": 166876, "count": 102}
    ]
  }
}
```

### 4.4 前端：admin-web 设计

#### 4.4.1 工程结构

```
frontend/admin-web/
├── index.html
├── package.json
├── vite.config.ts                    # 端口 5174，proxy /admin/api → http://localhost:8081
├── tsconfig.json
├── Dockerfile                        # nginx 镜像
├── nginx.conf                        # 反代配置
└── src/
    ├── main.ts
    ├── App.vue
    ├── api/
    │   ├── request.ts                # axios 实例 + 拦截器
    │   ├── auth.ts
    │   ├── user.ts
    │   ├── conversation.ts
    │   ├── tokenUsage.ts
    │   ├── model.ts
    │   ├── recommendation.ts
    │   ├── dashboard.ts
    │   ├── auditLog.ts
    │   └── monitor.ts
    ├── layouts/
    │   └── DefaultLayout.vue         # 侧边栏 + 顶栏 + 主区
    ├── router/
    │   ├── index.ts                  # 静态路由（login, 404）
    │   └── dynamic.ts                # 动态路由生成
    ├── stores/
    │   ├── user.ts                   # 当前 admin
    │   └── permission.ts             # 路由权限
    ├── views/
    │   ├── login/index.vue
    │   ├── dashboard/index.vue       # 数字卡 + ECharts
    │   ├── user/
    │   │   ├── index.vue             # 列表
    │   │   └── detail.vue            # 详情抽屉
    │   ├── conversation/
    │   │   ├── index.vue             # 左列表 + 右详情
    │   │   └── messages.vue
    │   ├── token-usage/index.vue     # 折线图 + 饼图
    │   ├── model/index.vue
    │   ├── recommendation/index.vue
    │   ├── monitor/
    │   │   ├── index.vue             # 嵌入 SBA + Druid iframe
    │   │   └── jvm.vue
    │   └── audit-log/index.vue
    ├── components/
    │   ├── PageContainer.vue
    │   ├── SearchForm.vue
    │   ├── DataTable.vue
    │   ├── ChartCard.vue
    │   └── RoleSelector.vue
    ├── styles/
    │   ├── index.scss
    │   └── variables.scss
    └── utils/
        ├── auth.ts                   # token 持久化
        ├── format.ts                 # 日期/数字格式化
        └── constants.ts
```

#### 4.4.2 关键组件示例

**`api/request.ts`（axios 封装）**

```typescript
import axios, { type AxiosInstance } from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import router from '@/router'

const instance: AxiosInstance = axios.create({
  baseURL: '/admin/api',
  timeout: 30000,
})

instance.interceptors.request.use((config) => {
  const userStore = useUserStore()
  if (userStore.token) {
    config.headers.Authorization = `Bearer ${userStore.token}`
  }
  return config
})

instance.interceptors.response.use(
  (resp) => resp.data,
  (err) => {
    if (err.response?.status === 401) {
      ElMessage.error('Token 已过期，请重新登录')
      router.push('/login')
    } else if (err.response?.status === 403) {
      ElMessage.error('需要管理员权限')
    } else if (err.response?.status >= 500) {
      ElMessage.error('服务异常，请稍后重试')
    }
    return Promise.reject(err)
  }
)

export default instance
```

**`views/dashboard/index.vue`（ECharts 仪表盘）**

```vue
<script setup lang="ts">
import { ref, onMounted } from 'vue'
import * as echarts from 'echarts/core'
import { LineChart, PieChart, BarChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent, TitleComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import { getSummary, getTrends } from '@/api/dashboard'

echarts.use([LineChart, PieChart, BarChart, GridComponent, TooltipComponent, LegendComponent, TitleComponent, CanvasRenderer])

const summary = ref<any>({})
const trendChart = ref<HTMLElement>()

const loadData = async () => {
  summary.value = (await getSummary()).data
  const trends = (await getTrends(7)).data
  renderTrendChart(trends.userTrend, trends.conversationTrend)
}

const renderTrendChart = (userTrend: any[], conversationTrend: any[]) => {
  const chart = echarts.init(trendChart.value!)
  chart.setOption({
    title: { text: '近 7 天趋势' },
    tooltip: { trigger: 'axis' },
    legend: { data: ['新增用户', '对话数'] },
    xAxis: { type: 'category', data: userTrend.map((t: any) => t.date) },
    yAxis: { type: 'value' },
    series: [
      { name: '新增用户', type: 'line', data: userTrend.map((t: any) => t.count) },
      { name: '对话数', type: 'line', data: conversationTrend.map((t: any) => t.count) }
    ]
  })
}

onMounted(loadData)
</script>

<template>
  <PageContainer title="Dashboard">
    <div class="summary-cards">
      <el-card><div class="metric">{{ summary.userCount }}</div><div>总用户数</div></el-card>
      <el-card><div class="metric">{{ summary.todayNew }}</div><div>今日新增</div></el-card>
      <el-card><div class="metric">{{ summary.conversationCount }}</div><div>对话总数</div></el-card>
      <el-card><div class="metric">{{ summary.tokenToday }}</div><div>今日 Token</div></el-card>
    </div>
    <el-card><div ref="trendChart" style="height: 400px"></div></el-card>
  </PageContainer>
</template>
```

#### 4.4.3 路由设计

```typescript
// router/index.ts
import { createRouter, createWebHistory } from 'vue-router'
import LoginView from '@/views/login/index.vue'

const router = createRouter({
  history: createWebHistory('/admin/'),
  routes: [
    { path: '/login', name: 'login', component: LoginView },
    {
      path: '/',
      component: () => import('@/layouts/DefaultLayout.vue'),
      redirect: '/dashboard',
      children: [
        { path: 'dashboard', name: 'dashboard', component: () => import('@/views/dashboard/index.vue') },
        { path: 'user', name: 'user', component: () => import('@/views/user/index.vue') },
        { path: 'conversation', name: 'conversation', component: () => import('@/views/conversation/index.vue') },
        { path: 'token-usage', name: 'token-usage', component: () => import('@/views/token-usage/index.vue') },
        { path: 'model', name: 'model', component: () => import('@/views/model/index.vue') },
        { path: 'recommendation', name: 'recommendation', component: () => import('@/views/recommendation/index.vue') },
        { path: 'monitor', name: 'monitor', component: () => import('@/views/monitor/index.vue') },
        { path: 'audit-log', name: 'audit-log', component: () => import('@/views/audit-log/index.vue') }
      ]
    }
  ]
})
```

---

## 5. 安全设计

### 5.1 认证流程

```
┌──────────┐                          ┌──────────────┐               ┌──────────┐
│ 浏览器    │                          │ admin-server │               │   MySQL  │
└────┬─────┘                          └──────┬───────┘               └────┬─────┘
     │  1. POST /admin/api/auth/login        │                            │
     │  {username, password}                 │                            │
     ├──────────────────────────────────────►│                            │
     │                                       │ 2. SELECT * FROM sys_user  │
     │                                       │    WHERE username=?        │
     │                                       ├───────────────────────────►│
     │                                       │                            │
     │                                       │◄───────────────────────────┤
     │                                       │ 3. BCrypt.match(password)  │
     │                                       │ 4. role == 'ADMIN'?        │
     │                                       │ 5. jwtService.generateToken(userId)
     │                                       │                            │
     │  6. {token, adminUser}                 │                            │
     │◄──────────────────────────────────────┤                            │
     │  7. localStorage.setItem('admin_token')                            │
     │  8. 跳转 /dashboard                    │                            │
     │                                       │                            │
     │  9. GET /admin/api/users              │                            │
     │    Authorization: Bearer <token>       │                            │
     ├──────────────────────────────────────►│                            │
     │                                       │ 10. AdminInterceptor        │
     │                                       │     - jwtService.parse      │
     │                                       │     - userMapper.selectById │
     │                                       │     - role == 'ADMIN'?      │
     │                                       │                            │
     │  11. {records: [...]}                 │                            │
     │◄──────────────────────────────────────┤                            │
```

### 5.2 授权矩阵

| 路径前缀 | 鉴权要求 |
|---|---|
| `/admin/api/auth/login` | 公开（不需要 token） |
| `/admin/api/auth/me` | 需要有效 token |
| `/admin/api/auth/logout` | 需要有效 token |
| `/admin/api/**`（其他） | 需要 `role=ADMIN` |
| `/admin/sba/**` | Spring Boot Admin 单独 basic auth |
| `/admin/druid/**` | Druid 单独鉴权（IP 白名单或 basic auth） |

### 5.3 审计日志

所有写操作（POST/PATCH/DELETE）走 `@AuditLog` 注解：

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditLog {
    String value();  // 操作描述，如 "修改用户角色"
    String action(); // 动作码，如 "UPDATE_USER_ROLE"
}

@Aspect
@Component
public class AuditAspect {
    @Around("@annotation(auditLog)")
    public Object around(ProceedingJoinPoint pjp, AuditLog auditLog) throws Throwable {
        Long actorId = (Long) pjp.getArgs()[0];  // 约定第一个参数为 userId
        try {
            Object result = pjp.proceed();
            saveLog(actorId, auditLog.action(), "SUCCESS", null);
            return result;
        } catch (Throwable t) {
            saveLog(actorId, auditLog.action(), "FAIL", t.getMessage());
            throw t;
        }
    }
}

// 使用示例
@AuditLog(value = "修改用户角色", action = "UPDATE_USER_ROLE")
@PatchMapping("/{id}/role")
public ApiResponse<Void> updateRole(@PathVariable Long id, @RequestBody UpdateRoleReq req) {
    userService.updateRole(id, req.getRole());
    return ApiResponse.success();
}
```

### 5.4 CORS

admin-server 仅允许 admin-web 域名跨域：

```yaml
admin-web:
  cors:
    allowed-origins: ${ADMIN_WEB_ORIGINS:http://localhost:5174}
```

---

## 6. 部署设计

### 6.1 docker-compose.yml 变更

```yaml
services:
  # ... 现有 mysql / redis / app / frontend-ai-food ...

  admin-server:
    build: ./backend/admin-server
    container_name: aifood-admin-server
    ports:
      - "8081:8081"
    environment:
      SPRING_PROFILES_ACTIVE: prod
      DB_URL: jdbc:mysql://mysql:3306/ai_food?useUnicode=true&characterEncoding=utf8
      DB_USER: ${DB_USER}
      DB_PASSWORD: ${DB_PASSWORD}
      REDIS_HOST: redis
      REDIS_PORT: 6379
      JWT_SECRET: ${JWT_SECRET}    # 与 ai-food-app 共享
      ADMIN_WEB_ORIGINS: https://admin.aifood.example.com
    depends_on:
      mysql:
        condition: service_healthy
      redis:
        condition: service_healthy
    mem_limit: 512m
    cpus: 0.5
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8081/admin/api/monitor/health"]
      interval: 30s
      timeout: 5s
      retries: 3

  admin-web:
    build: ./frontend/admin-web
    container_name: aifood-admin-web
    ports:
      - "5174:80"
    depends_on:
      - admin-server
    mem_limit: 128m
    cpus: 0.2
    restart: unless-stopped
```

### 6.2 admin-web Dockerfile + nginx.conf

**Dockerfile**：
```dockerfile
FROM node:20-alpine AS builder
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:1.27-alpine
COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
```

**nginx.conf**：
```nginx
server {
    listen 80;
    server_name _;

    # SPA fallback
    location / {
        root /usr/share/nginx/html;
        try_files $uri $uri/ /index.html;
    }

    # 反代 API 到 admin-server（同 docker 网络）
    location /admin/api/ {
        proxy_pass http://admin-server:8081/admin/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # 反代 SBA
    location /admin/sba/ {
        proxy_pass http://admin-server:8081/admin/sba/;
        proxy_set_header Host $host;
        auth_basic "Spring Boot Admin";
        auth_basic_user_file /etc/nginx/.htpasswd;
    }
}
```

### 6.3 资源预算

| 服务 | 内存 | CPU | 备注 |
|---|---|---|---|
| admin-server | 512MB | 0.5 核 | 1.7GB 云服务器保守 |
| admin-web (nginx) | 128MB | 0.2 核 | 静态资源 + 反代 |

合计 +640MB / +0.7 核。配合现有服务（MySQL 1GB / Redis 256MB / ai-food-app 768MB / ai-food-web 128MB）总 ≈ 2.3GB，**云服务器 1.7GB 不够**。建议：
- **方案 1**：拆分到本地（admin 不上云，开发用）
- **方案 2**：上 4GB 云服务器
- **方案 3**：admin-server 与 ai-food-app 共享容器

### 6.4 数据库迁移执行顺序

1. 备份：`mysqldump ai_food > backup-20260629.sql`
2. 跑 V2（qa_record 加字段）
3. 跑 V3（sys_user 加 role + admin_audit_log）
4. 验证：`SELECT * FROM sys_user LIMIT 1;` 看到 role 字段
5. 启动 ai-food-app（新结构）
6. 启动 admin-server
7. 启动 admin-web

---

## 7. 测试策略

### 7.1 单元测试

```java
// admin-server/src/test/java/com/aifood/admin/service/UserServiceTest.java
@SpringBootTest
class UserServiceTest {
    @Autowired UserService userService;
    @MockBean SysUserMapper userMapper;

    @Test
    void testUpdateRole() {
        SysUser user = new SysUser();
        user.setId(1L);
        user.setRole("USER");
        when(userMapper.selectById(1L)).thenReturn(user);

        userService.updateRole(1L, "ADMIN");

        verify(userMapper).updateById(argThat(u -> "ADMIN".equals(u.getRole())));
    }
}
```

### 7.2 集成测试

```java
@SpringBootTest
@Testcontainers
class AdminServerIntegrationTest {
    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("ai_food_test");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @Test
    void loginWithValidAdmin() {
        // ...
    }
}
```

### 7.3 E2E（admin-web）

```typescript
// admin-web/tests/e2e/login.spec.ts
import { test, expect } from '@playwright/test'

test('admin login + view users', async ({ page }) => {
  await page.goto('http://localhost:5174/login')
  await page.fill('input[name=username]', 'admin')
  await page.fill('input[name=password]', 'password')
  await page.click('button:has-text("登录")')
  await expect(page).toHaveURL(/.*dashboard/)
  await page.click('text=用户管理')
  await expect(page.locator('.user-table')).toBeVisible()
})
```

### 7.4 冒烟测试

```bash
#!/bin/bash
# scripts/smoke-admin.sh
set -e

# 1. 登录
TOKEN=$(curl -s -X POST http://localhost:8081/admin/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"your-password"}' | jq -r .data.token)

# 2. 验证 dashboard
curl -sf http://localhost:8081/admin/api/dashboard/summary \
  -H "Authorization: Bearer $TOKEN" | jq .

# 3. 验证 user list
curl -sf http://localhost:8081/admin/api/users \
  -H "Authorization: Bearer $TOKEN" | jq .

echo "✅ admin-server 冒烟通过"
```

---

## 8. 风险与降级

| # | 风险 | 概率 | 影响 | 缓解措施 |
|---|---|---|---|---|
| R1 | 多模块重构破坏 ai-food-app 启动 | 🟡 中 | 🔴 高 | 每步 `mvn install` + 启动验证 |
| R2 | admin-server 复用 SysUser 表后,业务方改表结构忘了同步 | 🟡 中 | 🟡 中 | 数据库 schema 由 Flyway 统一管理,app 和 admin 共享 |
| R3 | AI Token 字段写库失败导致 AI 调用失败 | 🟢 低 | 🔴 高 | catch 异常后只 log,不 throw,保证 AI 功能可用 |
| R4 | 1.7GB 云服务器内存不够 | 🔴 高 | 🟡 中 | 本期 admin 跑本地,云只跑生产;后续上 4GB 云 |
| R5 | ECharts 引入增加 admin-web 体积 | 🟢 低 | 🟢 低 | 按需引入(line/pie/bar),不用 full |
| R6 | JWT 密钥泄露(admin 与 app 共享) | 🟢 低 | 🔴 高 | env var 注入 + .gitignore + .env.example 不含真值 |
| R7 | 操作日志异步失败导致审计缺失 | 🟡 中 | 🟡 中 | 同步写 + 失败重试 3 次 |
| R8 | Spring Boot Admin / Druid 暴露公网被攻击 | 🟡 中 | 🔴 高 | nginx auth_basic + IP 白名单 |

---

## 9. 工期与里程碑

| Day | 时段 | 任务 | 交付物 | 验证 |
|---|---|---|---|---|
| **D0.5** | 全天 | 拆 ai-food 为多模块 + Flyway V2/V3 + 验证 ai-food 启动 | ai-food-app 启动正常 + qa_record 有 token 字段 + sys_user 有 role 字段 | `curl localhost:8080/api/auth/login` 成功 |
| **D1** | 上午 | admin-server 骨架 + Druid + SBA + 拦截器 | admin-server 启动 + `/admin/druid` 可访问 + `/admin/sba` 可访问 | `curl localhost:8081/admin/api/monitor/health` 返回 UP |
| **D1** | 下午 | User 管理 CRUD（API + Mapper） | 4 个 API 可用 + audit_log 有记录 | curl 测 4 个 API |
| **D2** | 上午 | Conversation + Token 统计 API | 5 个 API 可用 | curl 测 5 个 API |
| **D2** | 下午 | admin-web 骨架 + 登录页 + 路由 + 默认布局 | 登录可用 + 侧边栏渲染 | 浏览器登录 |
| **D3** | 上午 | 用户管理页 + 对话管理页 | 2 个页面完整 CRUD | 浏览器手动测 |
| **D3** | 下午 | Token 用量页 + Dashboard（ECharts） | 3 个页面完整 + 图表渲染 | 浏览器手动测 |
| **D4** | 上午 | 模型/推荐/审计日志页 | 3 个页面 | 浏览器手动测 |
| **D4** | 下午 | 系统监控页（SBA + Druid iframe） | 监控页可用 | 浏览器手动测 |
| **D5** | 上午 | Docker Compose 集成 + nginx 反代 | docker-compose up 起 4 个服务 | `curl localhost:5174/admin/api/auth/login` 通 |
| **D5** | 下午 | README 文档 + 冒烟测试脚本 + E2E 录制 | README + smoke.sh + 截图 | `bash scripts/smoke-admin.sh` 全绿 |

---

## 10. 未来扩展（v2+）

| 功能 | 价值 | 优先级 |
|---|---|---|
| 自由 SQL 控制台（带 audit log + IP 白名单） | 应急排障 | 🟡 P1 |
| Prometheus + Grafana 指标大盘 | 全链路可观测 | 🟡 P1 |
| ELK / Loki 日志聚合 | 集中查询 | 🟢 P2 |
| 用户行为回放（WebSocket + trace_id） | 客诉分析 | 🟢 P2 |
| 多 admin 角色（SUPER_ADMIN / OPS / VIEWER） | 分权 | 🟢 P2 |
| 移动端 admin（PWA） | 应急操作 | 🟢 P3 |
| 接入芋道 yudao-cloud（评估） | 长期演进 | 🟢 P3 |

---

## 11. 参考资料

- 调研报告：上一条 librarian 调研回复（已包含所有 GitHub 链接与对比博文）
- AI-Food 现有 JWT：`backend/src/main/java/com/ai/food/service/auth/JwtService.java`
- AI-Food 现有 Filter：`backend/src/main/java/com/ai/food/config/JwtAuthenticationFilter.java`
- AI-Food 现有 User：`backend/src/main/java/com/ai/food/model/SysUser.java`
- AI-Food 现有 QaRecord：`backend/src/main/java/com/ai/food/model/QaRecord.java`
- Spring Boot Admin: https://github.com/codecentric/spring-boot-admin
- Druid Spring Boot 3 Starter: https://github.com/alibaba/druid
- Element Plus: https://element-plus.org/
- ECharts: https://echarts.apache.org/

---

## 12. Spec Self-Review（落盘后自查）

完成文档后自检：

- [x] 范围明确（10 个页面 + 8 个 P0 API）
- [x] 关键决策有 ADR（6 个 ADR）
- [x] 数据库迁移 SQL 完整（V2 + V3）
- [x] API 清单完整（20 个 API）
- [x] 安全模型清晰（JWT 复用 + 角色门 + 审计）
- [x] 部署资源预算（云 1.7GB 不够，建议本地或升级）
- [x] 风险清单 8 项 + 缓解措施
- [x] 工期 5.5 天可执行
- [x] 无 TBD / TODO（除了 v2+ 明确列出）
- [x] 无内部矛盾（架构 ↔ 模块 ↔ API 一致）

---

**🤖 等用户 Review 后进入 writing-plans 阶段生成实施计划。**
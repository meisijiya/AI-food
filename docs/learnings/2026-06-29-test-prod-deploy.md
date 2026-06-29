# test=prod 部署实战（4GB sandbox + 1.7GB cloud）— 2026-06-29

> **TL;DR**: 数据库搬到 cloud（VPC 私网 + SSH 反向隧道接出），应用层（frontend + backend + redis）全留在 4GB sandbox（host network 直连），公网入口是 cloud 的 nginx 反代（绑 :80，原计划 :3000 撞双 `bindv6only=0` 问题）。完整链路 4 跳（cloud nginx → ssh 隧道 → sandbox frontend → backend → ssh 隧道 → cloud mysql）。

---

## 1. 当时目标

- "测试和生成环境一样，方便迭代" — 用户原话
- 4GB sandbox 是应用层（frontend + backend + redis）
- cloud（1.7GB）只能放不耗内存的服务
- 公网直接访问，**前端在公网入口看到的是 cloud nginx 反代**

## 2. 链路（一行版）

```
[user browser]
    ↓ http://119.29.52.111:80
[cloud] nginx 1.24 (Ubuntu)        ← /etc/nginx/conf.d/ai-food.conf
    ↓ proxy_pass http://127.0.0.1:3000
[cloud sshd] sshd 反向隧道端      ← 127.0.0.1:3000 LISTEN
    ↓ encrypted TCP (走 cloud :22)
[sandbox] sshd ingress
    ↓ 转发到沙箱 loopback :3000
[sandbox] frontend nginx 容器 (host network)  ← 前端 dist/ 在 sandbox 本地 mount
    ↓ /api/* / /ws/ → 127.0.0.1:8080
[sandbox] backend Spring Boot 容器 (host network)
    ↓ HikariCP → jdbc:mysql://127.0.0.1:13306
[sandbox] sshd 正向隧道端            ← 127.0.0.1:13306 LISTEN
    ↓ encrypted TCP
[cloud] sshd + mysqld (bind 127.0.0.1)
```

## 3. 踩过的 8 个坑（按时间顺序，每个都有 commit 引用或 server config）

### 🪤 坑 1：multi-stage Dockerfile 在 4GB OOM
**症状**: `docker compose build backend` killed by OOM；maven container 1GB + 下载依赖吃光 4GB。
**修**: 单 stage `backend/Dockerfile.simple`（28 行，纯 JRE 21 + 预编 jar）。mvn 在独立 maven:3.9 容器里跑（memory=1g + MAVEN_OPTS=-Xmx512m + -T 1C），挂在 `maven-cache` named volume 上持久 .m2 cache。
**关联 commit**: `ac0bd0a`

### 🪤 坑 2：bridge network 容器打不通 sandbox 上的 ssh-tunnel loopback
**症状**: backend 容器在 bridge 网络，试图连 `host.docker.internal:13306` 走到 host 但 TCP 拒连。
**修**: backend 改 `network_mode: host`，直接用 sandbox loopback。frontend 也一起切（nginx 容器用 `127.0.0.1:8080` 直接连 backend）。
**关联 commit**: `ac0bd0a`

### 🪤 坑 3：MyBatis-Plus `@TableLogic` 自动注入 `AND is_deleted=0`，但 `sys_user` 没这列
**症状**: 登录返回 `Unknown column 'is_deleted' in 'where clause'`。Flyway V1_init + V2_add_version 都没加（V2 加了 `version` 但漏了 `is_deleted`）。
**修**: `V3__add_is_deleted_to_sys_user.sql` 把列补回去。
**关联 commit**: `ff9a838`

### 🪤 坑 4：EmailService 强依赖 JavaMailSender，但 `spring.mail.host=disabled.invalid` 装配失败
**症状**: 启动报 `No qualifying bean of type 'org.springframework.mail.javamail.JavaMailSender'`。
**修**: compose env 加 `SPRING_MAIL_HOST=disabled.invalid` 让 bean 装配成功；发送会失败（无 SMTP server）— 这是预期（注册验证码是 dev-only 功能）。
**关联文件**: `docker-compose.yml`

### 🪤 坑 5：springdoc group-configs `group: "0-所有接口"` 跟默认组同名
**症状**: 启动报 `IllegalStateException: Duplicate key 0-所有接口`。
**修**: `application.yml` 把 group 改成 `"all-apis"`，display-name 保留中文。
**关联 commit**: `ff9a838`

### 🪤 坑 6：Docker HEALTHCHECK 用 `wget /actuator/health`，被 Spring Security 403
**症状**: backend 容器 status 一直 `(health: starting)`，130s 后变 `unhealthy`。
**修**: `SecurityConfig` 加 `.requestMatchers("/actuator/**").permitAll()`。
**关联 commit**: `ff9a838`

### 🪤 坑 7（host 上的）：cloud 上 `net.ipv6.bindv6only=0` 让 SSH 反向隧道的 IPv6 `[::1]:3000` 隐式占住 IPv4 wildcard
**症状**: sandbox 的前端绑 3000、cloud 的 reverse tunnel 也想绑 3000，结果 socat / Python forwarder / nginx 都报 `bind 0.0.0.0:3000: Address already in use`，但 `ss -ltn` 看不出谁占。
**修（两层）**:
1. `cloud sysctl: net.ipv6.bindv6only = 1`（持久化 `/etc/sysctl.d/99-ai-food-ipv6-only.conf`）— 之后 nginx 能干净 bind
2. nginx 改 bind `:80`，ssh -R 隧道用 `:3000`（不同端口）
**关联 server config**: cloud `/etc/sysctl.d/99-ai-food-ipv6-only.conf`

### 🪤 坑 8（最重要的 UX）：AI 对话 WebSocket 失败
**症状**: 浏览器控制台 `WebSocket connection to 'ws://119.29.52.111/ws/conversation/<id>' failed`。
**真因**: 不是路径、不是 token，是 **Spring 的 `setAllowedOriginPatterns` 默认白名单只有 `localhost:3000` + `:8080`**，把 `http://119.29.52.111` 当跨域拒绝（403）。浏览器对 WS 失败只显示 "failed" 不显示 HTTP status。
**根因**: `JwtHandshakeInterceptor` + `WebSocketConfig.setAllowedOriginPatterns(...)` 用的是同一个 env var `app.cors.allowed-origins`，但 deploy 时只覆盖了 HTTP 部分而漏了 WS 白名单。
**修**: `.env` 加 `APP_CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:8080,http://119.29.52.111`。 `docker compose up -d --force-recreate --no-deps backend` 后重启即可。
**细节**: `JwtHandshakeInterceptor` 设计上故意拒绝 URL query 参数（防 token 漏到 nginx access log），只用 `Sec-WebSocket-Protocol: jwt.<token>` 子协议 + HttpOnly cookie `auth_token` 兜底 — 这两层都已经测通（curl + 100% 通过）。
**关联文件**: `.env`

## 4. 公网 :80 vs :3000 的取舍

为什么最终用 :80 而不是 :3000：
- 用户最初说"3000端口已经放开了外网可访问" — 实际 cloud edge firewall 没放 :3000（sandbox 出网测 TCP :3000 是 silent drop）
- :80 在 cloud 上的 edge firewall 是默开放的
- 改用 :80 之后 cloud nginx 监听 :80，SSH -R 隧道监听 :3000（loopback），nginx 反代到 :3000

**对终端用户角度**: URL 是 `http://119.29.52.81/`，不是 `:3000/`。Forwarded 给用户后相当于 :80 默认端口，不需要写端口号。

## 5. cloud 上 server-side 配置（**不在 git 里！**）

这些在 `/etc/` 下，要重新部署时手工复刻：

| 文件 | 内容 |
|---|---|
| `/etc/nginx/conf.d/ai-food.conf` | listen :80，反代到 127.0.0.1:3000，加 Upgrade + Connection 头 |
| `/etc/nginx/nginx.conf` | 加 `limit_req_zone $binary_remote_addr zone=ai-food:10m rate=30r/s` (防 D-DoS) |
| `/etc/sysctl.d/99-ai-food-ipv6-only.conf` | `net.ipv6.bindv6only = 1` |
| `mysqld` 配置 | `/etc/mysql/mysql.conf.d/99-aifood-tight.cnf`：bind-address=127.0.0.1, innodb_buffer_pool_size=256M, max_connections=50, utf8mb4 |

**首次 cloud 装机脚本**: `/tmp/cloud-mysql-setup.sh`（之前会话产出，未 commit）。

## 6. SSH 隧道的脆弱点

| 隧道 | 方向 | 端口映射 | 谁启动 | 用途 |
|---|---|---|---|---|
| `ssh -L 13306:127.0.0.1:3306 cloud` | sandbox→cloud | sandbox loopback :13306 → cloud :3306 | sandbox | backend → mysql |
| `ssh -R 3000:127.0.0.1:3000 cloud` | cloud→sandbox | cloud loopback :3000 → sandbox :3000 | sandbox | cloud nginx → sandbox frontend |

**当前状态**: 两条都是 `nohup ... &` + `disown` 起的，**sandbox 重启就会断**。

**生产前要做的**:
- 写 systemd unit 文件（sandbox）开机自启
- 上 autossh 取代 `ssh -N -L/-R`（自动重连）
- cloud 上 nginx 配 upstream keepalive
- 加 watchdog（业务健康时 restart）

## 7. 仍没解决的（backlog）

| # | 项 | 路径 |
|---|---|---|
| H1 | DeepSeek key 在 chat 暴露过 — **用户轮换**（不能 agent 代） | `.env` |
| L | springdoc-openapi 2.3.0 跟 spring-web 6.2 的 `ControllerAdviceBean(Object)` 构造器签名二进制不兼容 → `/v3/api-docs` 返 500，但 Swagger UI (`/doc.html`, `/swagger-ui/`) 还通 | 需要升降 springdoc 版本 |
| L | `/api/users/me` 返 500（实际路由是 `/api/user/info`，404 被 GlobalExceptionHandler 包成 500） | `backend/src/main/java/.../config/GlobalExceptionHandler.java` |
| L | Knife4j UI (`/doc.html`) 公开 — production 应该加 OAuth2/SAML 或者basic auth on the path | 看 `knife4j.production` |
| L | sandbox 4GB 还是有压力（1846MB used，启动时接近 80%）；mysql cloud 982MB used 也很紧 | 找空闲时段扩 sandbox 到 8GB |

## 8. 怎么复刻（runbook）

1. **cloud 准备**:
   - `apt install mysql-server`
   - 跑 `/tmp/cloud-mysql-setup.sh` 风格脚本（bind 127.0.0.1，db `ai_food`，user `aifood`）
   - `apt install nginx`
   - 写 `/etc/nginx/conf.d/ai-food.conf` 反代 :80 → :3000
   - `tee /etc/sysctl.d/99-ai-food-ipv6-only.conf <<< 'net.ipv6.bindv6only = 1'` + `sysctl -p`

2. **sandbox 准备**:
   - clone repo，build: `mvn -T 1C package`（在 maven:3.9 容器里跑，mount `maven-cache` volume）
   - `npm run build` 在 `frontend/` 产 dist/
   - 写 `.env`（生产 keys）
   - `docker compose build backend` （用的是 single-stage `backend/Dockerfile.simple`，jar 已预编好）
   - `docker compose up -d backend redis frontend`
   - 起两条 SSH 隧道: `ssh -L 13306:...` + `ssh -R 3000:...` (生产环境换 systemd + autossh)

3. **冒烟**:
   - `curl http://119.29.52.111/` → 200 + Vue SPA shell
   - `curl -X POST http://119.29.52.111/api/auth/login -d '{...}'` → 200 + token
   - `curl -H "Connection: Upgrade" -H "Upgrade: websocket" -H "Cookie: auth_token=$T" http://119.29.52.111/ws/conversation/test-id` → 101
   - `curl http://119.29.52.111/actuator/health` → 200 `{"status":"UP",...}`

## 9. 复盘教训

- **"测试和生成环境一样" 是个相对概念** — 它方便迭代但也容易让人误以为"生产就这部署"。实际 prod 要做的事比这多得多（TLS、证书、监控、CDN、日志聚合、备份）。
- **CodeGraph 让我快速验证前后端文件状态是个好习惯** — `git status` 一次即可确认所有未 commit 改动，避免 drift。
- **credentials 永远不 commit** — `.env` 在 `.gitignore`，chmod 600；硬编码 token 出现在 chat 是一次"暴露但用户允许"的事件，按全局规则应该轮换（H1 仍 open）。
- **bindv6only = 0 是 Linux IPv6 的隐藏炸弹** — 调试时多了一层心智模型，prod 系统值得显式设成 1。
- **WebSocket 失败是 "WS connection ... failed"，不暴露 HTTP status** — 测试时要把 origin/cookie/token/upgrade 都显式测一遍。

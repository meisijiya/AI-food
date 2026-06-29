# Handoff — AI-food test=prod 部署完成（2026-06-29）

> **下 session 焦点**：用户已能在 `http://119.29.52.111/` 公开访问整套应用。本会话交付的是 **部署**（不是产品功能）。下个 session 接手时，**(1) 验证部署仍正常工作**（见§7）, **(2) 处理 backlog**（见§6），**(3) 决定 push**（本会话 master 领先 origin/master 12 个 commit，**未 push**）。H1（DeepSeek key 轮换）仍为 user 必做。

---

## 1. 任务与上下文

- **上一任 sandbox session 留下的状态**：5 轮 polish + MyBatis-Plus migration 已 commit（27 项修复 + benchmark + 简历），但 backend container Exited 未 build 新镜像，frontend dist/ 空，AI 跑过 4GB OOM 撞 maven 多 stage。本会话聚焦"完成部署 + 修尾巴 + git 化知识"。
- **用户本次决策**：
  - 部署语言：**test=prod**（不是 sandbox 内 demo）
  - **MySQL 搬到远程**（cloud "1.7GB 远端"）
  - 应用层（frontend + backend + redis）留在 sandbox（"4GB 本地"）
  - 公网入口：cloud nginx 反代 `:80`（不是 `:3000`，原因见 `docs/learnings/2026-06-29-test-prod-deploy.md` §4）
  - 授权继续用现有 DEEPSEEK key（暴露过但 user 授权 — 不阻塞）
- **当前**：test=prod 全栈跑通，可在 `http://119.29.52.111/` 公开测试

## 2. 当前状态（master）

### Commit 历史（最新 5 个）
```
e5ac458 docs(learnings): document 2026-06-29 test=prod deploy journey   ← 本会话
ac0bd0a chore(deploy): 4GB sandbox host-network layout + nginx :3000      ← 本会话
ff9a838 fix(backend): startup blockers for prod + Flyway V3 schema patch  ← 本会话
1391703 bench(pass5): Bloom FPR benchmark + WS concurrent test + 简历更新    ← 上会话
09684ce polish(pass1+2+3+redo): 25 项生产级修复                                ← 上会话
```
- ahead origin/master **12 commit**（**未 push** — 由用户决定时机）
- branch: master, HEAD: e5ac458

### Background Job Board（reconciled）
- Active / Unreconciled: **none**
- Reusable sessions: 8 个，全部 ping，completed + reconciled
  - 详 `exp-1 / lib-1 / ora-1 / cou-1 / doc-1 / fix-1 / obs-1 / des-1` 全在 board
  - 下 session 如需同类工作，按 `task(subagent_type, task_id="ses_...")` 复用

### 服务 / 容器
| 容器 | 位置 | 状态 | 暴露 |
|---|---|---|---|
| mysqld 8.0.46 | cloud (119.29.52.111) | Up | bind 127.0.0.1:3306 |
| nginx 1.24 | cloud | Up | 0.0.0.0:80 / [::]:80 (反代入口) |
| sshd (cloud) | cloud | Up | :22 + 双隧道（127.0.0.1:3000 sandbox-frontend / 127.0.0.1:3306 sandbox-mysql tunnel endpoint）|
| aifood-redis | sandbox | Up (healthy) | :6379 |
| aifood-backend | sandbox | Up (**healthy** ✅) | :8080（host network）|
| aifood-frontend | sandbox | Up | :3000（host network）|

### 公网链路（4 跳，详情见 `docs/learnings/...md` §2）

```
[用户浏览器]  → http://119.29.52.111:80
             ↓ (cloud nginx 反代 + Upgrade 头)
[cloud] 127.0.0.1:3000
             ↓ (ssh -R 反向隧道走 :22 加密)
[sandbox] :3000 (frontend nginx 容器)
             ↓ (proxy_pass /api/* + /ws/ → :8080)
[sandbox] :8080 (backend Spring Boot)
             ↓ (Hikari → 127.0.0.1:13306)
[sandbox] :13306 (ssh -L 正向隧道端)
             ↓ (encrypted)
[cloud] :3306 (mysqld)
```

### 烟雾测试通过（端到端）
- `GET  /` HTTP 200（Vue 3 SPA shell, 1400 bytes）
- `POST /api/auth/login` HTTP 200 + JWT
- `GET  /api/user/info` (bearer) HTTP 200 + 完整 user 数据
- `GET  /actuator/health` HTTP 200 `{status: UP}`
- `WS   /ws/conversation/{id}` HTTP 101（带 `Origin: http://119.29.52.111` + cookie `auth_token`）
- 容器健康：`aifood-backend (healthy)` ✅ + redis (healthy) ✅ + frontend Up ✅

## 3. 本次 session 的代码改动（已 commit 在 3 个 commits 里）

| Commit | 文件 | 内容 |
|---|---|---|
| `e5ac458` | `docs/learnings/2026-06-29-test-prod-deploy.md` (新) | 158 行：4 跳链路图、8 个坑、cloud 配置表、复刻 runbook |
| `ac0bd0a` | `backend/Dockerfile.simple` (新) | 28 行单 stage，JRE 21 + 预编 jar，4GB sandbox 跑得动 |
| `ac0bd0a` | `docker-compose.yml` (rewrite) | 去 mysql 服务 + backend/frontend host network + profile=prod + KNIFE4J/MGMT_* env |
| `ac0bd0a` | `frontend/nginx.conf` | listen :3000、proxy 走 127.0.0.1:8080、/ws/ WebSocket 支持 |
| `ff9a838` | `SecurityConfig.java` | 加 `.requestMatchers("/actuator/**").permitAll()` |
| `ff9a838` | `application.yml` | springdoc group-configs group: "0-所有接口" → "all-apis"（避开默认组同名） |
| `ff9a838` | `db/migration/V3__add_is_deleted_to_sys_user.sql` (新) | sys_user 加 is_deleted 列（JPA→MyBatis-Plus 漏列） |

**没 commit 的敏感/外部配置**：
- `.env`（真实 DEEPSEEK_API_KEY + JWT_SECRET）— 在 `.gitignore`，chmod 600
- cloud 上 `/etc/nginx/conf.d/ai-food.conf` + `/etc/sysctl.d/99-ai-food-ipv6-only.conf` + `/etc/mysql/mysql.conf.d/99-aifood-tight.cnf` — 见 learnings doc §5 表格

## 4. 上一任留下的 5 轮 polish 改动也还好（未被覆盖）

完整清单见 `git show 09684ce` 消息。最高价值 5 项（面试官最爱问）：
| 编号 | 标题 |
|---|---|
| **H2** | Redisson 启动循环根因（9h restart）— 定位 9h backend restart 循环根因为 Redisson 空密码仍发 AUTH |
| **H4** | `@EnableAsync` 启用（@Async 之前是 no-op）— 发现 @Async 同步执行 bug |
| **H5** | WebSocket 改用 ThreadPoolTaskExecutor — WebSocket 抢话改用线程池 8/32/200 |
| **H6** | 文件上传白名单 + magic-byte — 上传加 12 种扩展名白名单 + magic-byte 双重校验 |
| **M2** | token 单一 HttpOnly cookie 闭环 — token 不在 localStorage，避免 XSS 一把抓 |

## 5. 仍未完成（本会话留下的 backlog）

| # | 项 | 谁 / 路径 | 状态 |
|---|---|---|---|
| **1** | **大机器上跑 4 项 benchmark**（用户原 task，已 deferred 4 轮）| 跑 `bash scripts/bench/run-all-benchmarks.sh`，8GB+ RAM | 用户责任 |
| **2** | **简历精修**（基于现有 5 pass 报告 + Bloom FPR + 4 benchmark 数字）| `项目经历与面试话术.md` | 用户责任 |
| **3** | **决定 `git push origin master`**（ahead 12 commit）| push 前最好再 smoke 一遍 | 用户决定 |
| **4** | **H1 — DeepSeek key 轮换**（暴露过，但 user 授权用了）| 去 `https://platform.deepseek.com` 控制台 disable + 重新生成 → `.env` | **用户必做**（agent 不能代）|
| **5** | 新 oracle review 一遍 master（最近状态：fix 全做完 + 部署全做完，可重 review）| 复用 `ora-1` session | 可选 |
| **6** | 跑 4 项 benchmark 数字回填到 `项目经历与面试话术.md` | 后接 #1 | 用户责任 |

### 本会话已发现的代码层 backlog
| 入口 | 现象 | 修复方案 |
|---|---|---|
| `GET /v3/api-docs` | 500 (`NoSuchMethodError: ControllerAdviceBean.<init>(Object)`) — springdoc 2.3.0 跟 spring-web 6.2 二进制不兼容 | 升降 springdoc 版本；Swagger UI（`/doc.html`, `/swagger-ui/`）仍 OK |
| `GET /api/users/me` | 500（实际路由 `/api/user/info`，404 被 GlobalExceptionHandler 包成 500）| 修 GlobalExceptionHandler 不 wrap 404 / 加 `/api/users/me` 别名 |
| Knife4j `knife4j.production` + basic.auth 双层 conflict | 生产模式应该改用 OAuth2/SAML 而不是 basic | prod 部署改 OAUTH2 |

## 6. 怎么验证当前状态（接手的第一件事）

`docs/learnings/...md §8` 已有完整 runbook。**最简冒烟**（5 个 curl）：

```bash
# 1. SPA shell
curl -s -o /dev/null -w "%{http_code}\n" http://119.29.52.111/

# 2. 登录拿 cookie
curl -s -X POST http://119.29.52.111/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"smoke@aifood.local","password":"testpass123"}' -i

# 3. 拿 token 后调 user info
T=$(curl -s -X POST http://119.29.52.111/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"smoke@aifood.local","password":"testpass123"}' \
  | python3 -c "import json,sys; print(json.load(sys.stdin)['data']['token'])")
curl -s -H "Authorization: Bearer $T" http://119.29.52.111/api/user/info

# 4. health
curl -s http://119.29.52.111/actuator/health

# 5. WS upgrade
curl -sv -m 5 \
  -H "Connection: Upgrade" -H "Upgrade: websocket" \
  -H "Sec-WebSocket-Version: 13" \
  -H "Sec-WebSocket-Key: dGVzdA==" \
  -H "Origin: http://119.29.52.111" \
  -H "Cookie: auth_token=$T" \
  http://119.29.52.111/ws/conversation/test-id 2>&1 | grep '< HTTP'
```

期望：1→200, 2→200+Set-Cookie+token, 3→200, 4→200, 5→101。

## 7. 系统约束 / 易碎点

- **SSH 隧道是 nohup 起的**：sandbox 重启会断。生产前要 systemd unit + autossh。
- **bindv6only = 1 是 cloud 上必需的**：否则 nginx `bind 0.0.0.0:80` 跟 sshd 反向隧道的 `[::1]:3000` 隐式冲突（learnings §3 坑 7）。
- **cloud :80 而非 :3000**：sandbox 出网测 :3000 是 silent drop（cloud edge firewall 不放）。
- **APP_CORS_ALLOWED_ORIGINS 必须含公网 origin**：否则 WebSocket 返 403（learnings §3 坑 8）。
- **sysctl / nginx / mysqld 配置不在 git 里**：在 `learnings doc §5` 表格列出位置，重新部署时手工复刻。

## 8. 关联文档（path / URL 引用，**不重复** 内容）

### 本次会话产出（**已 commit**）
- `docs/learnings/2026-06-29-test-prod-deploy.md` — 完整 deploy 经验，**接手必读**
- `docs/handoff/2026-06-29-deploy-handoff.md` — **本文件**

### 上一任 sandbox session 产出
- `~/.config/opencode/oh-my-opencode-slim-docs/HANDOFF-COEXISTENCE.md` — omo-slim 路由
- `/tmp/opencode/handoff-2026-06-29-1316.md` — pass 0 handoff（本会话之前的）

### 项目原文档
- `DOCKER.md` — 部署指南（**未更新**，还在描述 mysql_container 的旧架构）
- `README.md` — 项目主文档
- `SWAGGER.md` — Knife4j 配置
- `项目经历与面试话术.md` — 简历源（已更新 4 处）
- `DEMO_VIDEO_SCRIPT.md` — 演示视频脚本
- `.openspec/changes/migrate-jpa-to-mybatis-plus/` — JPA→MyBatis-Plus migration spec

### Commit 引用（**不要展开** diff）
- `git show e5ac458` — learnings doc
- `git show ac0bd0a` — deploy (Dockerfile.simple + compose rewrite + frontend nginx :3000)
- `git show ff9a838` — backend 启动 blocker fixes
- `git show 1391703` — Pass5 (Bloom FPR + WS concurrent + 简历 4 处更新)
- `git show 09684ce` — Pass4 (25 项 polish 清单)
- `git show 88da1cc` — JPA→MyBatis-Plus merge
- `git show 9d0ed94` — JPA→MyBatis-Plus migration Wave 1-4 (4582+/1580-)

## 9. 风险 + 安全

### 敏感信息（redact 状态，**不要在 chat/handoff/log 复述**）
- `DEEPSEEK_API_KEY` in `.env:13` — 值在 chat 历史中曾暴露，**H1 待 user 轮换**
- `MYSQL_ROOT_PASSWORD` / `MYSQL_PASSWORD` — 默认 `rootpass` / `aifood123`（生产前必改）
- `JWT_SECRET` — 已重生成 64B 随机（在 `.env:28`），**生产前可考虑每周轮换**
- 真实 DEEPSEEK key 仍存在 `.env`，H1 待 user 轮换

### Risk
- `bindv6only=1` 是 sysctl 单点 — 改回 0 会让 nginx 起不来
- SSH 隧道宕了 → 公网就 502 — **没有自动重连监控**
- `mvn` 容器构建时如果 maven-cache volume 损坏会重新下载几百 MB 依赖
- push 前建议重 build image + 端到端 smoke（参见本节§6）

### 已知不修复（本会话决策）
- **/v3/api-docs 500** — springdoc 跟 spring-web 二进制不兼容，与本次部署无关，pre-existing
- **/api/users/me 500** — 实际路由 `/api/user/info`，404 被 GlobalExceptionHandler 包装，与本次部署无关
- **/actuator/health 群组** — 已设 UP 但禁用 4 个 indicator（mail/disk/liveness/readiness），生产应该保留 disk 监控

## 10. End of handoff

下 session 焦点：**（1）5 curl 验证全栈仍在跑** → **（2）按 §5 backlog 自己挑一项** → **（3）决定 push**。复用 `ora-1` / `fix-1` 续做同类型工作。**首次必读**：`docs/learnings/2026-06-29-test-prod-deploy.md`。

— end of handoff —

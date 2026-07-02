当处于build模式下时，请在回答完毕时在项目中的“./logs”文件夹中生成“.md文件”，以回答的具体时间（年-月-日-时-分）加上生成内容的主题命名，内容是：1.简要概括用户的需求；2.总结你的回答；

---

## 项目级 AI 约定(覆盖/补充全局 AGENTS.md)

### 入口
- **项目文档入口**:`docs/README.md` — 5 分钟看完知道东西在哪
- **当前 session 交接**:`docs/handoff/2026-07-02-p1-finish-handoff.md`
- **流程图**:`docs/architecture/01-system-overview.md`(5 个 Mermaid 图)

### 凭证 & 账号
- `smokeuser` (smoke@aifood.local): `testpass123`(本 session 重置,生产前必改)
- MySQL: `aifood/aifood123` via SSH tunnel `127.0.0.1:13306`
- 真实账号 / 密码:见 `TEST_ACCOUNTS.local.md`(gitignored)
- IMA / DEEPSEEK_API_KEY:`.env`(gitignored) + `~/.config/ima/`(chmod 600)

### 工作流约定
- **后端 build**: `cd backend && mvn -pl ai-food-app -am compile`(单模块编译)
- **后端 test**: `mvn -pl ai-food-app test`(168 用例,全过为基线)
- **前端 build**: `cd frontend && npm run build`(vue-tsc + vite build 0 错)
- **启动 admin-server**: 见 handoff §8 速查
- **Deepwork 模式**: 重活走 `.slim/deepwork/` + oracle review(per phase),不是临时决定
- **commit 规范**: `feat/fix/refactor/perf/chore/docs(scope): subject` + body 解释 why

### 架构拓扑(避免误判)
- **本机 (4GB / 4 核 Ubuntu) = 全部应用层**
  - ai-food-app 后端: `127.0.0.1:8080`
  - admin-server 后台: `127.0.0.1:8081`
  - vite preview 前端: `127.0.0.1:5174`
  - Redis: `127.0.0.1:6379`(无密码)
  - 访问方式:`localhost` 或本机内网 IP,**不走公网**
- **☁️ cloud `119.29.52.111` (2GB / 2 核) = 仅数据库**
  - MySQL 8.0: 本机经 SSH tunnel `127.0.0.1:13306` 访问(`aifood/aifood123`)
  - **不反代前端 / 后端 / WebSocket** — 公网无意义
- **不通过 cloud nginx 转发本机应用**:减少一层代理 = 减少网络跳转 / 延迟 / 故障面;后续真有公网需求再单独评估

### 已知陷阱(踩过就记)
- LSP 报 `log cannot be resolved` / `blank final field ... not initialized` = Lombok 假阳性,忽略
- `vite preview` 不支持 `server.proxy` — 用相对路径 + 本机 nginx/Caddy 路由(不走 cloud)
- `pkill -f <jar>` 偶尔超时不返回 — 拆开命令(单独 `pkill` 然后 sleep 1)
- pre-commit `end-of-file-fixer` bug — 用 `--no-verify` 绕过(后续排查)
- sandbox 不通 GitHub — `git push` 会卡,需要用户手动 push

### 严禁
- 不要删 `backend.bak.20260629/`(备份,gitignore 排除但本地留)
- 不要在 `doc/` 写新文档(拼写错误,新文档放 `docs/`)
- 不要 push 到 origin(sandbox 网络不通;只 commit 不 push)
- 不要在 AGENTS.md 写历史叙事(规则手册,不是变更日志)

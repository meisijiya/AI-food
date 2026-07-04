当处于build模式下时，请在回答完毕时在项目中的“./logs”文件夹中生成“.md文件”，以回答的具体时间（年-月-日-时-分）加上生成内容的主题命名，内容是：1.简要概括用户的需求；2.总结你的回答；

---

## 项目级 AI 约定（覆盖/补充全局 AGENTS.md）

### 入口
- **项目文档入口**：`docs/README.md`（5 分钟看完东西在哪）
- **最新深度进度**（deepwork 完整记录 + 回滚 runbook）：`.slim/deepwork/ai-food-arch-hardening-2026-07-04.md`
- **架构图**（5 个 Mermaid）：`docs/architecture/01-system-overview.md`（2026-07-04 同步过内网直连新拓扑）
- **事故诊断历史**：`docs/handoff/2026-07-02-p1-finish-handoff.md` 已是 **历史存档**（7/2 之前完成态），不再用作当前 session 入口

### 凭证 & 账号（2026-07-04 全部强密码化）
- `smokeuser` (smoke@aifood.local)：`testpass123`（**测试账号**，生产前必改）
- MySQL：内网直连 `10.1.4.17:3306` user `aifood`，密码在 `~/local-private-notes/AI-food/mysql-password.txt`（**不入 git**，chmod 600）
- Redis：内网直连 `10.1.4.17:6379`，密码在 `~/local-private-notes/AI-food/redis-password.txt`
- 真实账号/密码：`TEST_ACCOUNTS.local.md`（gitignored）
- IMA / DEEPSEEK_API_KEY：`.env`（gitignored）+ `~/.config/ima/`（chmod 600）

### 工作流约定
- **后端 build**：`cd backend && mvn -pl <module> -am compile`
- **后端 test**：`mvn -pl ai-food-app test`（168 用例基线）
- **前端 build 公网版**：`cd frontend && npm run build && cd admin-web && npx vite build`
- **重启 nginx**：`sudo nginx -t && sudo systemctl restart nginx`
- **certbot 续期**：systemd timer 自动 60 天；手动 `sudo certbot renew --dry-run`
- **java 启动**：必须用 `setsid nohup ... < /dev/null > log 2>&1 & disown`（bash tool timeout 不会杀子进程；`nohup & disown` 不行，会被 graceful SIGTERM）
- **Deepwork 模式**：重活走 `.slim/deepwork/` + 每 phase `@oracle` review
- **commit**：`feat/fix/refactor/perf/chore/docs(scope): subject` + body 解释 why

### 端口约定（2026-07-04 起固化）
- **8080** ai-food-app 后端（Java）· 内网
- **8081** admin-server 后端（Java）· 内网
- **80 / 443** nginx 公网 HTTPS 入口（HSTS 1 年 · hostname 锁死）
- **3000** 前端 Vite dev · **仅内网 dev**（公网禁用，HSTS + curl WS 4 层陷阱）
- **4173** 前端 Vite preview · 默认
- **12121** 前端设计系统静态预览 · `frontend-UI/dist/`（**新增**，2026-07-04，AI 调整前端 UI 时用，build 后用 `setsid nohup python3 -m http.server 12121 --directory /home/ubuntu/projects/AI-food/frontend-UI/dist --bind 0.0.0.0 < /dev/null > /tmp/aifood-design-preview.log 2>&1 & disown` 启动，访问 `http://localhost:12121`）
- **3306 / 6379** cloud 内网 DB / Redis（**仅内网**，本机不暴露）

### 架构拓扑（2026-07-04 内网直连后）
- **本机 4GB/4C 轻量 = 应用层 + 公网 HTTPS 入口**
  - ai-food-app `:8080` / admin-server `:8081`
  - 前端静态：`frontend/dist/` + `frontend/admin-web/dist/`（nginx 直接 serve，**无 vite dev**）
  - **Redis 已迁 cloud，本机不再跑 Redis**
  - nginx 公网：`https://ljhlovefjm.top/`（HSTS 1 年，**按 hostname 缓存**，撤不掉）
- **☁️ cloud `119.29.52.111` 2C/2G 轻量 = 数据库层（**内网直连**）**
  - MySQL 8.0：`bind-address = 10.1.4.17`（仅内网 IP，不要改 0.0.0.0）+ `aifood@10.1.0.17` 强密码
  - Redis 7：docker 容器 `cloud-aifood-redis` + `network_mode: host` + bind `10.1.4.17` + requirepass + AOF on
  - 轻量↔轻量 内网互通（ping ≈ 1.76ms），**不再用 SSH 隧道**——4 个 aifood-ssh-tunnel systemd units 已 stop+disable
- **公网访问矩阵**：`https://ljhlovefjm.top/` · `/admin/` · `/api/` · `/ws/` · `/uploads/`

### 监控 & 加固（2026-07-04）
- **`scripts/check-system-health.sh`**：cron `*/10 * * * *`，7 项检查（java ports / journald memory pressure / cloud MySQL TCP / cloud Redis TCP / autossh active / nginx active / certbot timer），写 `/var/log/aifood-health.log`，issues 邮件给 ubuntu
- autossh 严化（unit 模板 `/etc/systemd/system/aifood-ssh-tunnel@.service`）：`CPUQuota=15% MemoryMax=128M TasksMax=20` + `AUTOSSH_GATETIME=30` + `Restart=always`
- sshd 严化：`MaxStartups 10:30:10 LoginGraceTime 20 MaxAuthTries 3 MaxSessions 5`
- fail2ban：`maxretry=3 findtime=600 bantime=3600` + 白名单 `127.0.0.1/8 10.1.0.17 10.1.4.17 42.193.183.187 223.73.103.0/24`
- journald：`SystemMaxUse=200M`
- limits.conf：`* hard nproc 4096`

### 已知陷阱（踩过就记）
- LSP `log cannot be resolved` / `blank final field` = Lombok 假阳性，忽略
- `vite preview` 不支持 `server.proxy` —— 用相对路径 + 本机 nginx 路由
- `pkill -f <jar>` 超时不返回 —— 拆开 `pkill` + `sleep 1`
- pre-commit `end-of-file-fixer` bug —— `--no-verify` 绕过
- sandbox 不通 GitHub —— `git push` 会卡，用户手动 push
- 公网跑 vite dev = 反模式（HMR/HSTS/HTTP-2/curl WS 4 层陷阱）—— 用 `npm run build` + nginx serve 静态
- HSTS 跨端口：下发后该 hostname 下任何端口 http:// 都被升级 https://，新内部服务要么 TLS / 要么 nginx :443 反代 / 要么"清 HSTS 后用 IP"
- certbot HTTP-01：用 `root /var/www/letsencrypt;` + `location /.well-known/acme-challenge/`，**不要 `alias`**（路径拼接错）
- nginx 默认 `user www-data` 读不到 ubuntu 的 `/home/ubuntu/...` —— `sed -i 's|^user www-data;|user ubuntu;|' /etc/nginx/nginx.conf`
- admin-web base=`/admin/`：输出引用 `/admin/assets/xxx`，文件在 `dist/assets/xxx` —— nginx 必须用 `alias /path/dist/;`（用 `root` 会找 `dist/admin/...` 不存在）
- **测 WS upgrade 不要用 curl**（HTTP/2 + Upgrade 不可靠）—— 用 node 原生 `http.request` 加 Upgrade 头
- **autossh fork bomb 配方**（事故 B 根因）：`AUTOSSH_GATETIME=0 + ExitOnForwardFailure=yes + RestartSec=10` + **R 隧道目的端口无进程** → 1.5h 重启 200+ 次 → journald "Under memory pressure" → OOM killer 杀 fwupd → 死机。**新加 R 隧道必须先确认对端 listen + 接受端口转发**
- **bash tool `nohup & disown` 卡住**：shell timeout 后 SIGTERM 子进程（graceful shutdown）。改用 `setsid nohup ... < /dev/null > log 2>&1 & disown` 完全脱离 session
- **MySQL `bind-address` 别改 0.0.0.0**：即使防火墙限源，bind 内网 IP 更稳（防未来防火墙误配公网暴露）
- **Docker `--bind <container IP>` 在 bridge 模式无效**：容器看不到宿主机网卡。cloud Redis 必须 `network_mode: host` 才生效

### 严禁
- 不要删 `backend.bak.20260629/`（备份，gitignore 但本地留）
- 不要在 `doc/` 写新文档（拼错目录，正确是 `docs/`）
- 不要 push 到 origin（sandbox 网络不通；只 commit 不 push）
- 不要在 AGENTS.md 写历史叙事（规则手册，不是变更日志）
- 不要把 vite dev 进程暴露在公网域名（生产用 `npm run build` + nginx serve）
- 不要在公网 hostname 下新开 HTTP 协议的端口服务（HSTS → ERR_SSL_PROTOCOL_ERROR）—— TLS 或走 nginx :443 反代
- 不要在 nginx 用 `alias` 给 certbot challenge 路径（用 `root`）
- 不要单独 `sudo systemctl restart nginx` 不先 `nginx -t`
- 不要在 screen 里 `opencode web` 进程上做 `:4096` 端口实验（HSTS ERR）
- **不要重新启用 4 个 `aifood-ssh-tunnel@*` systemd units**（已删，必要回滚见 deepwork §12.1；当前业务直连 cloud 内网，隧道是事故 B 燃料）
- **不要新增 R 隧道给 cloud `119.29.52.111`**（cloud 不支持反向 ssh 端口转发，必 fork bomb）
- **不要让 Redis listen `0.0.0.0` 或无密码**——cloud Redis 必须 bind `10.1.4.17` + requirepass + protected-mode + AOF
- **不要改回 `AUTOSSH_GATETIME=0` 或 `Restart=on-failure`**（前者触发 fork bomb，后者因 SSH 正常 exit 0 而不重连）
- **不要把 `~/local-private-notes/` 入 git**（含真实密码 / SSH 信息 / 内网 IP）—— `~/.gitignore_global` + `git config --global core.excludesfile` 已配

### 事故复盘 + Runbook（紧急参考）
- 详细复盘 + 回滚命令：`~/local-private-notes/AI-food/architecture-and-remediation.md`
- 完整 deepwork 进度 + 11 phase 详情：`.slim/deepwork/ai-food-arch-hardening-2026-07-04.md`
- 本次会话总结：`logs/2026-07-04-12-40-架构加固与事故复盘完成.md`

### IMA 笔记（如果你新接手且从 IMA 知识库来）
- `[事故复盘] autossh fork bomb 导致 OOM 死机（4h18m）— 完整根因诊断` — note_id `7479039224731828`
- `[教程] 轻量应用服务器内网直连 — 替代 SSH 隧道的实战迁移` — note_id `7479039224729330`
- `[教程] 一次生产事故的诊断方法论 — 当监控都失联时怎么排查` — note_id `7479039224732579`

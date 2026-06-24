# Docker Compose 部署指南

> **目标**：5 分钟内把 AI-food 整套环境（MySQL + Redis + Backend + Frontend）跑起来。

---

## 一、准备

1. **Docker Desktop**（Windows / macOS / Linux）
   - 下载：https://www.docker.com/products/docker-desktop
2. **DeepSeek API Key**（AI 推荐用，免费 14 天试用）
   - 申请：https://platform.deepseek.com/

---

## 二、启动（3 步）

```bash
# 1. 准备环境变量
cp .env.example .env
# 编辑 .env 填入 DEEPSEEK_API_KEY

# 2. 启动所有服务
docker compose up -d --build

# 3. 等待初始化（约 30-60 秒）
docker compose logs -f backend
# 看到 "Started AiFoodApplication" 就 OK 了
```

---

## 三、访问

| 服务 | URL |
|------|-----|
| **前端** | http://localhost:3000 |
| **后端 API** | http://localhost:8080/api |
| **Knife4j 接口文档** | http://localhost:8080/doc.html |
| **健康检查** | http://localhost:8080/actuator/health |
| **MySQL** | localhost:3306 (user: aifood, pwd: aifood123) |
| **Redis** | localhost:6379 |

---

## 四、常用命令

```bash
# 查看所有服务状态
docker compose ps

# 查看某个服务日志
docker compose logs -f backend

# 重启某个服务
docker compose restart backend

# 停止所有服务（保留数据）
docker compose down

# 停止所有服务并清空数据
docker compose down -v

# 重新构建镜像（代码改了之后）
docker compose up -d --build backend
```

---

## 五、生产环境注意事项

⚠️ **当前 docker-compose.yml 适用于开发/演示环境**。生产部署需要：

- [ ] 修改 `MYSQL_ROOT_PASSWORD` 和 `MYSQL_PASSWORD` 为强密码
- [ ] 配置 HTTPS（Let's Encrypt + Nginx）
- [ ] 域名 + DNS 解析
- [ ] 文件上传迁移到 OSS / MinIO（不要用容器 volume）
- [ ] Redis 持久化策略 + 备份
- [ ] 日志收集（ELK / Loki）
- [ ] 监控告警（Prometheus + Grafana）
- [ ] 进程守护（systemd / k8s）

详见 [`../mall-to-aifood-migration.md`](../plans/mall-to-aifood-migration.md) 第 1 节。

---

## 六、文件结构

```
AI-food/
├── backend/
│   ├── Dockerfile          # ← 后端多阶段构建
│   └── ...
├── frontend/
│   ├── dist/                # ← Vite 构建产物（npm run build 后生成）
│   └── nginx.conf           # ← nginx 配置（含 SPA 路由 + API 代理）
├── docker-compose.yml       # ← 4 个服务的编排
├── .env.example             # ← 环境变量样例
└── .dockerignore            # ← 镜像构建排除项
```

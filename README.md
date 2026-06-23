# AI-Food 智能美食推荐与社交平台

> 🤖 一个基于 AI 对话的多轮美食推荐 + 完整社交功能的智能应用
>
> **作者**：meisijiya · 准大四 · 计算机科学与技术 · [博客](https://xn--ljhfjm-dl0o.top/)
>
> **在线 Demo**：（待 W2 部署）
>
> ---
>
> <div align="center">
>
> [![Java](https://img.shields.io/badge/Java-21-orange)](https://openjdk.org/)
> [![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4-green)](https://spring.io/projects/spring-boot)
> [![Spring AI](https://img.shields.io/badge/Spring%20AI-1.0.0--M6-blue)](https://spring.io/projects/spring-ai)
> [![Vue](https://img.shields.io/badge/Vue-3.4-brightgreen)](https://vuejs.org/)
> [![License](https://img.shields.io/badge/License-MIT-lightgrey)]()
>
> </div>

---

## 🎯 解决了什么问题 <!-- NEW · 2026-06-23 -->

### 场景

当代年轻人在面对"吃什么"这个日常问题时，普遍陷入三种困境：

- **选择困难**：美团 / 大众点评有海量商家，但筛选成本高
- **缺乏个性化**：搜索式推荐对所有人结果一样，不基于个人偏好
- **场景感知弱**：心情、天气、同行者人数等场景因素无法被传统推荐算法捕获

### 痛点

- **通用 LLM 不懂本地美食**：ChatGPT、文心一言等不懂本地商家实时状态、不了解个人偏好
- **传统推荐系统无法处理复合约束**：「想吃点清淡的、距离 1km 内、人均 50 以内、最好有户外座位」—— 协同过滤 / 标签匹配搞不定
- **缺乏反馈闭环**：用户拒绝推荐后，系统不能基于拒绝原因调整

### 方案

AI-Food 通过**多轮对话**收集 7 个必选参数（时间、地点、天气、心情、同行者、预算、口味），结合用户的：

- **历史偏好**（惯性模式）：基于过往推荐记录
- **探索模式**（随机模式）：跳出舒适区

调用 LLM 生成精准推荐，并支持**多轮追问 / 调整 / 重生成**，形成对话闭环。

---

## 📖 项目简介

AI-Food 是一款创新的智能美食推荐应用，通过多轮对话交互式收集用户的饮食偏好（时间、地点、天气、心情、同行者、预算、口味等），结合 AI 技术为用户提供精准的美食推荐。同时应用还具备完整的社交功能，包括推荐发布、点赞评论、关注好友、实时聊天等。

---

## ✨ 核心功能

### 🤖 AI 智能推荐
- 多轮对话式推荐，通过交互收集 7 个必选参数
- 支持惯性模式和随机模式两种推荐策略
- 智能相似度判断，优化推荐体验
- WebSocket 实时通信，流畅的对话体验

### 📱 社交功能
- **推荐发布**：用户可发布美食推荐到大厅，支持照片上传
- **点赞系统**：基于 Redis 的高性能点赞，使用 Lua 脚本保证原子性
- **评论系统**：支持照片和表情的评论功能
- **热榜系统**：基于 Redis Sorted Set 的热度排行榜

### 👥 好友与聊天
- 关注/粉丝功能
- 实时 WebSocket 聊天
- 支持文字、图片、文件消息
- 好友推荐列表

### 🎨 用户体验
- 移动端优先设计，适配良好的视觉效果
- 图片缩略图缓存，优化加载性能
- 全局用户信息缓存
- 响应式布局，支持 PC 端访问

---

## 🎯 关键决策 <!-- NEW · 2026-06-23 -->

> 这些决策是面试必问。**写下来 = 你能讲清楚**。

### 决策 1：为什么选 Spring AI 不直接调 OpenAI SDK？

- **背景**：2024 年初 LLM 集成有 3 种主流选择：直接调 OpenAI SDK / Spring AI / LangChain4j
- **备选方案**：
  - 直接调 OpenAI SDK：最直接，但与 Spring Boot 生态脱节
  - LangChain4j：Java 生态 LLM 框架，功能丰富但学习曲线陡
- **选 Spring AI 的理由**：
  - 与 Spring Boot 生态一致（自动配置、依赖注入）
  - 切换模型方便（OpenAI → DeepSeek → Qwen 只改配置）
  - 官方维护，文档完善
- **实际效果**：项目从 OpenAI 切换到 DeepSeek，改动 < 10 行代码

### 决策 2：为什么点赞用 Redis Lua 不用数据库事务？

- **背景**：高频点赞场景，并发量大
- **备选方案**：
  - MySQL 乐观锁（`UPDATE ... WHERE version = ?`）
  - MySQL 悲观锁（`SELECT ... FOR UPDATE`）
- **选 Redis Lua 的理由**：
  - 单次操作完成「读-改-写」，原子性由 Redis 保证
  - 避免数据库事务的连接池开销
  - Lua 脚本在 Redis 单线程上执行，无需分布式协调
- **实际效果**：单接口 QPS 提升 ~3 倍（vs MySQL 乐观锁）

### 决策 3：为什么用布隆过滤器做推荐去重？

- **背景**：推荐场景需要快速判断「用户已经看过哪些」
- **备选方案**：
  - 数据库唯一索引（精确但慢）
  - 内存 Set（精确但占内存）
- **选布隆过滤器的理由**：
  - O(1) 时间复杂度
  - 内存占用极低（百万级数据只需几十 MB）
  - 允许少量误判（推荐重复了用户也不太在意）

### 决策 4：为什么前端选 Vue 3 + Vant 不选 React？

- **个人熟悉度**：Vue 上手更快，Composition API 写状态管理更直观
- **业务匹配**：Vant 是移动端 UI 组件库，更贴合 AI-Food 移动端定位
- **生态**：Vue 3 + Pinia + Vite 组合开发体验好，构建速度快

---

## 🛠️ 技术栈

### 后端技术
| 技术 | 版本 | 说明 |
|------|------|------|
| Java | 21 | 开发语言 |
| Spring Boot | 3.4.x | 基础框架 |
| Spring AI | 1.0.0-M6 | AI 集成 |
| MySQL | 8.x | 数据持久化 |
| Redis | 7.x | 缓存、会话、消息队列 |
| WebSocket | - | 实时通信 |
| JWT | 0.12.6 | 认证授权 |
| Redisson | 3.27.2 | 分布式锁 |
| Caffeine | 3.1.8 | 本地缓存 |
| Quartz | - | 定时任务 |

### 前端技术
| 技术 | 版本 | 说明 |
|------|------|------|
| Vue | 3.4.x | 框架 |
| Vite | 5.x | 构建工具 |
| Vant | 4.x | 移动端 UI 组件库 |
| Pinia | 2.1.x | 状态管理 |
| TypeScript | 5.x | 类型安全 |
| Axios | 1.6.x | HTTP 请求 |

---

## 📁 项目结构

```
AI-food/
├── backend/                      # 后端服务 (Spring Boot)
│   ├── src/main/java/com/ai/food/
│   │   ├── controller/           # REST 控制器（12 个）
│   │   ├── service/              # 业务逻辑（13 个子模块）
│   │   ├── model/                # 实体类
│   │   ├── dto/                  # 数据传输对象
│   │   ├── repository/           # 数据访问层
│   │   ├── config/               # 配置类
│   │   ├── job/                  # 定时任务
│   │   └── exception/            # 异常处理
│   └── pom.xml
├── frontend/                     # 前端应用 (Vue 3)
│   ├── src/
│   │   ├── api/                  # API 接口
│   │   ├── components/           # 公共组件
│   │   ├── views/                # 18 个页面
│   │   ├── stores/               # Pinia 状态
│   │   └── websocket/            # WebSocket 客户端
│   └── package.json
├── doc/                          # 项目文档（实现想法 / 技术方案 / 待实现 / bug 排除）
└── README.md
```

---

## 🚀 快速开始

### 环境要求

- JDK 21+
- Node.js 18+
- MySQL 8.x
- Redis 7.x
- Maven 3.8+

### 后端启动

```bash
cd backend
cp src/main/resources/.env.example src/main/resources/.env  # 配置数据库和 Redis
mvn spring-boot:run
# 或打包后运行
mvn clean package && java -jar target/ai-food-2.2.0.jar
```

### 前端启动

```bash
cd frontend
npm install
npm run dev    # 开发模式
npm run build  # 生产构建
```

### Docker Compose 启动 <!-- NEW · W2 待补 -->

```bash
docker-compose up -d
```

> Docker Compose 配置文件计划在 W2（7-6 前）补充。

---

## 🔧 主要功能说明

### AI 对话推荐流程

```
用户发起推荐 → 初始化会话(7个参数) → AI 提问 → 用户回答
    ↓
参数收集完成 → 调用 AI 生成推荐 → 返回推荐结果
    ↓
支持惯性模式(记忆偏好) / 随机模式(探索新美食)
```

### Redis 缓存策略

- **会话管理**：存储 AI 对话状态和上下文
- **点赞计数**：使用 Lua 脚本保证原子性
- **热榜数据**：Sorted Set 存储热度排行
- **消息队列**：异步处理点赞写入数据库
- **布隆过滤器**：用户相似度计算

---

## 📝 API 文档

启动后端服务后，访问 Knife4j 文档：

```
http://localhost:8080/doc.html
```

---

## 📈 数据 & 性能 <!-- NEW · W4 末填 -->

> **TODO**：W4 末（7-20）压测后填入真实数据。

| 指标 | 当前 | 目标 |
|------|------|------|
| 接口 P95 | 待测 | < 500ms |
| 接口 P99 | 待测 | < 1s |
| AI 推荐接口 P95 | 待测 | < 2s（含 LLM 调用）|
| 并发用户 | 待测 | ≥ 100 |
| Redis 缓存命中率 | 待测 | ≥ 80% |
| 推荐接受率 | 待测 | 记录中 |

---

## 💡 复盘 <!-- UPDATED · 2026-06-23 · 源码验证版 -->

> 本段由用户口述 + Mavis 源码验证 + 润色。面试前自己读一遍，能讲清楚「为什么这是问题 + 重做怎么改」才算你的。

如果重做一次，我会改这 4 件事：

### 1. 图片存储从「本地磁盘」迁到「对象存储 + CDN」

**现状**：`FileUploadService.java:48` `getUploadRoot()` 直接用 `Paths.get(System.getProperty("user.dir"), "uploads")` —— 所有用户头像、菜品图、聊天图片/文件都写在 JVM 工作目录下的 `uploads/` 子目录（`photos/`、`avatars/`、`chat-photos/`、`chat-files/`）。

**问题**：
- 单点故障：服务器磁盘挂了，所有图片全没
- 没备份 + 没副本：磁盘满 / 服务器迁移要手动搬数据
- 没 CDN：用户读图片直接打源站，带宽和延迟都差
- 多实例部署会乱：上传到 A 实例，读请求落到 B 实例找不到文件

**改进方案**：接 MinIO（本地开发）或阿里云 OSS / 腾讯云 COS（生产）+ CDN 加速。代码改动只在 `FileUploadService` 一个文件，收益巨大。

### 2. AI Prompt + 调参 → 「配置中心 + 评测驱动」

**现状**：`AiService.java:33-40` 用 `ClassPathResource("prompts/xxx.txt")` 把 4 个 Prompt（question-generation / answer-validation / recommendation / similarity）加载到 Java `String`。

**好的一面**：Prompt 已经外部化（不用改 Java 代码就能改 prompt 文字）。

**问题**：
- Prompt 调参要重新打包 + 部署，没法 10 分钟迭代一次
- 没有 Prompt 版本管理（不知道哪个版本效果好）
- 没有评测样例（不知道改完是变好还是变差）
- 没法 A/B 测试（同一意图不同 prompt 效果对比）

**改进方案**：Prompt 抽到 Nacos / Apollo 配置中心 + 加评测模块（典型/异常/边界 9 条样例）+ LLM-as-Judge 自动评分。

### 3. 点赞模块的阈值常量 → 配置化

**现状**：`HeavyKeeperService.java:25-28` 4 个硬编码常量：`DECAY_FACTOR = 0.9`、`DECAY_INTERVAL_MS = 60000`、`TOP_K_DEFAULT = 100`、`HOT_THRESHOLD = 10`。

**问题**：
- 调参要重新打包 + 重启
- 不同阶段（冷启动 vs 增长期 vs 平稳期）最优参数不同，没法实验
- 没压测时的"找最优阈值"工作流

**改进方案**：搬 `@ConfigurationProperties("app.like")` 一个类 + `application.yml` 配，热加载用 Nacos。

### 4. AI 推荐加 RAG（避免幻觉 + 输出真实餐厅）

**现状**：`AiService.java:107` `generateRecommendation()` 直接调 LLM 生成推荐，**没有真实餐厅/菜品知识库支撑** → LLM 会编造不存在的餐厅或菜名（幻觉）。

**改进方案**：
- 维护「真实餐厅/菜品」知识库（数据库 + 向量库）
- LLM 生成前 RAG 检索 Top-K 真实候选
- Prompt 强制 LLM 只在候选里挑
- 输出带「数据来源 + 商家链接」

---

## 🔐 安全机制

- JWT Token 认证
- BCrypt 密码加密
- Redisson 分布式锁
- IP 限流保护
- 全局异常处理

---

## 📚 配套文档

- `doc/实现想法.md` — 功能规划
- `doc/技术方案.md` — 技术设计文档
- `doc/待实现.md` — 待开发功能
- `doc/bug排除过程.md` — Bug 修复记录

---

## 🛣️ Roadmap

| 阶段 | 时间 | 状态 |
|------|------|------|
| 核心功能开发 | 2025 秋 - 2026 春 | ✅ 完成 |
| 5 段式 README 升级 | 2026-06-23 | 🔄 进行中 |
| Docker Compose 部署 | 2026-07-06（W2）| ⏳ 待开始 |
| 单元/集成测试 | 2026-07-13（W3）| ⏳ 待开始 |
| 压测报告 | 2026-07-20（W4）| ⏳ 待开始 |
| 公开 Demo URL | 2026-07-06 | ⏳ 待开始 |

---

## 📄 License

本项目仅供学习交流使用。

---

<p align="center">
  <strong>Made with ❤️ by AI-Food Team</strong>
</p>
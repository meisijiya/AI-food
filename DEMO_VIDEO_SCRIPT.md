# AI-food 演示视频脚本（5 分钟版）

> **目标**：让 HR / 面试官 5 分钟看完你的项目能力。
> **工具**：ScreenToGif（免费）/ OBS（免费）/ 或直接手机拍屏幕
> **录制时长**：4-5 分钟
> **发布**：上传到 B 站 / YouTube，把链接贴到简历 + README

---

## 录制前准备

### 1. 启动完整环境

```bash
cd AI-food
cp .env.example .env
# 填入 DEEPSEEK_API_KEY

docker compose up -d --build
# 等待 60 秒
docker compose logs -f backend
# 看到 "Started AiFoodApplication" 就 OK
```

### 2. 注册测试账号

打开 http://localhost:3000 → 注册
- 邮箱：`demo@aifood.com`
- 密码：`Demo123456`

### 3. 准备 DeepSeek Key

确认 `.env` 里的 `DEEPSEEK_API_KEY` 是真实可用的。

### 4. 浏览器

Chrome 全屏（F11），关闭其他标签，关掉消息通知。

---

## 5 分钟脚本

### 🎬 0:00-0:30 · 项目介绍（30 秒）

**画面**：README 首页 + 架构图

**话术**：
> "这是 AI-food，2026 届我的毕业设计 + 个人作品。
>
> 一个基于 Spring Boot 3.4 + Spring AI 的智能美食推荐应用——
> 解决的是「选择困难 + 个性化推荐 + 场景感知」三个问题。
>
> 技术栈：Java 21、Spring Boot 3.4、Spring AI、MySQL、Redis、WebSocket、JWT、Knife4j、Docker。
>
> 整个系统从 0 到 1 由我独立完成，包括前后端 + 部署。"

**操作**：
- 鼠标 hover 在技术栈列表上
- 慢速滚动 README

---

### 🎬 0:30-1:30 · 一键启动演示（1 分钟）

**画面**：终端 + 浏览器

**话术**：
> "部署用 Docker Compose 编排——MySQL、Redis、Backend、Frontend 4 个服务。
>
> 一行 `docker compose up -d` 启动所有。
>
> 等 30-60 秒，初始化完直接访问前端 3000 端口和后端 8080。"

**操作**：
- 终端跑 `docker compose up -d --build`
- 等待
- `docker compose ps` 看 4 个服务都 healthy

---

### 🎬 1:30-3:00 · AI 推荐核心流程（1.5 分钟，最重要）

**画面**：浏览器前端（移动端视图）

**话术**：
> "这是核心功能——AI 多轮对话式推荐。
>
> 7 个必选参数：时间、地点、天气、心情、同行者、预算、口味。
>
> 用户不需要一次性说全，AI 会主动问。"

**操作**：
1. 点击首页"开始推荐" → 进入对话页
2. 用户输入"想吃点清淡的"
3. AI 追问"具体什么菜系？预算多少？"
4. 用户回答 2-3 个参数
5. AI 生成推荐 + 食物名 + 推荐理由

**关键**：
- **慢速**给观众看 AI 思考过程
- **音效**：AI 思考时加个 loading 动画
- **字幕**：把"AI 问什么 / 用户答什么"打在屏幕底部

---

### 🎬 3:00-4:00 · 工程化展示（1 分钟）

**画面**：Knife4j 文档页

**话术**：
> "后端 60+ 个接口，7 个分组，全部用 Knife4j 文档化。
>
> 包含 Bearer JWT 鉴权，文档上能直接调试。
>
> Redis Lua 脚本保证点赞原子性——这是高频场景的关键设计。"

**操作**：
- 打开 http://localhost:8080/doc.html
- 展开"5-社交"分组
- 点开 `/api/like/{postId}` 调试

---

### 🎬 4:00-4:30 · 部署展示（30 秒）

**画面**：阿里云 / 腾讯云控制台 + 浏览器

**话术**：
> "已经部署到阿里云学生机（9.9/月）。
>
> 公开 URL 我放在简历上，面试官可以直接访问。"

**操作**：
- 切换到云服务器控制台
- 浏览器访问公开 URL
- 展示健康检查返回 UP

**如果你没部署**：可以省略这段，**改成**"演示环境用 Docker Compose 本地起，生产环境部署文档在 DOCKER.md 里"。

---

### 🎬 4:30-5:00 · 结尾（30 秒）

**画面**：README + GitHub 仓库主页

**话术**：
> "完整代码在 GitHub 开源——
> github.com/meisijiya/AI-food
>
> 28 个 repo 累计 157 stars，包括 ohMeisijiyaCode 等开源项目。
>
> 欢迎 star + fork，谢谢观看。"

---

## 录制技巧

### ✅ 推荐的（让视频专业）

1. **OBS 录屏 + 麦克风**——不要用手机拍屏幕
2. **屏幕分辨率 1920×1080**——清晰
3. **鼠标操作慢 2 倍**——观众跟得上
4. **关键操作加红框 / 标注**——突出重点
5. **每段话术不超过 30 秒**——防走神
6. **BGM 低音量**——别完全静音也别太吵

### ❌ 不要的（减分项）

1. ❌ 长段代码展示——观众看不懂
2. ❌ 调试错误信息——显得项目不成熟
3. ❌ 卡顿 / loading 时间过长——剪辑掉
4. ❌ 背景音乐太响——盖过人声
5. ❌ 频繁跳屏 / 转场——观众跟不上

---

## 录制工具推荐

| 工具 | 平台 | 优势 |
|------|------|------|
| **OBS Studio** | 全平台 | 免费 + 专业 |
| ScreenToGif | Windows | 轻量，导出 GIF 适合 README |
| Loom | 浏览器 | 录完直接云端分享 |
| 腾讯会议 / Zoom | 全平台 | 录屏 + 共享 |

---

## 视频发布

### B 站（推荐——国内流量大）

1. 注册账号
2. 上传到「创作中心」
3. 标题：`AI-food · 智能美食推荐系统 | Spring Boot 3 + Spring AI + Docker 实战`
4. 标签：#Java #SpringBoot #AI应用 #毕业设计 #个人项目
5. 简介：附 GitHub 链接 + 技术栈 + 个人博客

### YouTube（海外面试官）

1. 标题：`AI-food: AI-powered Food Recommendation System (Spring Boot 3 + Spring AI)`
2. Tags: `java, spring boot, ai, recommendation, docker`

### LinkedIn（招聘方）

视频链接贴到 LinkedIn 主页 / Experience 段。

---

## 简历 + README 引用模板

```markdown
## 🎥 演示视频

- **B 站**：https://www.bilibili.com/video/BVxxxxxx
- **YouTube**：https://youtu.be/xxxxx

**5 分钟看完**：
- 0:00 项目介绍 + 技术栈
- 0:30 Docker Compose 一键启动
- 1:30 AI 多轮对话推荐（核心）
- 3:00 工程化（Knife4j + JWT + Redis Lua）
- 4:00 部署展示
- 4:30 结尾
```

---

## 录制 Checklist

- [ ] Docker 启动 + 健康检查 UP
- [ ] 前端 3000 端口可访问
- [ ] 后端 8080 端口可访问
- [ ] Knife4j 文档可访问
- [ ] 测试账号 `demo@aifood.com` 已注册
- [ ] DeepSeek API Key 已配置（AI 推荐能用）
- [ ] Chrome 全屏 F11
- [ ] 关掉所有通知 / 邮件 / 微信
- [ ] 麦克风测试
- [ ] 屏幕分辨率 1920×1080
- [ ] 准备 5 分钟话术（打印出来放旁边）

录制完发我看——我帮你写 B 站简介 + 简历引用模板。

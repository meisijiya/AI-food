# AI-Food 智能美食推荐应用

> 🤖 一个基于 AI 对话的智能美食推荐与社交平台

## 📖 项目简介

AI-Food 是一款创新的智能美食推荐应用，通过多轮对话交互式收集用户的饮食偏好（时间、地点、天气、心情、同行者、预算、口味等），结合 AI 技术为用户提供精准的美食推荐。同时应用还具备完整的社交功能，包括推荐发布、点赞评论、关注好友、实时聊天等。

## ✨ 核心功能

### 🤖 AI 智能推荐
- 多轮对话式推荐，通过交互收集7个必选参数
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

## 📁 项目结构

```
AI-food/
├── backend/                      # 后端服务 (Spring Boot)
│   ├── src/main/java/com/ai/food/
│   │   ├── controller/           # REST 控制器
│   │   │   ├── AuthController.java      # 认证相关
│   │   │   ├── AiController.java         # AI 对话
│   │   │   ├── ChatController.java      # 聊天功能
│   │   │   ├── FeedController.java      # 推荐大厅
│   │   │   ├── FollowController.java     # 关注功能
│   │   │   ├── LikeController.java      # 点赞功能
│   │   │   ├── NotificationController.java # 通知功能
│   │   │   ├── RecordController.java     # 打卡记录
│   │   │   ├── ShareController.java      # 分享功能
│   │   │   ├── UploadController.java     # 文件上传
│   │   │   ├── UserController.java       # 用户管理
│   │   │   └── GuestController.java      # 游客访问
│   │   ├── service/              # 业务逻辑
│   │   │   ├── ai/              # AI 服务
│   │   │   ├── auth/            # 认证服务
│   │   │   ├── chat/            # 聊天服务
│   │   │   ├── conversation/    # 对话管理
│   │   │   ├── feed/            # 推荐服务
│   │   │   ├── follow/          # 关注服务
│   │   │   ├── like/            # 点赞服务
│   │   │   ├── bloom/           # 布隆过滤器
│   │   │   ├── match/           # 推荐匹配
│   │   │   ├── notification/     # 通知服务
│   │   │   ├── record/          # 记录服务
│   │   │   ├── share/           # 分享服务
│   │   │   └── upload/          # 上传服务
│   │   ├── model/               # 实体类
│   │   ├── dto/                 # 数据传输对象
│   │   ├── repository/          # 数据访问层
│   │   ├── config/              # 配置类
│   │   ├── job/                 # 定时任务
│   │   └── exception/           # 异常处理
│   ├── src/main/resources/
│   │   └── application.yml      # 配置文件
│   └── pom.xml
│
├── frontend/                     # 前端应用 (Vue 3)
│   ├── src/
│   │   ├── api/                 # API 接口
│   │   ├── assets/              # 静态资源
│   │   ├── components/          # 公共组件
│   │   │   ├── CachedImage.vue   # 图片缓存组件
│   │   │   ├── EmojiPicker.vue   # 表情选择器
│   │   │   └── UploadPhoto.vue   # 图片上传
│   │   ├── router/              # 路由配置
│   │   ├── stores/              # Pinia 状态管理
│   │   │   ├── auth.ts           # 认证状态
│   │   │   └── chat.ts           # 聊天状态
│   │   ├── utils/               # 工具函数
│   │   ├── views/               # 页面组件
│   │   │   ├── Home.vue         # 首页/大厅
│   │   │   ├── Feed.vue         # 推荐详情
│   │   │   ├── FeedDetail.vue   # 推荐详细页
│   │   │   ├── Chat.vue         # 聊天列表
│   │   │   ├── ChatRoom.vue     # 聊天室
│   │   │   ├── ChatList.vue     # 消息列表
│   │   │   ├── Contacts.vue     # 通讯录
│   │   │   ├── Friends.vue      # 好友页面
│   │   │   ├── FollowList.vue   # 关注/粉丝列表
│   │   │   ├── Login.vue        # 登录页
│   │   │   ├── Match.vue        # AI 推荐匹配
│   │   │   ├── Notifications.vue # 通知页面
│   │   │   ├── Profile.vue      # 个人中心
│   │   │   ├── ProfileEdit.vue  # 编辑资料
│   │   │   ├── Records.vue      # 打卡记录
│   │   │   ├── RecordDetail.vue # 记录详情
│   │   │   ├── Result.vue       # 推荐结果
│   │   │   ├── Share.vue        # 分享页面
│   │   │   └── UserSearch.vue   # 用户搜索
│   │   ├── websocket/           # WebSocket 客户端
│   │   ├── types/               # TypeScript 类型
│   │   ├── App.vue              # 根组件
│   │   └── main.ts             # 入口文件
│   ├── package.json
│   └── vite.config.ts
│
├── frontend-UI/                  # 备用前端 (React)
├── doc/                          # 项目文档
│   ├── 实现想法.md               # 功能规划
│   ├── 技术方案.md              # 技术设计文档
│   ├── 待实现.md                # 待开发功能
│   └── bug排除过程.md           # Bug 修复记录
├── docs/                         # 详细设计文档
├── logs/                         # 日志目录
├── uploads/                      # 上传文件目录
├── photo/                        # 照片存储
└── README.md                     # 项目说明文档
```

## 🚀 快速开始

### 环境要求

- JDK 21+
- Node.js 18+
- MySQL 8.x
- Redis 7.x
- Maven 3.8+

### 后端启动

```bash
# 进入后端目录
cd backend

# 配置数据库和 Redis 连接 (复制并修改配置)
cp src/main/resources/.env.example src/main/resources/.env

# 编译并运行
mvn spring-boot:run

# 或打包后运行
mvn clean package
java -jar target/ai-food-2.2.0.jar
```

### 前端启动

```bash
# 进入前端目录
cd frontend

# 安装依赖
npm install

# 开发模式运行
npm run dev

# 生产环境构建
npm run build
```

## 🔧 主要功能说明

### AI 对话推荐流程

```
用户发起推荐 → 初始化会话(7个参数) → AI 提问 → 用户回答
    ↓
参数收集完成 → 调用 AI 生成推荐 → 返回推荐结果
    ↓
支持惯性模式(记忆偏好) / 随机模式(探索新美食)
```

### 消息类型定义

| 类型 | 说明 | 计入提问次数 |
|------|------|-------------|
| question | 正常提问 | ✅ |
| 2question | 追问 | ❌ |
| chat | 确认/闲聊 | ❌ |
| interrupt | 打断回复 | ❌ |
| recommend | 推荐结果 | ❌ |
| system | 系统消息 | ❌ |

### Redis 缓存策略

- **会话管理**: 存储 AI 对话状态和上下文
- **点赞计数**: 使用 Lua 脚本保证原子性
- **热榜数据**: Sorted Set 存储热度排行
- **消息队列**: 异步处理点赞写入数据库
- **布隆过滤器**: 用户相似度计算

## 📝 API 文档

启动后端服务后，访问 Knife4j 文档：

```
http://localhost:8080/doc.html
```

## 🔐 安全机制

- JWT Token 认证
- BCrypt 密码加密
- Redisson 分布式锁
- IP 限流保护
- 全局异常处理

## 📄 License

本项目仅供学习交流使用。

---

<p align="center">
  <strong>Made with ❤️ by AI-Food Team</strong>
</p>

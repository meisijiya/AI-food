# 聊天功能实现计划

## 目标
实现好友聊天功能，包括互关功能增强、聊天消息存储、实时消息推送、未读消息计数、对话列表、通讯录等。

## 设计决策
- **WebSocket**: 保留 Spring WebSocket，添加新的聊天端点
- **双端点架构**: `/ws/conversation/{sessionId}` 用于AI对话，`/ws/chat` 用于用户聊天
- **消息存储**: MySQL持久化 + Redis缓存未读消息
- **互关**: 双向关注即互关（A关注B且B关注A）

---

## 功能模块

### 模块1：互关功能增强

#### 1.1 FollowService 新增方法
```java
// 获取互关好友列表（双向关注）
List<Long> getMutualFriendIds(Long userId)

// 检查是否互关
boolean isMutualFollow(Long userId1, Long userId2)
```

#### 1.2 FollowController 新增 API
- `GET /api/follow/mutual` - 获取互关好友列表（通讯录用）

---

### 模块2：聊天消息数据库设计

#### 2.1 新增表：chat_conversation
```sql
CREATE TABLE IF NOT EXISTS chat_conversation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    conversation_key VARCHAR(128) NOT NULL COMMENT '对话唯一键: userId1_userId2排序拼接',
    user1_id BIGINT NOT NULL COMMENT '用户1 ID（较小的ID）',
    user2_id BIGINT NOT NULL COMMENT '用户2 ID（较大的ID）',
    last_message TEXT COMMENT '最后一条消息预览',
    last_message_at DATETIME COMMENT '最后消息时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_conversation_key (conversation_key),
    INDEX idx_user1 (user1_id),
    INDEX idx_user2 (user2_id)
);
```

#### 2.2 新增表：chat_message
```sql
CREATE TABLE IF NOT EXISTS chat_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    conversation_id BIGINT NOT NULL COMMENT '对话ID',
    sender_id BIGINT NOT NULL COMMENT '发送者ID',
    receiver_id BIGINT NOT NULL COMMENT '接收者ID',
    content TEXT NOT NULL COMMENT '消息内容',
    message_type VARCHAR(20) DEFAULT 'text' COMMENT '消息类型: text/image',
    is_read BOOLEAN DEFAULT FALSE COMMENT '是否已读',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_conversation (conversation_id, created_at DESC),
    INDEX idx_receiver (receiver_id, is_read),
    INDEX idx_sender (sender_id)
);
```

---

### 模块3：Redis 未读消息计数

#### 3.1 Redis Key 设计
| Key | 类型 | 说明 |
|-----|------|------|
| chat:unread:{userId} | Hash | 未读消息计数，field=conversationId, value=count |
| chat:unread:total:{userId} | String | 总未读数 |
| chat:online:{userId} | String | 用户在线状态（过期时间5分钟） |

#### 3.2 未读消息逻辑
- 发送消息时：Redis INCR `chat:unread:{receiverId}` 的 conversationId 字段
- 用户读取消息时：Redis DEL 对应字段，更新总未读数
- 用户上线时：设置 `chat:online:{userId}`，过期时间5分钟
- 用户下线时：删除在线状态

---

### 模块4：聊天 WebSocket Handler

#### 4.1 ChatWebSocketHandler
- 端点: `/ws/chat`
- 连接时需要 JWT 认证（通过 URL 参数或首条消息）
- 维护用户在线状态 Map<userId, WebSocketSession>

#### 4.2 消息类型
```java
// 客户端发送
{
    "action": "auth",      // 认证
    "token": "jwt_token"
}

{
    "action": "send",      // 发送消息
    "receiverId": 123,
    "content": "你好",
    "messageType": "text"
}

{
    "action": "read",      // 标记已读
    "conversationId": 456
}

{
    "action": "ping"       // 心跳
}

// 服务端推送
{
    "type": "message",     // 新消息
    "message": { ... }
}

{
    "type": "unread",      // 未读更新
    "conversationId": 456,
    "count": 0
}

{
    "type": "online",      // 好友上线
    "userId": 123
}

{
    "type": "offline",     // 好友下线
    "userId": 123
}
```

---

### 模块5：聊天 Service

#### 5.1 ChatService
```java
// 发送消息
ChatMessage sendMessage(Long senderId, Long receiverId, String content, String messageType)

// 获取对话列表（最近聊天的人）
List<ChatConversationVO> getConversationList(Long userId)

// 获取聊天记录
Page<ChatMessage> getChatHistory(Long conversationId, int page, int size)

// 获取或创建对话
ChatConversation getOrCreateConversation(Long userId1, Long userId2)

// 标记已读
void markAsRead(Long userId, Long conversationId)

// 获取未读消息数
Map<String, Object> getUnreadCounts(Long userId)

// 获取总未读数
int getTotalUnreadCount(Long userId)
```

---

### 模块6：聊天 Controller

#### 6.1 ChatController
- `GET /api/chat/conversations` - 获取对话列表
- `GET /api/chat/messages/{conversationId}` - 获取聊天记录
- `POST /api/chat/read/{conversationId}` - 标记已读
- `GET /api/chat/unread` - 获取未读消息数
- `GET /api/chat/contacts` - 获取通讯录（互关好友）

---

### 模块7：前端实现

#### 7.1 新增页面
- `ChatList.vue` - 对话列表页面
- `ChatRoom.vue` - 聊天室页面
- `Contacts.vue` - 通讯录页面

#### 7.2 修改页面
- 底部导航栏添加"消息"入口
- Profile 页面添加"通讯录"入口

#### 7.3 WebSocket 客户端
- 新增 `ChatWebSocketClient` 类
- 支持认证、发送消息、接收消息、心跳保活

#### 7.4 API 接口
```typescript
export const chatApi = {
  getConversations: () => request('get', '/chat/conversations'),
  getMessages: (conversationId: number, params?: { page?: number; size?: number }) =>
    request('get', `/chat/messages/${conversationId}`, undefined, { params }),
  markRead: (conversationId: number) => request('post', `/chat/read/${conversationId}`),
  getUnread: () => request('get', '/chat/unread'),
  getContacts: () => request('get', '/chat/contacts')
}
```

---

## 实现顺序

1. **阶段1：数据库设计**
   - 新增 chat_conversation 表
   - 新增 chat_message 表

2. **阶段2：实体类和 Repository**
   - ChatConversation 实体
   - ChatMessage 实体
   - 对应的 Repository

3. **阶段3：互关功能增强**
   - FollowService 添加 getMutualFriendIds 方法
   - FollowController 添加 /api/follow/mutual API

4. **阶段4：ChatService**
   - 消息发送
   - 对话列表
   - 聊天记录
   - 已读标记
   - 未读计数

5. **阶段5：ChatController**
   - REST API 实现

6. **阶段6：ChatWebSocketHandler**
   - WebSocket 连接管理
   - 消息推送
   - 在线状态管理

7. **阶段7：WebSocket 配置**
   - 注册新的 Handler
   - 配置端点

8. **阶段8：前端 API 和 WebSocket**
   - chatApi 接口定义
   - ChatWebSocketClient

9. **阶段9：前端页面**
   - ChatList.vue
   - ChatRoom.vue
   - Contacts.vue
   - 导航栏修改

10. **阶段10：测试与优化**

---

## 文件清单

### 后端新增文件
- `backend/.../model/ChatConversation.java`
- `backend/.../model/ChatMessage.java`
- `backend/.../repository/ChatConversationRepository.java`
- `backend/.../repository/ChatMessageRepository.java`
- `backend/.../service/chat/ChatService.java`
- `backend/.../controller/ChatController.java`
- `backend/.../websocket/ChatWebSocketHandler.java`
- `backend/.../dto/ChatMessageDTO.java`

### 后端修改文件
- `backend/.../config/WebSocketConfig.java` - 注册新的 Handler
- `backend/.../service/follow/FollowService.java` - 添加互关方法
- `backend/.../controller/FollowController.java` - 添加互关 API
- `backend/src/main/resources/db/schema.sql` - 新增表

### 前端新增文件
- `frontend/src/views/ChatList.vue`
- `frontend/src/views/ChatRoom.vue`
- `frontend/src/views/Contacts.vue`
- `frontend/src/websocket/chat.ts`

### 前端修改文件
- `frontend/src/api/index.ts` - 添加 chatApi
- `frontend/src/router/index.ts` - 添加路由
- `frontend/src/components/TabBar.vue` - 添加消息入口

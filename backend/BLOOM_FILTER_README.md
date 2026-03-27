# 用户相似度比较器 - 布隆过滤器实现

## 概述

用户相似度比较器是一个基于**布隆过滤器（Bloom Filter）**算法的推荐记录相似度计算系统。通过对用户的推荐参数进行多次哈希取值，生成一个固定长度的位数组（Bit Array），用于表示用户偏好特征。两个用户的位数组通过按位与（AND）运算后计算重叠位数，即可得出相似度。

### 核心特性

- **布隆过滤器机制**：使用 3 个 MurmurHash3 哈希函数将推荐参数映射到 256 位数组
- **FIFO 滑动窗口**：固定维护最近 10 条推荐记录
- **Redis 高速存储**：位数组和队列存储在 Redis 中
- **MySQL 持久化**：每小时自动同步到 MySQL 防止数据丢失
- **自动集成**：与推荐系统自动集成，推荐结果自动更新布隆过滤器

---

## 架构设计

### 数据流

```
用户触发推荐
     │
     ▼
┌─────────────────────────────────────────┐
│  1. 生成 recommendation_result 记录       │
└─────────────────────┬───────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────┐
│  2. 采集推荐时的 collected_params        │
│     将 param_value 拼接后哈希             │
└─────────────────────┬───────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────┐
│  3. FIFO 窗口管理                        │
│     队列 ≥ 10 时出队最旧记录，清除对应bits│
└─────────────────────┬───────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────┐
│  4. 布隆过滤器更新                        │
│     3个哈希位置设 1                       │
│     更新 Redis bit数组                    │
└─────────────────────┬───────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────┐
│  5. 定时任务（每小时）                    │
│     同步 Redis → MySQL                  │
└─────────────────────────────────────────┘
```

### Redis 数据结构

| Key Pattern | Type | 说明 |
|------------|------|------|
| `bloom:user:{userId}` | String (Hex) | 256 bits = 32 bytes 的位数组 |
| `bloom:queue:{userId}` | List | FIFO 队列，存储最近 10 个推荐记录 ID |
| `bloom:pending:sync` | Set | 待同步到 MySQL 的用户 ID 集合 |

### MySQL 数据表

#### user_bloom_filter（用户布隆过滤器表）

| 字段 | 类型 | 说明 |
|-----|------|------|
| id | BIGINT | 主键 |
| user_id | BIGINT | 用户 ID（唯一） |
| bit_array | VARBINARY(32) | 256 bits 位数组 |
| record_count | INT | 当前记录数 |
| last_record_id | VARCHAR(64) | 最新推荐记录 ID |
| updated_at | DATETIME | 更新时间 |
| created_at | DATETIME | 创建时间 |

#### bloom_sync_log（同步日志表）

| 字段 | 类型 | 说明 |
|-----|------|------|
| id | BIGINT | 主键 |
| user_id | BIGINT | 用户 ID |
| sync_type | VARCHAR(20) | 同步类型：redis_to_mysql / mysql_to_redis |
| status | VARCHAR(20) | 状态：success / failed |
| error_msg | TEXT | 错误信息 |
| synced_at | DATETIME | 同步时间 |

---

## 算法原理

### 哈希函数

使用改进的 **MurmurHash3** 算法，通过添加不同的盐值生成 3 个不同的哈希位置：

```java
H1 = murmurHash(paramValue) & 0xFF                    // 位置范围 0-255
H2 = murmurHash(paramValue + "bloom_salt1") & 0xFF
H3 = murmurHash(paramValue + "bloom_salt2") & 0xFF
```

### 位数组结构

```
256 bits = 32 bytes = 8 个 int
┌──────────┬──────────┬──────────┬──────────┬──────────┬──────────┬──────────┬──────────┐
│  0-31    │  32-63   │  64-95   │ 96-127   │ 128-159  │ 160-191  │ 192-223  │ 224-255  │
└──────────┴──────────┴──────────┴──────────┴──────────┴──────────┴──────────┴──────────┘
```

### 相似度计算

```
similarity = (bitArrayA AND bitArrayB).bitCount() / 256.0

结果范围: 0.0 ~ 1.0
- 0.0   : 完全不同（无重叠）
- 0.3   : 30% 重叠
- 0.7   : 70% 重叠（高度相似）
- 1.0   : 完全相同
```

### FIFO 窗口管理

```
初始状态: 队列 = []
推荐记录1: 队列 = [记录1],        位数组 = H(记录1)
推荐记录2: 队列 = [记录1,2],     位数组 = H(记录1) OR H(记录2)
...
推荐记录10: 队列 = [记录1-10],   位数组 = H(记录1) OR ... OR H(记录10)
推荐记录11: 队列 = [记录2-11],    位数组 = H(记录1) OR ... OR H(记录11) 清除记录1的bits
                                   实际上：H(记录1) 的3个位置减1，H(记录11) 的3个位置加1
```

> **注意**：布隆过滤器不支持删除操作（会误删其他记录）。当前实现采用精确清除模式，当能够获取到被删除记录的原始 paramValue 时，可以精确清除对应 bits。

---

## 配置参数

### application.yml

```yaml
bloom:
  bit-size: 256              # 位数组长度
  hash-functions: 3         # 哈希函数数量
  window-size: 10           # FIFO 窗口大小
  sync:
    enabled: true           # 是否启用同步
    cron: "0 0 * * * ?"     # Cron 表达式：每小时执行
    batch-size: 100         # 批量同步大小
```

### 参数说明

| 参数 | 默认值 | 说明 |
|-----|-------|------|
| bit-size | 256 | 位数组长度，建议 256-1024 |
| hash-functions | 3 | 哈希函数数量，建议 3-7 |
| window-size | 10 | FIFO 队列长度 |
| sync.cron | `0 0 * * * ?` | 每小时同步一次 |

---

## API 接口

### 1. 添加推荐记录

```
POST /api/bloom/add
```

**参数：**

| 参数 | 类型 | 必填 | 说明 |
|-----|------|-----|------|
| recordId | String | 是 | 推荐记录 ID |
| sessionId | String | 是 | 会话 ID |
| paramValue | String | 是 | 推荐参数值 |

**响应示例：**
```json
{
  "code": 200,
  "message": "推荐记录已添加",
  "data": null
}
```

> 此接口通常由系统内部自动调用，用户无需手动调用。

---

### 2. 获取与指定用户的相似度

```
GET /api/bloom/similarity/{targetUserId}
```

**路径参数：**

| 参数 | 类型 | 说明 |
|-----|------|------|
| targetUserId | Long | 目标用户 ID |

**响应示例：**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "similarity": 0.45,
    "percentage": 45
  }
}
```

---

### 3. 获取最相似的 K 个用户

```
GET /api/bloom/top-k/{k}
```

**路径参数：**

| 参数 | 类型 | 说明 |
|-----|------|------|
| k | int | 返回数量，范围 1-100 |

**响应示例：**
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "userId": 123,
      "nickname": "美食达人",
      "similarity": 0.85
    },
    {
      "userId": 456,
      "nickname": "吃货一枚",
      "similarity": 0.72
    }
  ]
}
```

---

### 4. 查看自己的布隆过滤器状态

```
GET /api/bloom/my
```

**响应示例：**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "userId": 1,
    "recordCount": 8,
    "recentRecords": ["1001", "1002", "1003", "1004", "1005", "1006", "1007", "1008"],
    "maxRecords": 10
  }
}
```

---

## 使用场景

### 1. 协同过滤推荐

通过找到与当前用户兴趣相似的其他用户，可以：

- **推荐相似用户喜欢的美食**
- **发现口味相投的好友**
- **构建用户兴趣画像**

### 2. 好友匹配

基于相似度分数，可以：

- **推荐潜在好友**（相似度 > 0.5）
- **显示口味匹配度**（如：你们有 72% 的相似度）
- **筛选高相似用户进行社交**

### 3. 内容分发优化

- **个性化 Feed 排序**：优先展示与用户相似的用户发布的内容
- **精准营销**：识别高相似用户群体进行定向推荐
- **冷启动优化**：新用户可以通过初始偏好快速找到相似群体

---

## 代码结构

```
backend/src/main/java/com/ai/food/
├── controller/
│   └── BloomController.java              # REST API 控制器
├── dto/
│   └── UserSimilarityDTO.java           # 相似度返回 DTO
├── model/
│   ├── BloomSyncLog.java                # 同步日志实体
│   └── UserBloomFilter.java             # 用户布隆过滤器实体
├── repository/
│   ├── BloomSyncLogRepository.java      # 同步日志 Repository
│   └── UserBloomFilterRepository.java   # 用户布隆过滤器 Repository
├── service/
│   └── bloom/
│       ├── BloomFilterRedisDao.java     # Redis 操作层
│       ├── BloomFilterService.java      # 核心服务接口
│       ├── BloomPersistenceService.java # 持久化服务接口
│       └── impl/
│           ├── BloomFilterServiceImpl.java      # 核心服务实现
│           └── BloomPersistenceServiceImpl.java # 持久化服务实现
└── job/
    └── BloomSyncJob.java                 # 定时同步任务
```

---

## 文件清单

### 新增文件

| 文件路径 | 说明 |
|---------|------|
| `model/UserBloomFilter.java` | 用户布隆过滤器实体类 |
| `model/BloomSyncLog.java` | 同步日志实体类 |
| `repository/UserBloomFilterRepository.java` | 用户布隆过滤器 Repository |
| `repository/BloomSyncLogRepository.java` | 同步日志 Repository |
| `service/bloom/BloomFilterRedisDao.java` | Redis 数据访问层 |
| `service/bloom/BloomFilterService.java` | 布隆过滤器核心服务接口 |
| `service/bloom/impl/BloomFilterServiceImpl.java` | 布隆过滤器核心服务实现 |
| `service/bloom/BloomPersistenceService.java` | 持久化服务接口 |
| `service/bloom/impl/BloomPersistenceServiceImpl.java` | 持久化服务实现 |
| `job/BloomSyncJob.java` | 定时同步任务 |
| `controller/BloomController.java` | REST API 控制器 |
| `dto/UserSimilarityDTO.java` | 相似度 DTO |

### 修改文件

| 文件路径 | 修改内容 |
|---------|---------|
| `db/schema.sql` | 新增 `user_bloom_filter` 和 `bloom_sync_log` 表 |
| `application.yml` | 新增 `bloom` 配置节点 |
| `config/QuartzConfig.java` | 新增 `bloomSyncJob` 定时任务配置 |
| `service/conversation/ConversationService.java` | 推荐结果保存后自动触发布隆过滤器更新 |

---

## 定时任务

### BloomSyncJob

- **执行频率**：每小时执行一次
- **执行内容**：将 Redis 中变更的位数组同步到 MySQL
- **Cron 表达式**：`0 0 * * * ?`

### CleanupSoftDeletedJob

- **执行频率**：每天凌晨 3:00 执行
- **执行内容**：清理软删除记录

---

## 注意事项

### 1. 布隆过滤器的局限性

- **假阳性**：可能将不相似的用户误判为相似（概率约 5-10%）
- **不支持删除**：当窗口移动时，需要原始 paramValue 才能精确清除
- **无法精确比较**：只能判断"可能相似"，不能判断"一定相似"

### 2. 数据一致性

- Redis 作为主要存储，提供高速读写
- MySQL 作为持久化层，每小时同步一次
- 故障恢复时，可从 MySQL 恢复到 Redis

### 3. 性能考虑

- 相似度计算时间复杂度：O(32) = O(1)
- Top-K 查询需要遍历所有用户，适合用户量 < 10 万的场景
- 大量用户场景建议增加缓存或使用近似算法

---

## 扩展方向

1. **分布式部署**：使用 Redis Cluster 支持多节点
2. **增量计算**：使用 Redis Bitmap 的 AND、OR 操作优化相似度计算
3. **分层索引**：按相似度阈值分层索引，减少全量扫描
4. **实时更新**：推荐点击行为实时更新位数组
5. **多维度画像**：支持多个布隆过滤器（如：口味、风格、价格敏感度）

---

## 术语表

| 术语 | 说明 |
|-----|------|
| Bloom Filter | 布隆过滤器，一种空间效率高的概率型数据结构 |
| Bit Array | 位数组，用于存储哈希映射结果 |
| MurmurHash | 一种非加密哈希算法，适合布隆过滤器 |
| FIFO | 先进先出队列 |
| False Positive | 假阳性，布隆过滤器可能的误判 |

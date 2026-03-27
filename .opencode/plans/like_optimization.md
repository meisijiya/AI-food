# 点赞功能优化实施计划

## 目标
优化点赞功能，实现高性能、高可用的异步处理架构。

## 架构设计

```
┌─────────┐    ┌─────────────┐    ┌─────────────┐    ┌──────────────┐    ┌─────────┐
│  前端   │───▶│  LikeController │───▶│  Lua脚本检查  │───▶│ Redis写入     │
└─────────┘    └─────────────┘    └─────────────┘    └──────────────┘    └─────────┘
                                                                    │
                    ┌───────────────────────────────────────────────┘
                    ▼
              ┌─────────────┐    ┌──────────────┐    ┌─────────┐
              │Redis Streams │───▶│ 异步消费者    │───▶│  MySQL  │
              └─────────────┘    └──────────────┘    └─────────┘
                    
         ┌─────────────────────────────────────────────┐
         │  HeavyKeeper (ZSET)   │  Caffeine本地缓存   │
         │  热点探测 + 时间片分散  │  热门数据缓存       │
         └─────────────────────────────────────────────┘
```

## 详细实施步骤

### 第一阶段：基础组件创建

#### 1.1 新增 Redis 配置和 Lua 脚本
**文件:** `config/RedisConfig.java`
```java
// 1. 新增 Lua 脚本 bean
@Bean
public RedisScript<Long> toggleLikeScript() {
    String script = """
        local postId = KEYS[1]
        local userId = ARGV[1]
        local countKey = ARGV[2]
        local isMember = redis.call('SISMEMBER', postId, userId)
        
        if isMember == 1 then
            redis.call('SREM', postId, userId)
            local newCount = redis.call('DECR', countKey)
            if newCount < 0 then redis.call('SET', countKey, 0) end
            return -1  -- unlike
        else
            redis.call('SADD', postId, userId)
            redis.call('INCR', countKey)
            return 1   -- like
        end
        """;
    return new DefaultRedisScript<>(script, Long.class);
}

// 2. 新增 StringRedisTemplate 专用 bean (用于 Lua 脚本)
@Bean
public StringRedisTemplate stringRedisTemplate(...) { ... }
```

#### 1.2 新增 Caffeine 热点缓存配置
**文件:** `config/CaffeineConfig.java`
```java
@Bean("hotPostLikeCache")
public Cache<Long, Long> hotPostLikeCache() {
    return Caffeine.newBuilder()
            .expireAfterWrite(30, TimeUnit.SECONDS)  // 热点数据短TTL
            .maximumSize(10000)
            .build();
}

@Bean("hotPostLikeStatusCache")  
public Cache<String, Boolean> hotPostLikeStatusCache() {
    return Caffeine.newBuilder()
            .expireAfterWrite(60, TimeUnit.SECONDS)
            .maximumSize(50000)
            .build();
}
```

### 第二阶段：核心服务实现

#### 2.1 LikeService 重构
**文件:** `service/LikeService.java` (新建)

| 方法 | 说明 |
|------|------|
| `toggleLikeAsync(Long postId, Long userId)` | 异步点赞入口，写入 Redis 后发送 Stream 事件 |
| `syncLikeCount(Long postId)` | 同步热点帖子的点赞计数到 Caffeine |
| `getLikeCount(Long postId)` | 获取点赞数（优先 Caffeine） |
| `isLiked(Long postId, Long userId)` | 检查是否已点赞 |

**核心逻辑:**
1. 执行 Lua 脚本原子操作
2. HeavyKeeper 计数更新 (ZINCRBY)
3. 检查是否超过时间片阈值，决定是否立即返回或延迟
4. 写入 Redis Stream 事件

#### 2.2 HeavyKeeper 热点探测服务
**文件:** `service/HeavyKeeperService.java` (新建)

使用 Redis ZSET 实现 Top-K 频率统计算法：
- Key: `hk:like:count` (ZSET, score=频率计数)
- Key: `hk:like:decay` (ZSET, score=带时间衰减的计数)
- 定期 decay 减少计数，探测真正的热点

```java
// 核心逻辑
public void recordAccess(Long postId) {
    stringRedisTemplate.opsForZSet().incrementScore("hk:like:count", postId.toString(), 1);
}

public List<Long> getTopKHotPosts(int k) {
    Set<ZSetOperations.TypedTuple<String>> topK = 
        stringRedisTemplate.opsForZSet().reverseRangeWithScores("hk:like:count", 0, k-1);
    return topK.stream().map(t -> Long.parseLong(t.getValue())).toList();
}
```

#### 2.3 时间片分散压力
**策略:** 将点赞事件按时间片（如 100ms 一个 bucket）分散写入

```java
// 时间片 bucket key
private String getBucketKey(Long postId) {
    long bucket = System.currentTimeMillis() / 100;  // 100ms bucket
    return "feed:like:bucket:" + postId + ":" + bucket;
}
```

#### 2.4 Redis Stream 事件发送
**文件:** `service/LikeStreamProducer.java` (新建)

```java
@RequiredArgsConstructor
public class LikeStreamProducer {
    private final StringRedisTemplate template;
    
    public void sendLikeEvent(Long postId, Long userId, boolean liked) {
        Map<String, String> event = new HashMap<>();
        event.put("postId", postId.toString());
        event.put("userId", userId.toString());
        event.put("liked", String.valueOf(liked));
        event.put("timestamp", String.valueOf(System.currentTimeMillis()));
        
        template.opsForStream().add("stream:like:events", event);
    }
}
```

### 第三阶段：异步消费者

#### 3.1 Redis Stream 消费者
**文件:** `service/LikeStreamConsumer.java` (新建)

```java
@Scheduled(fixedDelay = 100)  // 每100ms消费一次
public void consumeLikeEvents() {
    // 1. XREADGROUP 读取批次事件
    // 2. 批量更新 MySQL
    // 3. XACK 确认消息
}
```

**关键特性:**
- 消费者组 (Consumer Group) 确保消息不丢失
- 批量更新数据库减少 IO
- 失败重试机制

#### 3.2 异步批量写入数据库
```java
@Transactional
public void batchUpdateDb(List<LikeEvent> events) {
    // 按 postId 分组
    // 批量 UPDATE feed_post SET like_count = ? WHERE id = ?
}
```

### 第四阶段：接口适配

#### 4.1 LikeController 改造
**文件:** `controller/LikeController.java` (新建)

```java
@PostMapping("/like/{postId}")
public ApiResponse<Map<String, Object>> toggleLike(@PathVariable Long postId) {
    Long userId = getCurrentUserId();
    // 异步处理，返回快速响应
    likeService.toggleLikeAsync(postId, userId);
    return ApiResponse.success(Map.of("queued", true));
}
```

#### 4.2 获取点赞状态的优化接口
```java
@GetMapping("/like/{postId}/status")
public ApiResponse<Map<String, Object>> getLikeStatus(@PathVariable Long postId) {
    Long userId = getCurrentUserId();
    boolean liked = likeService.isLiked(postId, userId);
    long count = likeService.getLikeCount(postId);
    return ApiResponse.success(Map.of("liked", liked, "count", count));
}
```

### 第五阶段：热榜与缓存联动

#### 5.1 定时刷新 Caffeine 热点缓存
```java
@Scheduled(fixedRate = 5000)  // 每5秒
public void refreshHotPostCache() {
    List<Long> hotPosts = heavyKeeperService.getTopKHotPosts(100);
    for (Long postId : hotPosts) {
        Long count = likeService.getLikeCountFromRedis(postId);
        hotPostLikeCache.put(postId, count);
    }
}
```

## 文件清单

| 操作 | 文件路径 |
|------|----------|
| 新建 | `service/LikeService.java` |
| 新建 | `service/HeavyKeeperService.java` |
| 新建 | `service/LikeStreamProducer.java` |
| 新建 | `service/LikeStreamConsumer.java` |
| 新建 | `controller/LikeController.java` |
| 修改 | `config/RedisConfig.java` |
| 修改 | `config/CaffeineConfig.java` |
| 修改 | `FeedService.java` (移除旧逻辑) |

## Redis Key 设计

| Key Pattern | Type | 说明 |
|-------------|------|------|
| `feed:like:{postId}` | SET | 点赞用户集合 |
| `feed:like:count:{postId}` | STRING | 点赞计数 |
| `hk:like:count` | ZSET | HeavyKeeper 频率统计 |
| `hk:like:decay` | ZSET | 带时间衰减的频率 |
| `feed:like:cache:{postId}` | STRING | Caffeine 缓存镜像 |
| `stream:like:events` | STREAM | 点赞事件流 |
| `cg:like:consumers` | - | 消费者组 |

## 配置参数建议

```yaml
like:
  time-bucket-ms: 100        # 时间片大小
  stream-batch-size: 100     # 批量消费大小
  cache-ttl-seconds: 30      # Caffeine 缓存 TTL
  hk-decay-interval: 60000   # HeavyKeeper 衰减间隔(ms)
  hk-top-k: 100              # Top K 热点数量
```

## 实施顺序

1. **RedisConfig + Lua脚本** (基础)
2. **CaffeineConfig** (缓存层)
3. **HeavyKeeperService** (热点探测)
4. **LikeStreamProducer** (事件发送)
5. **LikeStreamConsumer** (异步消费)
6. **LikeService** (核心逻辑)
7. **LikeController** (接口适配)
8. **FeedService** (清理旧逻辑)

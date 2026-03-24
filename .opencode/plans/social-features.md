# 社交功能实现计划

## 目标
实现好友关注/粉丝功能、推荐记录可见性控制、热度排行榜、大厅副导航栏（热榜、友榜）。

## 设计决策
- **关注关系**：单向关注（类似微博），A关注B，B不一定关注A
- **可见性**：
  - `public`: 发布到大厅，所有人可见
  - `friends`: 仅发布给好友（粉丝），大厅不显示
- **热度值计算**：浏览次数 + 点赞数×3 + 评论数×5

## 功能模块

### 模块1：好友关注/粉丝系统

#### 1.1 数据库设计
- 新增 `user_follow` 表
  - id, follower_id, following_id, created_at
  - 唯一索引：follower_id + following_id
  - 索引：follower_id, following_id

#### 1.2 后端实现
- 新增 `UserFollow` 实体类
- 新增 `UserFollowRepository`
- 新增 `FollowController` + `FollowService`
  - POST /api/follow/{userId} - 关注/取关
  - GET /api/follow/following - 我的关注列表
  - GET /api/follow/followers - 我的粉丝列表
  - GET /api/follow/check/{userId} - 检查是否已关注

#### 1.3 前端实现
- Profile 页面添加关注数/粉丝数展示
- 新增 FollowList 页面（Tab切换关注/粉丝列表）
- 用户主页支持关注/取关操作

---

### 模块2：推荐记录可见性控制

#### 2.1 数据库修改
- `feed_post` 表新增 `visibility` 字段
  - ENUM('public', 'friends') DEFAULT 'public'
  - public: 发布到大厅，所有人可见
  - friends: 仅好友可见（友榜中显示）

#### 2.2 后端修改
- 修改 `FeedService.publishPost()` 支持 visibility 参数
- 修改 `FeedService.getFeedList()` 
  - 大厅只显示 visibility='public'
  - 新增 getFriendFeedList() 获取好友的推荐（包括 public 和 friends）

#### 2.3 前端修改
- 发布时添加可见性选择器
- 大厅页面根据 Tab 显示不同内容

---

### 模块3：Redis热度排行榜

#### 3.1 Redis数据结构
```
feed:hot:rank - Sorted Set
  - member: postId
  - score: 热度值（浏览次数）
```

#### 3.2 后端实现
- 修改 `FeedService.getFeedDetail()` 
  - 每次访问详情时 ZINCRBY feed:hot:rank 1 {postId}
- 新增 `FeedService.getHotRank()`
  - ZREVRANGE feed:hot:rank 0 19 获取 Top 20
  - 根据 postId 从数据库获取详细信息
- 新增 API: GET /api/feed/hot-rank

#### 3.3 注意事项
- 热度数据持久化到数据库，Redis 作为缓存
- 定期同步热度数据到 feed_post 表的 view_count 字段

---

### 模块4：大厅副导航栏

#### 4.1 前端设计
- Feed 页面添加副导航栏
  - Tab1: 大厅（现有公开推荐列表）
  - Tab2: 热榜（热度排行榜 Top 20）
  - Tab3: 友榜（好友推荐列表）

#### 4.2 友榜数据来源
- Redis List 结构存储好友推荐
- Key: `feed:friend:{userId}`
- Value: 简化的帖子信息 JSON（最小化内存占用）
  - postId, userId, foodName, thumbnailUrl, nickname, avatar, publishedAt
- 最多存储 100 条（FIFO）
- 点击后从数据库加载完整详情

#### 4.3 后端实现
- 修改 `FeedService.publishPost()`
  - 获取发布者的所有粉丝
  - 将帖子摘要推送到每个粉丝的 `feed:friend:{fanId}`
  - 如果超过 100 条，移除最早的记录
- 新增 `FeedService.getFriendFeedList()`
  - 从 `feed:friend:{userId}` 获取列表
  - 分页支持

---

### 模块5：个人中心增强

#### 5.1 Profile 页面修改
- 添加关注数、粉丝数展示
- 点击可跳转到关注/粉丝列表页

#### 5.2 新增页面
- FollowList.vue - 关注/粉丝列表页面
  - Tab 切换：关注的人 / 粉丝
  - 列表展示用户头像、昵称、关注按钮
  - 支持关注/取关操作

---

## 实现顺序

1. **阶段1：数据库设计与实体类** 
   - 创建 user_follow 表
   - 修改 feed_post 表
   - 创建实体类和 Repository

2. **阶段2：关注/粉丝后端 API**
   - FollowController, FollowService
   - 关注/取关、列表查询

3. **阶段3：可见性控制**
   - 修改发布 API 支持 visibility
   - 修改列表 API 过滤逻辑

4. **阶段4：热度排行榜后端**
   - Redis Sorted Set 集成
   - 热榜 API

5. **阶段5：友榜数据同步**
   - 发布时推送到粉丝 Redis List
   - 友榜查询 API

6. **阶段6：前端页面**
   - Profile 页面关注/粉丝展示
   - FollowList 页面
   - Feed 页面副导航栏
   - 发布时可见性选择

7. **阶段7：测试与优化**
   - 功能测试
   - 性能优化

---

## Redis Key 设计

| Key | 类型 | 说明 |
|-----|------|------|
| feed:hot:rank | Sorted Set | 热度排行榜，member=postId, score=热度值 |
| feed:friend:{userId} | List | 好友推荐列表，最多100条 |
| follow:following:{userId} | Set | 用户关注列表缓存 |
| follow:followers:{userId} | Set | 用户粉丝列表缓存 |

---

## 数据库变更

### 新增表：user_follow
```sql
CREATE TABLE IF NOT EXISTS user_follow (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    follower_id BIGINT NOT NULL COMMENT '关注者ID',
    following_id BIGINT NOT NULL COMMENT '被关注者ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_follow (follower_id, following_id),
    INDEX idx_follower (follower_id),
    INDEX idx_following (following_id)
);
```

### 修改表：feed_post
```sql
ALTER TABLE feed_post 
ADD COLUMN visibility VARCHAR(20) DEFAULT 'public' COMMENT 'public/friends',
ADD COLUMN view_count INT DEFAULT 0 COMMENT '浏览次数',
ADD INDEX idx_visibility (visibility);
```

---

## 详细代码实现

### 1. 数据库变更

#### schema.sql 新增表
```sql
-- 用户关注表
CREATE TABLE IF NOT EXISTS user_follow (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    follower_id BIGINT NOT NULL COMMENT '关注者ID',
    following_id BIGINT NOT NULL COMMENT '被关注者ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_follow (follower_id, following_id),
    INDEX idx_follower (follower_id),
    INDEX idx_following (following_id)
);
```

#### schema.sql 修改 feed_post 表
```sql
-- 在 feed_post 表定义中添加
visibility VARCHAR(20) DEFAULT 'public' COMMENT 'public/friends',
view_count INT DEFAULT 0 COMMENT '浏览次数',
INDEX idx_visibility (visibility)
```

---

### 2. 后端新增文件

#### UserFollow.java
```java
package com.ai.food.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "user_follow", indexes = {
    @Index(name = "idx_follower", columnList = "follower_id"),
    @Index(name = "idx_following", columnList = "following_id")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_follow", columnNames = {"follower_id", "following_id"})
})
public class UserFollow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "follower_id", nullable = false)
    private Long followerId;

    @Column(name = "following_id", nullable = false)
    private Long followingId;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
```

#### UserFollowRepository.java
```java
package com.ai.food.repository;

import com.ai.food.model.UserFollow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserFollowRepository extends JpaRepository<UserFollow, Long> {
    
    Optional<UserFollow> findByFollowerIdAndFollowingId(Long followerId, Long followingId);
    
    List<UserFollow> findByFollowerId(Long followerId);
    
    List<UserFollow> findByFollowingId(Long followingId);
    
    long countByFollowerId(Long followerId);
    
    long countByFollowingId(Long followingId);
    
    void deleteByFollowerIdAndFollowingId(Long followerId, Long followingId);
    
    boolean existsByFollowerIdAndFollowingId(Long followerId, Long followingId);
    
    @Query("SELECT uf.followingId FROM UserFollow uf WHERE uf.followerId = :userId")
    List<Long> findFollowingIdsByUserId(@Param("userId") Long userId);
    
    @Query("SELECT uf.followerId FROM UserFollow uf WHERE uf.followingId = :userId")
    List<Long> findFollowerIdsByUserId(@Param("userId") Long userId);
}
```

#### FollowController.java
```java
package com.ai.food.controller;

import com.ai.food.dto.ApiResponse;
import com.ai.food.service.follow.FollowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/follow")
@RequiredArgsConstructor
public class FollowController {

    private final FollowService followService;

    @PostMapping("/{userId}")
    public ApiResponse<Map<String, Object>> toggleFollow(@PathVariable Long userId) {
        Long currentUserId = getCurrentUserId();
        if (currentUserId.equals(userId)) {
            return ApiResponse.error("不能关注自己");
        }
        Map<String, Object> result = followService.toggleFollow(currentUserId, userId);
        return ApiResponse.success(result);
    }

    @GetMapping("/following")
    public ApiResponse<Map<String, Object>> getFollowingList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = getCurrentUserId();
        Map<String, Object> result = followService.getFollowingList(userId, page, size);
        return ApiResponse.success(result);
    }

    @GetMapping("/followers")
    public ApiResponse<Map<String, Object>> getFollowersList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = getCurrentUserId();
        Map<String, Object> result = followService.getFollowersList(userId, page, size);
        return ApiResponse.success(result);
    }

    @GetMapping("/check/{userId}")
    public ApiResponse<Map<String, Object>> checkFollow(@PathVariable Long userId) {
        Long currentUserId = getCurrentUserId();
        boolean isFollowing = followService.isFollowing(currentUserId, userId);
        return ApiResponse.success(Map.of("isFollowing", isFollowing));
    }

    @GetMapping("/stats/{userId}")
    public ApiResponse<Map<String, Object>> getFollowStats(@PathVariable Long userId) {
        Map<String, Object> result = followService.getFollowStats(userId);
        return ApiResponse.success(result);
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return Long.parseLong(auth.getPrincipal().toString());
    }
}
```

#### FollowService.java
```java
package com.ai.food.service.follow;

import com.ai.food.model.SysUser;
import com.ai.food.model.UserFollow;
import com.ai.food.repository.UserFollowRepository;
import com.ai.food.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class FollowService {

    private final UserFollowRepository userFollowRepository;
    private final UserRepository userRepository;

    @Transactional
    public Map<String, Object> toggleFollow(Long followerId, Long followingId) {
        Optional<UserFollow> existing = userFollowRepository.findByFollowerIdAndFollowingId(followerId, followingId);
        
        boolean isFollowing;
        if (existing.isPresent()) {
            userFollowRepository.delete(existing.get());
            isFollowing = false;
            log.info("User {} unfollowed user {}", followerId, followingId);
        } else {
            UserFollow follow = new UserFollow();
            follow.setFollowerId(followerId);
            follow.setFollowingId(followingId);
            userFollowRepository.save(follow);
            isFollowing = true;
            log.info("User {} followed user {}", followerId, followingId);
        }

        long followerCount = userFollowRepository.countByFollowingId(followingId);
        
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("isFollowing", isFollowing);
        result.put("followerCount", followerCount);
        return result;
    }

    public Map<String, Object> getFollowingList(Long userId, int page, int size) {
        List<Long> followingIds = userFollowRepository.findFollowingIdsByUserId(userId);
        
        int start = page * size;
        int end = Math.min(start + size, followingIds.size());
        
        List<Map<String, Object>> items = new ArrayList<>();
        if (start < followingIds.size()) {
            List<Long> pageIds = followingIds.subList(start, end);
            for (Long followingId : pageIds) {
                userRepository.findById(followingId).ifPresent(user -> {
                    items.add(buildUserMap(user, true));
                });
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", items);
        result.put("page", page);
        result.put("size", size);
        result.put("total", followingIds.size());
        return result;
    }

    public Map<String, Object> getFollowersList(Long userId, int page, int size) {
        List<Long> followerIds = userFollowRepository.findFollowerIdsByUserId(userId);
        
        int start = page * size;
        int end = Math.min(start + size, followerIds.size());
        
        Set<Long> myFollowingIds = new HashSet<>(userFollowRepository.findFollowingIdsByUserId(userId));
        
        List<Map<String, Object>> items = new ArrayList<>();
        if (start < followerIds.size()) {
            List<Long> pageIds = followerIds.subList(start, end);
            for (Long followerId : pageIds) {
                userRepository.findById(followerId).ifPresent(user -> {
                    boolean isFollowing = myFollowingIds.contains(followerId);
                    items.add(buildUserMap(user, isFollowing));
                });
                }
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", items);
        result.put("page", page);
        result.put("size", size);
        result.put("total", followerIds.size());
        return result;
    }

    public boolean isFollowing(Long followerId, Long followingId) {
        return userFollowRepository.existsByFollowerIdAndFollowingId(followerId, followingId);
    }

    public Map<String, Object> getFollowStats(Long userId) {
        long followingCount = userFollowRepository.countByFollowerId(userId);
        long followerCount = userFollowRepository.countByFollowingId(userId);
        
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("followingCount", followingCount);
        result.put("followerCount", followerCount);
        return result;
    }

    public List<Long> getFollowerIds(Long userId) {
        return userFollowRepository.findFollowerIdsByUserId(userId);
    }

    private Map<String, Object> buildUserMap(SysUser user, boolean isFollowing) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("userId", user.getId());
        map.put("nickname", user.getNickname() != null ? user.getNickname() : user.getUsername());
        map.put("avatar", user.getAvatar());
        map.put("isFollowing", isFollowing);
        return map;
    }
}
```

---

### 3. 后端修改文件

#### FeedPost.java 修改
```java
// 添加字段
@Column(name = "visibility", length = 20)
private String visibility = "public";

@Column(name = "view_count")
private Integer viewCount = 0;
```

#### FeedPostRepository.java 修改
```java
// 添加方法
Page<FeedPost> findByVisibilityOrderByPublishedAtDesc(String visibility, Pageable pageable);

@Query("SELECT f FROM FeedPost f WHERE f.visibility = 'public' AND " +
       "(:foodName IS NULL OR f.foodName LIKE %:foodName%) AND " +
       "(:paramValue IS NULL OR f.collectedParams LIKE %:paramValue%) " +
       "ORDER BY f.publishedAt DESC")
Page<FeedPost> findPublicByFilters(@Param("foodName") String foodName,
                                   @Param("paramValue") String paramValue,
                                   Pageable pageable);

@Query("SELECT f FROM FeedPost f WHERE f.userId IN :userIds " +
       "ORDER BY f.publishedAt DESC")
Page<FeedPost> findByUserIdsOrderByPublishedAtDesc(@Param("userIds") List<Long> userIds, Pageable pageable);

List<FeedPost> findByIdIn(List<Long> ids);
```

#### FeedController.java 修改
```java
// 修改 publishPost 方法
@PostMapping("/publish")
public ApiResponse<Map<String, Object>> publishPost(@RequestBody Map<String, String> body) {
    String sessionId = body.get("sessionId");
    if (sessionId == null || sessionId.isBlank()) {
        return ApiResponse.error("请提供会话ID");
    }
    Long userId = getCurrentUserId();
    String commentPreview = body.get("commentPreview");
    String visibility = body.get("visibility"); // public/friends
    if (visibility == null || (!visibility.equals("public") && !visibility.equals("friends"))) {
        visibility = "public";
    }
    Map<String, Object> result = feedService.publishPost(userId, sessionId, commentPreview, visibility);
    return ApiResponse.success("发布成功", result);
}

// 修改 getFeedList 方法，只显示 public
@GetMapping("/list")
public ApiResponse<Map<String, Object>> getFeedList(...) {
    Map<String, Object> result = feedService.getPublicFeedList(page, size, foodName, paramName, paramValue);
    return ApiResponse.success(result);
}

// 新增热榜 API
@GetMapping("/hot-rank")
public ApiResponse<Map<String, Object>> getHotRank() {
    Map<String, Object> result = feedService.getHotRank();
    return ApiResponse.success(result);
}

// 新增友榜 API
@GetMapping("/friend-feed")
public ApiResponse<Map<String, Object>> getFriendFeedList(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size) {
    Long userId = getCurrentUserId();
    Map<String, Object> result = feedService.getFriendFeedList(userId, page, size);
    return ApiResponse.success(result);
}
```

#### FeedService.java 修改
```java
// 新增常量
private static final String HOT_RANK_KEY = "feed:hot:rank";
private static final String FRIEND_FEED_KEY = "feed:friend:";

// 修改 publishPost 方法
@Transactional
public Map<String, Object> publishPost(Long userId, String sessionId, String commentPreview, String visibility) {
    // ... 现有逻辑 ...
    post.setVisibility(visibility);
    
    FeedPost saved = feedPostRepository.save(post);
    
    // 推送到粉丝的友榜
    pushToFollowersFeeds(saved, userId);
    
    return buildPostMap(saved);
}

// 修改 getFeedList 为 getPublicFeedList
public Map<String, Object> getPublicFeedList(int page, int size, String foodName, String paramName, String paramValue) {
    // 只查询 visibility='public' 的帖子
    Page<FeedPost> postPage = feedPostRepository.findPublicByFilters(
            foodName, searchParamValue, pageable);
    // ... 其余逻辑不变 ...
}

// 新增 getHotRank 方法
public Map<String, Object> getHotRank() {
    // 从 Redis 获取 Top 20
    Set<ZSetOperations.TypedTuple<String>> hotPosts = stringRedisTemplate.opsForZSet()
            .reverseRangeWithScores(HOT_RANK_KEY, 0, 19);
    
    if (hotPosts == null || hotPosts.isEmpty()) {
        return Map.of("items", new ArrayList<>());
    }
    
    List<Long> postIds = new ArrayList<>();
    Map<Long, Double> scoreMap = new HashMap<>();
    for (ZSetOperations.TypedTuple<String> tuple : hotPosts) {
        Long postId = Long.parseLong(tuple.getValue());
        postIds.add(postId);
        scoreMap.put(postId, tuple.getScore());
    }
    
    List<FeedPost> posts = feedPostRepository.findByIdIn(postIds);
    List<Map<String, Object>> items = new ArrayList<>();
    for (FeedPost post : posts) {
        Map<String, Object> item = buildPostMap(post);
        item.put("hotScore", scoreMap.get(post.getId()));
        enrichUserInfo(item, post.getUserId());
        items.add(item);
    }
    
    return Map.of("items", items);
}

// 新增 getFriendFeedList 方法
public Map<String, Object> getFriendFeedList(Long userId, int page, int size) {
    String key = FRIEND_FEED_KEY + userId;
    long start = page * size;
    long end = start + size - 1;
    
    List<String> feedJsons = stringRedisTemplate.opsForList().range(key, start, end);
    if (feedJsons == null) {
        return Map.of("items", new ArrayList<>());
    }
    
    List<Map<String, Object>> items = new ArrayList<>();
    for (String json : feedJsons) {
        try {
            Map<String, Object> item = objectMapper.readValue(json, Map.class);
            items.add(item);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse friend feed item", e);
        }
    }
    
    Long total = stringRedisTemplate.opsForList().size(key);
    
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("items", items);
    result.put("page", page);
    result.put("size", size);
    result.put("total", total != null ? total : 0);
    return result;
}

// 新增 pushToFollowersFeeds 方法
private void pushToFollowersFeeds(FeedPost post, Long userId) {
    List<Long> followerIds = followService.getFollowerIds(userId);
    
    // 构建最小化的摘要
    Map<String, Object> summary = new LinkedHashMap<>();
    summary.put("postId", post.getId());
    summary.put("userId", post.getUserId());
    summary.put("foodName", post.getFoodName());
    summary.put("thumbnailUrl", post.getThumbnailUrl());
    summary.put("publishedAt", post.getPublishedAt().toString());
    
    try {
        String summaryJson = objectMapper.writeValueAsString(summary);
        
        for (Long followerId : followerIds) {
            String key = FRIEND_FEED_KEY + followerId;
            // 从左侧推入（最新的在前）
            stringRedisTemplate.opsForList().leftPush(key, summaryJson);
            // 保持最多 100 条
            stringRedisTemplate.opsForList().trim(key, 0, 99);
        }
    } catch (JsonProcessingException e) {
        log.error("Failed to serialize feed summary", e);
    }
}

// 修改 getFeedDetail，增加热度计数
public Map<String, Object> getFeedDetail(Long postId, Long currentUserId) {
    // ... 现有逻辑 ...
    
    // 增加热度
    stringRedisTemplate.opsForZSet().incrementScore(HOT_RANK_KEY, postId.toString(), 1);
    
    // 更新数据库 view_count
    feedPostRepository.findById(postId).ifPresent(post -> {
        post.setViewCount(post.getViewCount() + 1);
        feedPostRepository.save(post);
    });
    
    return result;
}

// 修改 toggleLike，增加热度
public Map<String, Object> toggleLike(Long postId, Long userId) {
    // ... 现有逻辑 ...
    
    if (liked) {
        stringRedisTemplate.opsForZSet().incrementScore(HOT_RANK_KEY, postId.toString(), 3);
    }
    
    return result;
}

// 修改 addComment，增加热度
public Map<String, Object> addComment(Long postId, Long userId, String content) {
    // ... 现有逻辑 ...
    
    stringRedisTemplate.opsForZSet().incrementScore(HOT_RANK_KEY, postId.toString(), 5);
    
    return result;
}
```

---

### 4. 前端实现

#### api/index.ts 新增接口
```typescript
// 关注相关接口
export const followApi = {
  toggleFollow: (userId: number) => request('post', `/follow/${userId}`),
  getFollowingList: (params?: { page?: number; size?: number }) =>
    request('get', '/follow/following', undefined, { params }),
  getFollowersList: (params?: { page?: number; size?: number }) =>
    request('get', '/follow/followers', undefined, { params }),
  checkFollow: (userId: number) => request('get', `/follow/check/${userId}`),
  getFollowStats: (userId: number) => request('get', `/follow/stats/${userId}`)
}

// 修改 feedApi
export const feedApi = {
  // ... 现有接口 ...
  publish: (data: { sessionId: string; commentPreview?: string; visibility?: string }) => 
    request('post', '/feed/publish', data),
  getHotRank: () => request('get', '/feed/hot-rank'),
  getFriendFeed: (params?: { page?: number; size?: number }) =>
    request('get', '/feed/friend-feed', undefined, { params })
}
```

#### router/index.ts 新增路由
```typescript
{
  path: '/follow',
  name: 'FollowList',
  component: () => import('@/views/FollowList.vue'),
  meta: { requiresAuth: true }
}
```

#### Feed.vue 副导航栏修改
```vue
<template>
  <div class="feed-container">
    <!-- Header -->
    <div class="feed-header">
      <h1 class="page-title"><em>大厅</em></h1>
    </div>

    <!-- Sub Navigation -->
    <div class="sub-nav">
      <button 
        class="sub-nav-item" 
        :class="{ active: activeTab === 'feed' }"
        @click="switchTab('feed')"
      >
        大厅
      </button>
      <button 
        class="sub-nav-item" 
        :class="{ active: activeTab === 'hot' }"
        @click="switchTab('hot')"
      >
        热榜
      </button>
      <button 
        class="sub-nav-item" 
        :class="{ active: activeTab === 'friend' }"
        @click="switchTab('friend')"
      >
        友榜
      </button>
    </div>

    <!-- Tab Content -->
    <div v-if="activeTab === 'feed'">
      <!-- 现有大厅内容 -->
    </div>
    
    <div v-else-if="activeTab === 'hot'">
      <!-- 热榜内容 -->
      <div class="hot-rank-list">
        <div v-for="(item, index) in hotRankList" :key="item.id" class="hot-rank-item">
          <span class="rank-num" :class="{ 'top-3': index < 3 }">{{ index + 1 }}</span>
          <div class="rank-content" @click="router.push('/feed/' + item.id)">
            <div class="rank-food">{{ item.foodName }}</div>
            <div class="rank-score">热度 {{ item.hotScore }}</div>
          </div>
        </div>
      </div>
    </div>
    
    <div v-else-if="activeTab === 'friend'">
      <!-- 友榜内容 -->
      <div class="friend-feed-list">
        <div v-for="item in friendFeedList" :key="item.postId" class="friend-feed-item">
          <!-- 好友推荐卡片 -->
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
const activeTab = ref('feed')
const hotRankList = ref<any[]>([])
const friendFeedList = ref<any[]>([])

function switchTab(tab: string) {
  activeTab.value = tab
  if (tab === 'hot') fetchHotRank()
  if (tab === 'friend') fetchFriendFeed()
}

async function fetchHotRank() {
  const res = await feedApi.getHotRank()
  hotRankList.value = res?.items || []
}

async function fetchFriendFeed() {
  const res = await feedApi.getFriendFeed({ page: 0, size: 20 })
  friendFeedList.value = res?.items || []
}
</script>
```

#### Profile.vue 添加关注/粉丝统计
```vue
<!-- Stats Card 修改 -->
<div class="stats-card">
  <div class="stat-item" @click="router.push('/follow?type=following')">
    <div class="stat-value">{{ followStats.followingCount }}</div>
    <div class="stat-label">关注</div>
  </div>
  <div class="stat-item" @click="router.push('/follow?type=followers')">
    <div class="stat-value">{{ followStats.followerCount }}</div>
    <div class="stat-label">粉丝</div>
  </div>
  <div class="stat-item">
    <div class="stat-value">{{ conversationCount }}</div>
    <div class="stat-label">推荐记录</div>
  </div>
</div>
```

#### FollowList.vue 新页面
```vue
<template>
  <div class="follow-list-container">
    <div class="page-header">
      <button class="back-btn" @click="router.back()">
        <svg>...</svg>
      </button>
      <h1 class="page-title">{{ activeTab === 'following' ? '关注' : '粉丝' }}</h1>
    </div>

    <div class="tab-bar">
      <button 
        class="tab-item" 
        :class="{ active: activeTab === 'following' }"
        @click="switchTab('following')"
      >
        关注 {{ followStats.followingCount }}
      </button>
      <button 
        class="tab-item" 
        :class="{ active: activeTab === 'followers' }"
        @click="switchTab('followers')"
      >
        粉丝 {{ followStats.followerCount }}
      </button>
    </div>

    <div class="user-list">
      <div v-for="user in userList" :key="user.userId" class="user-item">
        <div class="user-avatar">
          <img v-if="user.avatar" :src="user.avatar" />
          <span v-else>{{ user.nickname?.charAt(0) }}</span>
        </div>
        <div class="user-info">
          <div class="user-name">{{ user.nickname }}</div>
        </div>
        <button 
          class="follow-btn"
          :class="{ following: user.isFollowing }"
          @click="handleToggleFollow(user)"
        >
          {{ user.isFollowing ? '已关注' : '关注' }}
        </button>
      </div>
    </div>
  </div>
</template>
```

---

## 文件清单

### 后端新增文件
- `backend/src/main/java/com/ai/food/model/UserFollow.java`
- `backend/src/main/java/com/ai/food/repository/UserFollowRepository.java`
- `backend/src/main/java/com/ai/food/controller/FollowController.java`
- `backend/src/main/java/com/ai/food/service/follow/FollowService.java`

### 后端修改文件
- `backend/src/main/resources/db/schema.sql` - 添加 user_follow 表，修改 feed_post 表
- `backend/src/main/java/com/ai/food/model/FeedPost.java` - 添加 visibility, viewCount 字段
- `backend/src/main/java/com/ai/food/repository/FeedPostRepository.java` - 添加查询方法
- `backend/src/main/java/com/ai/food/controller/FeedController.java` - 添加热榜、友榜 API
- `backend/src/main/java/com/ai/food/service/feed/FeedService.java` - 核心业务逻辑

### 前端新增文件
- `frontend/src/views/FollowList.vue`

### 前端修改文件
- `frontend/src/api/index.ts` - 添加关注、热榜、友榜接口
- `frontend/src/router/index.ts` - 添加 FollowList 路由
- `frontend/src/views/Feed.vue` - 添加副导航栏
- `frontend/src/views/Profile.vue` - 添加关注/粉丝统计

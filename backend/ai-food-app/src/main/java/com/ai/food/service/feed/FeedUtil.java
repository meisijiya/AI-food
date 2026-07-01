package com.ai.food.service.feed;

import com.ai.food.common.mapper.UserMapper;
import com.ai.food.common.model.CollectedParam;
import com.ai.food.common.model.FeedPost;
import com.ai.food.common.model.SysUser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Feed 模块共享常量与对象映射工具。
 * <p>
 * 由原 {@link FeedService}（828 行）的拆分抽出，避免在 4 个子 service 中重复定义 Redis key 与
 * 通用 DTO 转换方法。
 * </p>
 *
 * <p>ponytail: 这里只放常量 + 纯函数工具，不放任何业务逻辑；ObjectMapper 维持原版 {@code new ObjectMapper()}
 * 以保证序列化行为零变化。</p>
 */
@Slf4j
final class FeedUtil {

    private FeedUtil() {
    }

    // ===== Redis key 常量 =====
    static final String LIKE_SET_KEY = "feed:like:";
    static final String LIKE_COUNT_KEY = "feed:like:count:";
    static final String LIKE_LOCK_KEY = "feed:like:lock:";
    static final String UNREAD_LIKES_KEY = "feed:notification:likes:";
    static final String UNREAD_COMMENTS_KEY = "feed:notification:comments:";
    static final String HOT_RANK_KEY = "feed:hot:rank";
    static final String HOT_DETAILS_KEY = "feed:hot:details";
    static final String FRIEND_FEED_KEY = "feed:friend:";

    /** ponytail: 保留原版 {@code new ObjectMapper()} 实例，避免 Spring 全局 ObjectMapper 配置（如 JSR310 模块）改变序列化行为。 */
    static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * 构造帖子详情 Map（不含用户信息，由 enrich 方法补充）。
     */
    static Map<String, Object> buildPostMap(FeedPost post) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", post.getId());
        map.put("userId", post.getUserId());
        map.put("foodName", post.getFoodName());
        map.put("commentPreview", post.getCommentPreview());
        map.put("thumbnailUrl", post.getThumbnailUrl());
        map.put("originalPhotoUrl", post.getOriginalPhotoUrl());
        map.put("reason", post.getReason());
        map.put("collectedParams", post.getCollectedParams());
        map.put("likeCount", post.getLikeCount());
        map.put("commentCount", post.getCommentCount());
        map.put("visibility", post.getVisibility());
        map.put("publishedAt", post.getPublishedAt());
        return map;
    }

    /**
     * 构造简化版帖子 Map（用于热榜卡片展示）。
     */
    static Map<String, Object> buildSimplifiedPostMap(FeedPost post) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", post.getId());
        map.put("userId", post.getUserId());
        map.put("foodName", post.getFoodName());
        map.put("thumbnailUrl", post.getThumbnailUrl());
        map.put("likeCount", post.getLikeCount());
        map.put("commentCount", post.getCommentCount());
        map.put("visibility", post.getVisibility());
        return map;
    }

    /**
     * 注入单条用户公开信息（昵称 / 头像）。
     */
    static void enrichUserInfo(Map<String, Object> target, Long userId, UserMapper userMapper) {
        SysUser user = userMapper.selectById(userId);
        if (user != null) {
            target.put("nickname", user.getNickname() != null ? user.getNickname() : "匿名用户");
            target.put("avatar", user.getAvatar());
        }
    }

    /**
     * 批量注入用户公开信息，减少 N+1 查询。
     */
    static void batchEnrichUserInfo(List<Map<String, Object>> items, UserMapper userMapper) {
        if (items == null || items.isEmpty()) return;
        Set<Long> userIds = new LinkedHashSet<>();
        for (Map<String, Object> item : items) {
            Number uid = (Number) item.get("userId");
            if (uid != null) userIds.add(uid.longValue());
        }
        if (userIds.isEmpty()) return;
        Map<Long, SysUser> userMap = new LinkedHashMap<>();
        for (SysUser user : userMapper.findByIdIn(new ArrayList<>(userIds))) {
            userMap.put(user.getId(), user);
        }
        for (Map<String, Object> item : items) {
            Number uid = (Number) item.get("userId");
            if (uid == null) continue;
            SysUser user = userMap.get(uid.longValue());
            if (user != null) {
                item.put("nickname", user.getNickname() != null ? user.getNickname() : "匿名用户");
                item.put("avatar", user.getAvatar());
            }
        }
    }

    /**
     * 序列化为前端可直接展示的已收集参数 JSON。
     */
    static String buildCollectedParamsJson(List<CollectedParam> params) {
        List<Map<String, String>> list = new ArrayList<>();
        for (CollectedParam p : params) {
            Map<String, String> pm = new LinkedHashMap<>();
            pm.put("name", p.getParamName());
            pm.put("value", p.getParamValue());
            list.add(pm);
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize collected params", e);
            return "[]";
        }
    }
}

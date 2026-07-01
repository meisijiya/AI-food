package com.ai.food.service.feed;

import com.ai.food.common.mapper.CollectedParamMapper;
import com.ai.food.common.mapper.FeedPostMapper;
import com.ai.food.common.mapper.PhotoMapper;
import com.ai.food.common.mapper.UserMapper;
import com.ai.food.common.model.CollectedParam;
import com.ai.food.common.model.FeedPost;
import com.ai.food.common.model.Photo;
import com.ai.food.exception.PermissionDeniedException;
import com.ai.food.service.follow.FollowService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.ai.food.service.feed.FeedUtil.FRIEND_FEED_KEY;
import static com.ai.food.service.feed.FeedUtil.LIKE_SET_KEY;
import static com.ai.food.service.feed.FeedUtil.OBJECT_MAPPER;
import static com.ai.food.service.feed.FeedUtil.batchEnrichUserInfo;
import static com.ai.food.service.feed.FeedUtil.buildPostMap;
import static com.ai.food.service.feed.FeedUtil.enrichUserInfo;

/**
 * Feed 查询服务：公共列表 / 详情 / 关注流 / 热榜查询。
 * <p>ponytail: 读路径纯查询 + 必要的视图计数；可见性校验保留为 private helper。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeedQueryService {

    private final FeedPostMapper feedPostMapper;
    private final PhotoMapper photoMapper;
    private final CollectedParamMapper collectedParamMapper;
    private final UserMapper userMapper;
    private final FollowService followService;
    private final StringRedisTemplate stringRedisTemplate;
    private final FeedHotRankService feedHotRankService;

    /**
     * 公共动态流：已登录则过滤"仅粉丝可见"；支持按 foodName / paramValue 模糊搜索。
     */
    public Map<String, Object> getPublicFeedList(int page, int size, String foodName,
                                                  String paramName, String paramValue, Long currentUserId) {
        // MP 分页 1-based，控制器传 0-based
        IPage<FeedPost> postPage = new Page<>(page + 1, size);

        String searchParamValue = null;
        if (paramName != null && paramValue != null) {
            searchParamValue = "\"" + paramName + "\":\"" + paramValue + "\"";
        }

        String searchFoodName = foodName != null && !foodName.isBlank() ? foodName : null;

        if (currentUserId != null) {
            List<Long> followingIds = followService.getFollowingIds(currentUserId);
            if (!followingIds.isEmpty()) {
                postPage = feedPostMapper.findPublicAndFansOnlyByFilters(
                        postPage, followingIds, searchFoodName, searchParamValue);
            } else {
                postPage = feedPostMapper.findPublicByFilters(
                        postPage, searchFoodName, searchParamValue);
            }
        } else {
            postPage = feedPostMapper.findPublicByFilters(
                    postPage, searchFoodName, searchParamValue);
        }

        List<Map<String, Object>> items = new ArrayList<>();
        for (FeedPost post : postPage.getRecords()) {
            Map<String, Object> item = buildPostMap(post);
            items.add(item);
        }
        batchEnrichUserInfo(items, userMapper);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", items);
        result.put("page", (int) postPage.getCurrent() - 1);
        result.put("size", postPage.getSize());
        result.put("totalElements", postPage.getTotal());
        result.put("totalPages", (int) postPage.getPages());
        return result;
    }

    /**
     * 热榜 Top20：未登录走缓存，登录时按粉丝可见性过滤。
     */
    public Map<String, Object> getHotRank(Long currentUserId) {
        // 优先从缓存获取（仅当未登录时使用缓存，已登录时需要过滤粉丝可见帖子）
        if (currentUserId == null) {
            Map<String, Object> cached = feedHotRankService.readHotRankCache();
            if (cached != null) {
                return cached;
            }
        }

        // 缓存不存在或需要过滤，从数据库加载
        return feedHotRankService.loadAndCacheHotRank(currentUserId);
    }

    /**
     * 读取某用户的关注流（已由发布时 fan-out 写入 Redis List）。
     */
    public Map<String, Object> getFriendFeedList(Long userId, int page, int size) {
        String key = FRIEND_FEED_KEY + userId;
        long start = page * size;
        long end = start + size - 1;

        List<String> feedJsons = stringRedisTemplate.opsForList().range(key, start, end);
        if (feedJsons == null || feedJsons.isEmpty()) {
            return Map.of("items", new ArrayList<>(), "total", 0);
        }

        List<Map<String, Object>> items = new ArrayList<>();
        for (String json : feedJsons) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> item = OBJECT_MAPPER.readValue(json, Map.class);
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

    /**
     * 动态详情：含可见性校验、是否已点赞、关联照片 / 参数、浏览数 +1、命中热榜则刷缓存。
     */
    public Map<String, Object> getFeedDetail(Long postId, Long currentUserId) {
        FeedPost post = feedPostMapper.selectById(postId);
        if (post == null) {
            throw new RuntimeException("动态不存在");
        }
        assertFeedVisible(post, currentUserId);

        Map<String, Object> result = buildPostMap(post);
        enrichUserInfo(result, post.getUserId(), userMapper);

        // Check if current user liked
        Boolean isLiked = currentUserId != null ?
            stringRedisTemplate.opsForSet().isMember(LIKE_SET_KEY + postId, currentUserId.toString()) :
            false;
        result.put("isLiked", Boolean.TRUE.equals(isLiked));

        // Photo info
        Photo optPhoto = photoMapper.findFirstByRelatedSessionIdOrderByCreatedAtDesc(post.getSessionId());
        if (optPhoto != null) {
            result.put("photoUrl", optPhoto.getOriginalPath());
            result.put("thumbnailUrl", optPhoto.getThumbnailPath());
        }

        // Collected params
        List<CollectedParam> params = collectedParamMapper.findBySessionId(post.getSessionId());
        List<Map<String, String>> paramList = new ArrayList<>();
        for (CollectedParam p : params) {
            Map<String, String> pm = new LinkedHashMap<>();
            pm.put("name", p.getParamName());
            pm.put("value", p.getParamValue());
            paramList.add(pm);
        }
        result.put("collectedParams", paramList);

        // Increment hot score for view
        feedHotRankService.incrementHotScore(postId, 1);

        // Update view count in DB
        post.setViewCount(post.getViewCount() + 1);
        feedPostMapper.updateById(post);

        return result;
    }

    /**
     * 对详情接口重复执行可见性校验，避免绕过列表过滤直接读到受限动态。
     */
    private void assertFeedVisible(FeedPost post, Long currentUserId) {
        if ("public".equals(post.getVisibility())) {
            return;
        }
        if (currentUserId != null && Objects.equals(post.getUserId(), currentUserId)) {
            return;
        }
        if ("friends".equals(post.getVisibility()) && currentUserId != null && followService.isFollowing(currentUserId, post.getUserId())) {
            return;
        }
        throw new PermissionDeniedException("无权查看该动态");
    }
}

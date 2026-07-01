package com.ai.food.service.feed;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Feed 服务 facade：保留原 828 行 {@code FeedService} 的全部公开方法签名，内部按职责 delegate 到
 * {@link FeedQueryService} / {@link FeedPublishService} / {@link FeedCommentService} /
 * {@link FeedHotRankService}。
 * <p>
 * Controller 与其它调用方（如 {@code RecordService#cleanRedisForDeletedPost}）无需修改，
 * 仍按原 {@code feedService.xxx(...)} 调用。
 * </p>
 *
 * <p>ponytail: 纯转调，0 业务逻辑；事务边界由各子 service 自己负责。{@link Scheduled} 注解保留在 facade
 * 上以便 Spring 调度器仍能命中入口。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeedService {

    private final FeedQueryService feedQueryService;
    private final FeedPublishService feedPublishService;
    private final FeedCommentService feedCommentService;
    private final FeedHotRankService feedHotRankService;

    // ===== 查询（delegate → FeedQueryService） =====

    public Map<String, Object> getPublicFeedList(int page, int size, String foodName,
                                                  String paramName, String paramValue, Long currentUserId) {
        return feedQueryService.getPublicFeedList(page, size, foodName, paramName, paramValue, currentUserId);
    }

    public Map<String, Object> getFeedDetail(Long postId, Long currentUserId) {
        return feedQueryService.getFeedDetail(postId, currentUserId);
    }

    public Map<String, Object> getHotRank(Long currentUserId) {
        return feedQueryService.getHotRank(currentUserId);
    }

    public Map<String, Object> getFriendFeedList(Long userId, int page, int size) {
        return feedQueryService.getFriendFeedList(userId, page, size);
    }

    // ===== 发布 / 撤回（delegate → FeedPublishService） =====

    public Map<String, Object> publishPost(Long userId, String sessionId, String commentPreview, String visibility) {
        return feedPublishService.publishPost(userId, sessionId, commentPreview, visibility);
    }

    public void unpublish(Long userId, String sessionId) {
        feedPublishService.unpublish(userId, sessionId);
    }

    public Map<String, Object> checkPublishedWithVisibility(String sessionId, Long userId) {
        return feedPublishService.checkPublishedWithVisibility(sessionId, userId);
    }

    public boolean checkPublished(String sessionId, Long userId) {
        return feedPublishService.checkPublished(sessionId, userId);
    }

    /**
     * 由 {@code RecordService} 外部调用，签名必须保持 public。
     */
    public void cleanRedisForDeletedPost(Long postId, Long userId) {
        feedPublishService.cleanRedisForDeletedPost(postId, userId);
    }

    // ===== 评论 / 点赞（delegate → FeedCommentService） =====

    public Map<String, Object> toggleLike(Long postId, Long userId) {
        return feedCommentService.toggleLike(postId, userId);
    }

    public Map<String, Object> addComment(Long postId, Long userId, String content, String imageUrl) {
        return feedCommentService.addComment(postId, userId, content, imageUrl);
    }

    public Map<String, Object> getComments(Long postId, int page, int size) {
        return feedCommentService.getComments(postId, page, size);
    }

    public void deleteComment(Long commentId, Long userId) {
        feedCommentService.deleteComment(commentId, userId);
    }

    // ===== 定时任务（delegate → FeedHotRankService） =====

    /**
     * 定时刷新热榜缓存（仅未登录分支），每 10 分钟一次。
     * <p>ponytail: 注解必须留在 facade 上，否则 Spring 调度器无法命中（内部调用不经过 AOP 代理）。</p>
     */
    @Scheduled(cron = "0 */10 * * * ?")
    public void refreshHotRankCacheScheduled() {
        log.info("Scheduled hot rank cache refresh started");
        feedHotRankService.loadAndCacheHotRank(null);
        log.info("Scheduled hot rank cache refresh completed");
    }
}

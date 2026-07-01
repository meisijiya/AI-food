package com.ai.food.service.feed;

import com.ai.food.common.mapper.ChatPhotoMapper;
import com.ai.food.common.mapper.FeedCommentMapper;
import com.ai.food.common.mapper.FeedPostMapper;
import com.ai.food.common.mapper.UserMapper;
import com.ai.food.common.model.ChatPhoto;
import com.ai.food.common.model.FeedComment;
import com.ai.food.common.model.FeedPost;
import com.ai.food.common.model.SysUser;
import com.ai.food.service.like.LikeService;
import com.ai.food.service.notification.NotificationService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.ai.food.service.feed.FeedUtil.batchEnrichUserInfo;
import static com.ai.food.service.feed.FeedUtil.enrichUserInfo;

/**
 * Feed 评论服务：新增评论 / 删除评论 / 查询评论 / 点赞（代理 LikeService）。
 * <p>ponytail: 保留原 addComment / deleteComment 的事务边界与热榜联动顺序。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeedCommentService {

    private final FeedCommentMapper feedCommentMapper;
    private final FeedPostMapper feedPostMapper;
    private final ChatPhotoMapper chatPhotoMapper;
    private final UserMapper userMapper;
    private final NotificationService notificationService;
    private final LikeService likeService;
    private final FeedHotRankService feedHotRankService;

    /**
     * 点赞 / 取消点赞（实际逻辑在 LikeService）。
     */
    @Transactional
    public Map<String, Object> toggleLike(Long postId, Long userId) {
        return likeService.toggleLike(postId, userId);
    }

    /**
     * 添加评论并更新帖子评论计数 + 通知作者 + 命中热榜则刷缓存。
     */
    @Transactional
    public Map<String, Object> addComment(Long postId, Long userId, String content, String imageUrl) {
        FeedPost post = feedPostMapper.selectById(postId);
        if (post == null) {
            throw new RuntimeException("动态不存在");
        }

        FeedComment comment = new FeedComment();
        comment.setPostId(postId);
        comment.setUserId(userId);
        comment.setContent(content);
        comment.setImageUrl(imageUrl);
        feedCommentMapper.insert(comment);

        // Increment comment count
        post.setCommentCount(post.getCommentCount() + 1);
        feedPostMapper.updateById(post);

        // Add notification for post owner
        if (!post.getUserId().equals(userId)) {
            String commenterName = "匿名用户";
            SysUser commenter = userMapper.selectById(userId);
            if (commenter != null) {
                commenterName = commenter.getNickname() != null ? commenter.getNickname() : commenter.getUsername();
            }
            notificationService.addCommentNotification(
                    post.getUserId(), comment.getId(), postId, userId, commenterName, content);
        }

        // Increment hot score for comment (+5)
        feedHotRankService.incrementHotScore(postId, 5);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", comment.getId());
        result.put("postId", comment.getPostId());
        result.put("userId", comment.getUserId());
        result.put("content", comment.getContent());
        result.put("imageUrl", comment.getImageUrl());
        result.put("createdAt", comment.getCreatedAt());
        enrichUserInfo(result, comment.getUserId(), userMapper);
        return result;
    }

    /**
     * 获取某帖子的评论分页（按时间倒序）。
     */
    public Map<String, Object> getComments(Long postId, int page, int size) {
        IPage<FeedComment> commentPage = new Page<>(page + 1, size);
        feedCommentMapper.findByPostIdOrderByCreatedAtDesc(commentPage, postId);

        List<Map<String, Object>> items = new ArrayList<>();
        for (FeedComment comment : commentPage.getRecords()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", comment.getId());
            item.put("postId", comment.getPostId());
            item.put("userId", comment.getUserId());
            item.put("content", comment.getContent());
            item.put("imageUrl", comment.getImageUrl());
            item.put("createdAt", comment.getCreatedAt());
            items.add(item);
        }
        batchEnrichUserInfo(items, userMapper);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", items);
        result.put("page", (int) commentPage.getCurrent() - 1);
        result.put("size", commentPage.getSize());
        result.put("totalElements", commentPage.getTotal());
        result.put("totalPages", (int) commentPage.getPages());
        return result;
    }

    /**
     * 删除评论：归属校验 → 软删评论 → 同步删除关联 chat_photo → 同步通知/计数 → 热榜扣分。
     */
    @Transactional
    public void deleteComment(Long commentId, Long userId) {
        FeedComment comment = feedCommentMapper.findByIdAndUserId(commentId, userId);
        if (comment == null) {
            throw new RuntimeException("评论不存在或无权限删除");
        }
        if (Boolean.TRUE.equals(comment.getIsDeleted())) {
            return;
        }

        int updated = feedCommentMapper.softDeleteByIdAndUserId(commentId, userId);
        if (updated <= 0) {
            return;
        }

        if (comment.getImageUrl() != null && !comment.getImageUrl().isBlank()) {
            ChatPhoto photo = chatPhotoMapper.findByOriginalPath(comment.getImageUrl());
            if (photo == null) {
                photo = chatPhotoMapper.findByThumbnailPath(comment.getImageUrl());
            }
            if (photo != null) {
                photo.setIsDeleted(1);
                chatPhotoMapper.updateById(photo);
            }
        }

        FeedPost postForNotif = feedPostMapper.selectById(comment.getPostId());
        if (postForNotif != null) {
            notificationService.removeCommentNotification(postForNotif.getUserId(), commentId);
        }

        FeedPost postForCount = feedPostMapper.selectById(comment.getPostId());
        if (postForCount != null) {
            int currentCount = postForCount.getCommentCount() == null ? 0 : postForCount.getCommentCount();
            postForCount.setCommentCount(Math.max(0, currentCount - 1));
            feedPostMapper.updateById(postForCount);
        }

        feedHotRankService.incrementHotScore(comment.getPostId(), -5);
    }

    /**
     * 把外部累计的点赞数同步回数据库（内部使用）。
     * <p>ponytail: 原 FeedService 私有方法，目前未被外部调用，保留以维持原代码结构。</p>
     */
    private void updateDbLikeCount(Long postId, int count) {
        FeedPost post = feedPostMapper.selectById(postId);
        if (post != null) {
            post.setLikeCount(count);
            feedPostMapper.updateById(post);
        }
    }
}

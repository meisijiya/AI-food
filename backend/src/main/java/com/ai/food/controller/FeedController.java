package com.ai.food.controller;

import com.ai.food.dto.ApiResponse;
import com.ai.food.service.feed.FeedService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/feed")
@RequiredArgsConstructor
public class FeedController {

    private final FeedService feedService;

    @PostMapping("/publish")
    public ApiResponse<Map<String, Object>> publishPost(@RequestBody Map<String, String> body) {
        String sessionId = body.get("sessionId");
        if (sessionId == null || sessionId.isBlank()) {
            return ApiResponse.error("请提供会话ID");
        }
        Long userId = getCurrentUserId();
        String commentPreview = body.get("commentPreview");
        String visibility = body.get("visibility");
        if (visibility == null || (!visibility.equals("public") && !visibility.equals("friends"))) {
            visibility = "public";
        }
        Map<String, Object> result = feedService.publishPost(userId, sessionId, commentPreview, visibility);
        return ApiResponse.success("发布成功", result);
    }

    @GetMapping("/list")
    public ApiResponse<Map<String, Object>> getFeedList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String foodName,
            @RequestParam(required = false) String paramName,
            @RequestParam(required = false) String paramValue) {
        Map<String, Object> result = feedService.getPublicFeedList(page, size, foodName, paramName, paramValue);
        return ApiResponse.success(result);
    }

    @GetMapping("/hot-rank")
    public ApiResponse<Map<String, Object>> getHotRank() {
        Map<String, Object> result = feedService.getHotRank();
        return ApiResponse.success(result);
    }

    @GetMapping("/friend-feed")
    public ApiResponse<Map<String, Object>> getFriendFeedList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long userId = getCurrentUserId();
        Map<String, Object> result = feedService.getFriendFeedList(userId, page, size);
        return ApiResponse.success(result);
    }

    @GetMapping("/detail/{postId}")
    public ApiResponse<Map<String, Object>> getFeedDetail(@PathVariable Long postId) {
        Long currentUserId = getCurrentUserId();
        Map<String, Object> result = feedService.getFeedDetail(postId, currentUserId);
        return ApiResponse.success(result);
    }

    @PostMapping("/like/{postId}")
    public ApiResponse<Map<String, Object>> toggleLike(@PathVariable Long postId) {
        Long userId = getCurrentUserId();
        Map<String, Object> result = feedService.toggleLike(postId, userId);
        return ApiResponse.success(result);
    }

    @PostMapping("/comment/{postId}")
    public ApiResponse<Map<String, Object>> addComment(@PathVariable Long postId,
                                                        @RequestBody Map<String, String> body) {
        String content = body.get("content");
        if (content == null || content.isBlank()) {
            return ApiResponse.error("评论内容不能为空");
        }
        String imageUrl = body.get("imageUrl");
        Long userId = getCurrentUserId();
        Map<String, Object> result = feedService.addComment(postId, userId, content, imageUrl);
        return ApiResponse.success("评论成功", result);
    }

    @GetMapping("/comments/{postId}")
    public ApiResponse<Map<String, Object>> getComments(@PathVariable Long postId,
                                                         @RequestParam(defaultValue = "0") int page,
                                                         @RequestParam(defaultValue = "10") int size) {
        Map<String, Object> result = feedService.getComments(postId, page, size);
        return ApiResponse.success(result);
    }

    @GetMapping("/check/{sessionId}")
    public ApiResponse<Map<String, Object>> checkPublished(@PathVariable String sessionId) {
        Long userId = getCurrentUserId();
        boolean published = feedService.checkPublished(sessionId, userId);
        return ApiResponse.success(Map.of("published", published));
    }

    @DeleteMapping("/unpublish/{sessionId}")
    public ApiResponse<Void> unpublish(@PathVariable String sessionId) {
        Long userId = getCurrentUserId();
        feedService.unpublish(userId, sessionId);
        return ApiResponse.success("已取消发布", null);
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return Long.parseLong(auth.getPrincipal().toString());
    }
}

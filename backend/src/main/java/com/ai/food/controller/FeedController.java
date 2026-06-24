package com.ai.food.controller;

import com.ai.food.dto.ApiResponse;
import com.ai.food.service.feed.FeedService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Feed推荐", description = "推荐发布、Feed 流、热榜、好友 Feed")
public class FeedController {

    private final FeedService feedService;

    @PostMapping("/publish")
    @Operation(summary = "发布推荐", description = "用户把推荐发布到 Feed 大厅（公开/好友可见）")
    public ApiResponse<Map<String, Object>> publishPost(
            @Parameter(description = "推荐内容：sessionId（必填）、commentPreview、visibility(public/friends)")
            @RequestBody Map<String, String> body) {
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
    @Operation(summary = "公共 Feed 列表", description = "分页查询公共 Feed 流，支持按 foodName / paramName / paramValue 过滤")
    public ApiResponse<Map<String, Object>> getFeedList(
            @Parameter(description = "页码（0 起）", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页条数", example = "10") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "美食名过滤（可选）") @RequestParam(required = false) String foodName,
            @Parameter(description = "参数名过滤（可选，如 taste/restriction/preference/health）") @RequestParam(required = false) String paramName,
            @Parameter(description = "参数值过滤（可选）") @RequestParam(required = false) String paramValue) {
        Long userId = getCurrentUserIdOrNull();
        Map<String, Object> result = feedService.getPublicFeedList(page, size, foodName, paramName, paramValue, userId);
        return ApiResponse.success(result);
    }

    @GetMapping("/hot-rank")
    @Operation(summary = "推荐热榜", description = "基于点赞数 + HeavyKeeper 衰减算法的 Top 热门推荐")
    public ApiResponse<Map<String, Object>> getHotRank() {
        Long userId = getCurrentUserIdOrNull();
        Map<String, Object> result = feedService.getHotRank(userId);
        return ApiResponse.success(result);
    }

    @GetMapping("/friend-feed")
    @Operation(summary = "好友 Feed", description = "只看我关注的人发布的推荐（需登录）")
    public ApiResponse<Map<String, Object>> getFriendFeedList(
            @Parameter(description = "页码（0 起）", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页条数", example = "10") @RequestParam(defaultValue = "10") int size) {
        Long userId = getCurrentUserId();
        Map<String, Object> result = feedService.getFriendFeedList(userId, page, size);
        return ApiResponse.success(result);
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            try {
                return Long.parseLong(authentication.getName());
            } catch (NumberFormatException e) {
                log.warn("Invalid user ID format in authentication: {}", authentication.getName());
            }
        }
        return 0L;
    }

    private Long getCurrentUserIdOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal())) {
            try {
                return Long.parseLong(authentication.getName());
            } catch (NumberFormatException ignore) {
            }
        }
        return null;
    }
}

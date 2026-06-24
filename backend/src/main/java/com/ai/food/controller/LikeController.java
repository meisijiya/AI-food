package com.ai.food.controller;

import com.ai.food.dto.ApiResponse;
import com.ai.food.service.like.LikeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/like")
@RequiredArgsConstructor
@Tag(name = "点赞与社交", description = "点赞/取消点赞、点赞状态、热门推荐（HeavyKeeper 算法）")
public class LikeController {

    private final LikeService likeService;

    @PostMapping("/{postId}")
    @Operation(summary = "点赞/取消点赞", description = "对推荐切换点赞状态：已点赞→取消，未点赞→点赞。底层用 Redis Lua 保证原子性")
    public ApiResponse<Map<String, Object>> toggleLike(
            @Parameter(description = "推荐 ID", example = "10001") @PathVariable Long postId) {
        Long userId = getCurrentUserId();
        Map<String, Object> result = likeService.toggleLike(postId, userId);
        return ApiResponse.success(result);
    }

    @GetMapping("/{postId}/status")
    @Operation(summary = "查询点赞状态", description = "查询当前用户对某推荐的点赞状态 + 点赞数 + 是否热门")
    public ApiResponse<Map<String, Object>> getLikeStatus(
            @Parameter(description = "推荐 ID", example = "10001") @PathVariable Long postId) {
        Long userId = getCurrentUserId();
        Map<String, Object> status = likeService.getLikeStatus(postId, userId);
        return ApiResponse.success(status);
    }

    @GetMapping("/{postId}/count")
    @Operation(summary = "查询点赞数", description = "只查点赞数（公开接口，无需登录）")
    public ApiResponse<Map<String, Object>> getLikeCount(
            @Parameter(description = "推荐 ID", example = "10001") @PathVariable Long postId) {
        long count = likeService.getLikeCount(postId);
        return ApiResponse.success(Map.of("postId", postId, "likeCount", count));
    }

    @GetMapping("/hot-posts")
    @Operation(summary = "热门推荐 Top N", description = "用 HeavyKeeper 算法（ZSet + 时间衰减）取 Top K 热门推荐 ID")
    public ApiResponse<Map<String, Object>> getHotPosts(
            @Parameter(description = "返回数量", example = "20") @RequestParam(defaultValue = "20") int limit) {
        List<Long> hotPosts = likeService.getHotPosts(limit);
        return ApiResponse.success(Map.of("hotPosts", hotPosts));
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
}

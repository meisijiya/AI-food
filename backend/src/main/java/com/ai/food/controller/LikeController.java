package com.ai.food.controller;

import com.ai.food.dto.ApiResponse;
import com.ai.food.service.like.LikeService;
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
public class LikeController {

    private final LikeService likeService;

    @PostMapping("/{postId}")
    public ApiResponse<Map<String, Object>> toggleLike(@PathVariable Long postId) {
        Long userId = getCurrentUserId();
        Map<String, Object> result = likeService.toggleLike(postId, userId);
        return ApiResponse.success(result);
    }

    @GetMapping("/{postId}/status")
    public ApiResponse<Map<String, Object>> getLikeStatus(@PathVariable Long postId) {
        Long userId = getCurrentUserId();
        Map<String, Object> status = likeService.getLikeStatus(postId, userId);
        return ApiResponse.success(status);
    }

    @GetMapping("/{postId}/count")
    public ApiResponse<Map<String, Object>> getLikeCount(@PathVariable Long postId) {
        long count = likeService.getLikeCount(postId);
        return ApiResponse.success(Map.of("postId", postId, "likeCount", count));
    }

    @GetMapping("/hot-posts")
    public ApiResponse<Map<String, Object>> getHotPosts(@RequestParam(defaultValue = "20") int limit) {
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

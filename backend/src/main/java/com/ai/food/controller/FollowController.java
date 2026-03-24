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

    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> getMyFollowStats() {
        Long userId = getCurrentUserId();
        Map<String, Object> result = followService.getFollowStats(userId);
        return ApiResponse.success(result);
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return Long.parseLong(auth.getPrincipal().toString());
    }
}

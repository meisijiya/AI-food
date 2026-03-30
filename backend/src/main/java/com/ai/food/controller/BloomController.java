package com.ai.food.controller;

import com.ai.food.dto.ApiResponse;
import com.ai.food.dto.MatchUserDetailDTO;
import com.ai.food.dto.UserSimilarityDTO;
import com.ai.food.service.bloom.BloomFilterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@RestController
@RequestMapping("/api/bloom")
@RequiredArgsConstructor
public class BloomController {

    private final BloomFilterService bloomFilterService;

    @PostMapping("/add")
    public ApiResponse<Void> addRecommendation(
            @RequestParam String recordId,
            @RequestParam String sessionId,
            @RequestParam String paramValue) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return ApiResponse.error("用户未登录");
        }
        bloomFilterService.addRecommendation(userId, recordId, paramValue);
        return ApiResponse.success("推荐记录已添加", null);
    }

    @GetMapping("/similarity/{targetUserId}")
    public ApiResponse<Map<String, Object>> getSimilarity(@PathVariable Long targetUserId) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return ApiResponse.error("用户未登录");
        }
        if (userId.equals(targetUserId)) {
            return ApiResponse.error("不能与自己比较");
        }
        double similarity = bloomFilterService.calculateSimilarity(userId, targetUserId);
        return ApiResponse.success(Map.of(
                "similarity", Math.round(similarity * 100.0) / 100.0,
                "percentage", (int) Math.round(similarity * 100)
        ));
    }

    @GetMapping("/top-k/{k}")
    public ApiResponse<List<UserSimilarityDTO>> getTopKSimilarUsers(@PathVariable int k) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return ApiResponse.error("用户未登录");
        }
        if (k <= 0 || k > 100) {
            k = 10;
        }
        List<UserSimilarityDTO> result = bloomFilterService.getTopKSimilarUsers(userId, k);
        return ApiResponse.success(result);
    }

    @GetMapping("/my")
    public ApiResponse<Map<String, Object>> getMyBloomFilter() {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return ApiResponse.error("用户未登录");
        }
        int recordCount = bloomFilterService.getRecordCount(userId);
        List<String> recentRecords = bloomFilterService.getRecentRecords(userId);
        return ApiResponse.success(Map.of(
                "userId", userId,
                "recordCount", recordCount,
                "recentRecords", recentRecords,
                "maxRecords", 10
        ));
    }

    @GetMapping("/random-match")
    public ApiResponse<MatchUserDetailDTO> getRandomMatch(
            @RequestParam(required = false) List<Long> excludeIds) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return ApiResponse.error("用户未登录");
        }
        Set<Long> excludeSet = excludeIds != null ? Set.copyOf(excludeIds) : Set.of();
        MatchUserDetailDTO result = bloomFilterService.getRandomSimilarUser(userId, excludeSet);
        return ApiResponse.success(result);
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Long) {
            return (Long) authentication.getPrincipal();
        }
        return null;
    }
}
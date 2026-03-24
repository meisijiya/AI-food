package com.ai.food.controller;

import com.ai.food.dto.ApiResponse;
import com.ai.food.service.share.ShareService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/share")
@RequiredArgsConstructor
public class ShareController {

    private final ShareService shareService;

    @PostMapping("/create")
    public ApiResponse<Map<String, Object>> createShare(@RequestBody Map<String, String> body) {
        String sessionId = body.get("sessionId");
        if (sessionId == null || sessionId.isBlank()) {
            return ApiResponse.error("请提供会话ID");
        }
        Long userId = getCurrentUserId();
        Map<String, Object> result = shareService.createShare(sessionId, userId);
        return ApiResponse.success("分享链接已创建", result);
    }

    @GetMapping("/detail/{shareToken}")
    public ApiResponse<Map<String, Object>> getShareDetail(@PathVariable String shareToken) {
        Map<String, Object> detail = shareService.getShareDetail(shareToken);
        return ApiResponse.success(detail);
    }

    @GetMapping("/check/{sessionId}")
    public ApiResponse<Map<String, Object>> checkShare(@PathVariable String sessionId) {
        Long userId = getCurrentUserId();
        Map<String, Object> result = shareService.checkShare(sessionId, userId);
        return ApiResponse.success(result);
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return Long.parseLong(auth.getPrincipal().toString());
    }
}

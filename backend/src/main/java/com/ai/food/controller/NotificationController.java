package com.ai.food.controller;

import com.ai.food.dto.ApiResponse;
import com.ai.food.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/notification")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/list")
    public ApiResponse<Map<String, Object>> getNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long userId = getCurrentUserId();
        Map<String, Object> result = notificationService.getNotifications(userId, page, size);
        return ApiResponse.success(result);
    }

    @GetMapping("/unread")
    public ApiResponse<Map<String, Object>> getUnreadCount() {
        Long userId = getCurrentUserId();
        int unread = notificationService.getUnreadCount(userId);
        return ApiResponse.success(Map.of("unread", unread));
    }

    @DeleteMapping("/{notificationId}")
    public ApiResponse<Void> deleteNotification(@PathVariable String notificationId) {
        Long userId = getCurrentUserId();
        notificationService.deleteNotification(userId, notificationId);
        return ApiResponse.success("已删除", null);
    }

    @DeleteMapping("/clear-all")
    public ApiResponse<Void> clearAll() {
        Long userId = getCurrentUserId();
        notificationService.clearAllNotifications(userId);
        return ApiResponse.success("已清空", null);
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return Long.parseLong(auth.getPrincipal().toString());
    }
}

package com.ai.food.controller;

import com.ai.food.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 游客访问控制器
 * 提供游客无需登录即可访问的接口
 */
@Slf4j
@RestController
@RequestMapping("/api/guest")
@RequiredArgsConstructor
public class GuestController {

    /**
     * 获取游客访问信息
     * 用于首页展示游客访问提示
     */
    @GetMapping("/info")
    public ApiResponse<Map<String, Object>> getGuestInfo() {
        Map<String, Object> data = new HashMap<>();
        data.put("isGuest", true);
        data.put("title", "欢迎来到 AI Food");
        data.put("subtitle", "登录后享受AI智能美食推荐服务");
        data.put("features", new String[]{
            "智能对话交互",
            "场景感知推荐",
            "个性口味匹配"
        });
        return ApiResponse.success(data);
    }

    /**
     * 获取游客访问统计信息
     * 用于大厅页展示
     */
    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> getGuestStats() {
        Map<String, Object> data = new HashMap<>();
        data.put("totalPosts", 0);
        data.put("totalUsers", 0);
        data.put("guestCanView", true);
        data.put("guestCanInteract", false);
        return ApiResponse.success(data);
    }
}

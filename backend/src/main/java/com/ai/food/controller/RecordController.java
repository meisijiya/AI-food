package com.ai.food.controller;

import com.ai.food.dto.ApiResponse;
import com.ai.food.service.record.RecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/record")
@RequiredArgsConstructor
public class RecordController {

    private final RecordService recordService;

    @GetMapping("/list")
    public ApiResponse<Map<String, Object>> getRecordList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long userId = getCurrentUserId();
        Page<RecordService.RecordListItem> result = recordService.getRecordList(userId, page, size);

        Map<String, Object> data = new HashMap<>();
        data.put("list", result.getContent());
        data.put("total", result.getTotalElements());
        data.put("pages", result.getTotalPages());
        data.put("current", result.getNumber());
        return ApiResponse.success(data);
    }

    @GetMapping("/detail/{sessionId}")
    public ApiResponse<RecordService.RecordDetail> getRecordDetail(@PathVariable String sessionId) {
        RecordService.RecordDetail detail = recordService.getRecordDetail(sessionId);
        return ApiResponse.success(detail);
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return Long.parseLong(authentication.getPrincipal().toString());
    }
}

package com.ai.food.controller;

import com.ai.food.dto.ApiResponse;
import com.ai.food.service.upload.FileUploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
public class UploadController {

    private final FileUploadService fileUploadService;

    @PostMapping("/photo")
    public ApiResponse<Map<String, Object>> uploadPhoto(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "sessionId", required = false) String sessionId) {
        try {
            Long userId = getCurrentUserId();
            Map<String, Object> result = fileUploadService.uploadPhoto(file, userId, sessionId);
            return ApiResponse.success("上传成功", result);
        } catch (Exception e) {
            log.error("Photo upload failed", e);
            return ApiResponse.error("上传失败: " + e.getMessage());
        }
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return Long.parseLong(auth.getPrincipal().toString());
    }
}

package com.ai.food.controller;

import com.ai.food.dto.ApiResponse;
import com.ai.food.service.upload.FileUploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "聊天与文件", description = "图片/文件上传（菜品图、头像、聊天图片/文件）")
public class UploadController {

    private final FileUploadService fileUploadService;

    @PostMapping("/photo")
    @Operation(summary = "上传菜品推荐图", description = "上传图片（jpg/png/webp/gif），自动生成 400px 缩略图")
    public ApiResponse<Map<String, Object>> uploadPhoto(
            @Parameter(description = "图片文件（≤10MB）", required = true) @RequestParam("file") MultipartFile file,
            @Parameter(description = "关联会话 ID（可选）") @RequestParam(value = "sessionId", required = false) String sessionId,
            @Parameter(description = "旧图片 URL（用于替换时清理，可选）") @RequestParam(value = "oldPhotoUrl", required = false) String oldPhotoUrl) {
        try {
            Long userId = getCurrentUserId();
            Map<String, Object> result = fileUploadService.uploadPhoto(file, userId, sessionId, oldPhotoUrl);
            return ApiResponse.success("上传成功", result);
        } catch (Exception e) {
            log.error("Photo upload failed", e);
            return ApiResponse.error("图片上传失败");
        }
    }

    @PostMapping("/chat-photo")
    @Operation(summary = "上传聊天图片", description = "聊天场景的图片上传（≤10MB），自动生成缩略图")
    public ApiResponse<Map<String, Object>> uploadChatPhoto(
            @Parameter(description = "图片文件", required = true) @RequestParam("file") MultipartFile file) {
        try {
            Map<String, Object> result = fileUploadService.uploadChatPhoto(file);
            return ApiResponse.success("上传成功", result);
        } catch (Exception e) {
            log.error("Chat photo upload failed", e);
            return ApiResponse.error("聊天图片上传失败");
        }
    }

    @PostMapping("/chat-file")
    @Operation(summary = "上传聊天文件", description = "聊天场景的文件上传（≤50MB），不限类型")
    public ApiResponse<Map<String, Object>> uploadChatFile(
            @Parameter(description = "任意文件（≤50MB）", required = true) @RequestParam("file") MultipartFile file) {
        try {
            Map<String, Object> result = fileUploadService.uploadChatFile(file);
            return ApiResponse.success("上传成功", result);
        } catch (Exception e) {
            log.error("Chat file upload failed", e);
            return ApiResponse.error("文件上传失败");
        }
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return Long.parseLong(auth.getPrincipal().toString());
    }
}

package com.ai.food.controller;

import com.ai.food.dto.ApiResponse;
import com.ai.food.model.SysUser;
import com.ai.food.service.upload.FileUploadService;
import com.ai.food.service.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final FileUploadService fileUploadService;

    @GetMapping("/info")
    public ApiResponse<SysUser> getUserInfo() {
        Long userId = getCurrentUserId();
        SysUser user = userService.getUserInfo(userId);
        return ApiResponse.success(user);
    }

    @PostMapping("/sign")
    public ApiResponse<Map<String, Object>> signIn() {
        Long userId = getCurrentUserId();
        Map<String, Object> result = userService.signIn(userId);
        return ApiResponse.success("签到成功", result);
    }

    @GetMapping("/sign-status")
    public ApiResponse<Map<String, Object>> getSignStatus() {
        Long userId = getCurrentUserId();
        Map<String, Object> result = userService.getSignStatus(userId);
        return ApiResponse.success(result);
    }

    @GetMapping("/search")
    public ApiResponse<Map<String, Object>> searchUsers(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long userId = getCurrentUserId();
        Map<String, Object> result = userService.searchUsers(userId, keyword, page, size);
        return ApiResponse.success(result);
    }

    @PostMapping("/avatar")
    public ApiResponse<SysUser> uploadAvatar(@RequestParam("file") MultipartFile file) {
        Long userId = getCurrentUserId();
        try {
            SysUser currentUser = userService.getUserInfo(userId);
            Map<String, String> urls = fileUploadService.uploadAvatar(file, currentUser.getAvatar());
            SysUser user = userService.updateAvatar(userId, urls.get("thumbnailUrl"));
            return ApiResponse.success("头像更新成功", user);
        } catch (Exception e) {
            log.error("Avatar upload failed", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    @PutMapping("/nickname")
    public ApiResponse<SysUser> updateNickname(@RequestBody Map<String, String> body) {
        Long userId = getCurrentUserId();
        String nickname = body.get("nickname");
        SysUser user = userService.updateNickname(userId, nickname);
        return ApiResponse.success("昵称更新成功", user);
    }

    @PutMapping("/password")
    public ApiResponse<Void> updatePassword(@RequestBody Map<String, String> body) {
        Long userId = getCurrentUserId();
        String oldPassword = body.get("oldPassword");
        String newPassword = body.get("newPassword");
        userService.updatePassword(userId, oldPassword, newPassword);
        return ApiResponse.success("密码修改成功", null);
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return Long.parseLong(authentication.getPrincipal().toString());
    }
}

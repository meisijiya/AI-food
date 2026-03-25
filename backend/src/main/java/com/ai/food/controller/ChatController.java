package com.ai.food.controller;

import com.ai.food.dto.ApiResponse;
import com.ai.food.model.ChatMessage;
import com.ai.food.service.chat.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @GetMapping("/conversations")
    public ApiResponse<List<Map<String, Object>>> getConversationList() {
        Long userId = getCurrentUserId();
        List<Map<String, Object>> result = chatService.getConversationList(userId);
        return ApiResponse.success(result);
    }

    @GetMapping("/messages/{conversationId}")
    public ApiResponse<Map<String, Object>> getChatHistory(
            @PathVariable Long conversationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = getCurrentUserId();
        Map<String, Object> result = chatService.getChatHistory(conversationId, userId, page, size);
        return ApiResponse.success(result);
    }

    @PostMapping("/read/{conversationId}")
    public ApiResponse<Void> markAsRead(@PathVariable Long conversationId) {
        Long userId = getCurrentUserId();
        chatService.markAsRead(userId, conversationId);
        return ApiResponse.success("已读", null);
    }

    @GetMapping("/unread")
    public ApiResponse<Map<String, Object>> getUnreadCounts() {
        Long userId = getCurrentUserId();
        Map<String, Object> result = chatService.getUnreadCounts(userId);
        return ApiResponse.success(result);
    }

    @GetMapping("/contacts")
    public ApiResponse<List<Map<String, Object>>> getContacts() {
        Long userId = getCurrentUserId();
        List<Map<String, Object>> result = chatService.getContacts(userId);
        return ApiResponse.success(result);
    }

    @PostMapping("/send")
    public ApiResponse<Map<String, Object>> sendMessage(@RequestBody Map<String, Object> body) {
        Long senderId = getCurrentUserId();
        Long receiverId = Long.parseLong(body.get("receiverId").toString());
        String content = body.get("content").toString();
        String messageType = body.get("messageType") != null ? body.get("messageType").toString() : "text";

        try {
            ChatMessage message = chatService.sendMessage(senderId, receiverId, content, messageType);

            Map<String, Object> result = new java.util.LinkedHashMap<>();
            result.put("id", message.getId());
            result.put("conversationId", message.getConversationId());
            result.put("senderId", message.getSenderId());
            result.put("receiverId", message.getReceiverId());
            result.put("content", message.getContent());
            result.put("messageType", message.getMessageType());
            result.put("createdAt", message.getCreatedAt());
            return ApiResponse.success("发送成功", result);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @GetMapping("/permission/{receiverId}")
    public ApiResponse<Map<String, Object>> checkSendPermission(@PathVariable Long receiverId) {
        Long senderId = getCurrentUserId();
        String permission = chatService.checkSendPermission(senderId, receiverId);
        int remaining = chatService.getRemainingMessages(senderId, receiverId);
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("permission", permission);
        result.put("remaining", remaining);
        return ApiResponse.success(result);
    }

    @DeleteMapping("/conversation/{conversationId}")
    public ApiResponse<Void> clearConversation(@PathVariable Long conversationId) {
        Long userId = getCurrentUserId();
        try {
            chatService.clearConversation(userId, conversationId);
            return ApiResponse.success("已清除聊天记录", null);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return Long.parseLong(auth.getPrincipal().toString());
    }
}

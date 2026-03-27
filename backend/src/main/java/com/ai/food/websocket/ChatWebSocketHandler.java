package com.ai.food.websocket;

import com.ai.food.model.ChatMessage;
import com.ai.food.service.auth.JwtService;
import com.ai.food.service.chat.ChatService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final ChatService chatService;
    private final JwtService jwtService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // userId -> WebSocketSession
    private final Map<Long, WebSocketSession> userSessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.info("Chat WebSocket connected: {}", session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        log.debug("Chat received: {}", payload);

        try {
            Map<String, Object> msg = objectMapper.readValue(payload, Map.class);
            String action = (String) msg.get("action");

            switch (action) {
                case "auth" -> handleAuth(session, msg);
                case "send" -> handleSend(session, msg);
                case "read" -> handleRead(session, msg);
                case "ping" -> handlePing(session);
                default -> sendError(session, "Unknown action: " + action);
            }
        } catch (Exception e) {
            log.error("Error handling chat message", e);
            sendError(session, "消息处理失败");
        }
    }

    private void handleAuth(WebSocketSession session, Map<String, Object> msg) throws IOException {
        String token = (String) msg.get("token");
        if (token == null || token.isBlank()) {
            sendError(session, "Token required");
            return;
        }

        try {
            Long userId = jwtService.getUserId(token);
            if (userId == null) {
                sendError(session, "Invalid token");
                return;
            }

            // 存储用户会话
            userSessions.put(userId, session);
            session.getAttributes().put("userId", userId);

            // 设置在线状态
            chatService.setUserOnline(userId);

            // 发送认证成功消息
            Map<String, Object> response = new ConcurrentHashMap<>();
            response.put("type", "auth");
            response.put("success", true);
            response.put("userId", userId);
            sendMessage(session, response);

            // 发送未读消息数
            int unreadCount = chatService.getTotalUnreadCount(userId);
            Map<String, Object> unreadMsg = new ConcurrentHashMap<>();
            unreadMsg.put("type", "unread_total");
            unreadMsg.put("count", unreadCount);
            sendMessage(session, unreadMsg);

            log.info("Chat user authenticated: userId={}", userId);
        } catch (Exception e) {
            log.error("Auth failed", e);
            sendError(session, "认证失败");
        }
    }

    private void handleSend(WebSocketSession session, Map<String, Object> msg) throws IOException {
        Long senderId = (Long) session.getAttributes().get("userId");
        if (senderId == null) {
            sendError(session, "Not authenticated");
            return;
        }

        Long receiverId = Long.parseLong(msg.get("receiverId").toString());
        String content = (String) msg.get("content");
        String messageType = (String) msg.getOrDefault("messageType", "text");
        Long photoId = msg.get("photoId") != null ? Long.parseLong(msg.get("photoId").toString()) : null;
        Long fileId = msg.get("fileId") != null ? Long.parseLong(msg.get("fileId").toString()) : null;

        if (content == null || content.isBlank()) {
            sendError(session, "Content required");
            return;
        }

        // 检查发送权限
        String permission = chatService.checkSendPermission(senderId, receiverId);
        if (permission.equals("not_allowed")) {
            sendError(session, "对方未关注你，无法发送消息");
            return;
        }
        if (permission.equals("max_reached")) {
            sendError(session, "非互关最多发送5条消息");
            return;
        }

        // 发送消息
        ChatMessage chatMessage = chatService.sendMessage(senderId, receiverId, content, messageType, photoId, fileId);
        log.info("WS message sent: from={} to={}, conversationId={}, msgId={}",
                senderId, receiverId, chatMessage.getConversationId(), chatMessage.getId());

        // 构建消息响应
        Map<String, Object> messageResponse = new ConcurrentHashMap<>();
        messageResponse.put("type", "message");
        messageResponse.put("id", chatMessage.getId());
        messageResponse.put("conversationId", chatMessage.getConversationId());
        messageResponse.put("senderId", chatMessage.getSenderId());
        messageResponse.put("receiverId", chatMessage.getReceiverId());
        messageResponse.put("content", chatMessage.getContent());
        messageResponse.put("messageType", chatMessage.getMessageType());
        messageResponse.put("createdAt", chatMessage.getCreatedAt().toString());

        // 发送给发送者（确认）
        Map<String, Object> confirmMsg = new ConcurrentHashMap<>(messageResponse);
        confirmMsg.put("type", "sent");
        sendMessage(session, confirmMsg);

        // 发送给接收者（如果在线）
        WebSocketSession receiverSession = userSessions.get(receiverId);
        if (receiverSession != null && receiverSession.isOpen()) {
            sendMessage(receiverSession, messageResponse);
        }
    }

    private void handleRead(WebSocketSession session, Map<String, Object> msg) throws IOException {
        Long userId = (Long) session.getAttributes().get("userId");
        if (userId == null) {
            sendError(session, "Not authenticated");
            return;
        }

        Long conversationId = Long.parseLong(msg.get("conversationId").toString());
        chatService.markAsRead(userId, conversationId);

        // 发送已读确认
        Map<String, Object> response = new ConcurrentHashMap<>();
        response.put("type", "read");
        response.put("conversationId", conversationId);
        sendMessage(session, response);
    }

    private void handlePing(WebSocketSession session) throws IOException {
        Map<String, Object> response = new ConcurrentHashMap<>();
        response.put("type", "pong");
        sendMessage(session, response);

        // 延长在线时间
        Long userId = (Long) session.getAttributes().get("userId");
        if (userId != null) {
            chatService.setUserOnline(userId);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long userId = (Long) session.getAttributes().get("userId");
        if (userId != null) {
            userSessions.remove(userId);
            chatService.setUserOffline(userId);
            log.info("Chat user disconnected: userId={}", userId);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        Long userId = (Long) session.getAttributes().get("userId");
        log.error("Chat transport error for user {}: {}", userId, exception.getMessage());
        if (userId != null) {
            userSessions.remove(userId);
        }
    }

    public void sendToUser(Long userId, Map<String, Object> message) {
        WebSocketSession session = userSessions.get(userId);
        if (session != null && session.isOpen()) {
            try {
                sendMessage(session, message);
            } catch (IOException e) {
                log.error("Failed to send message to user {}", userId, e);
            }
        }
    }

    private void sendMessage(WebSocketSession session, Map<String, Object> message) throws IOException {
        String json = objectMapper.writeValueAsString(message);
        session.sendMessage(new TextMessage(json));
    }

    private void sendError(WebSocketSession session, String errorMessage) throws IOException {
        Map<String, Object> error = new ConcurrentHashMap<>();
        error.put("type", "error");
        error.put("message", errorMessage);
        sendMessage(session, error);
    }
}

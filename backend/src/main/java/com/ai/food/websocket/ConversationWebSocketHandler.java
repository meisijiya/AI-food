package com.ai.food.websocket;

import com.ai.food.dto.ClientMessage;
import com.ai.food.dto.ConversationState;
import com.ai.food.dto.WebSocketMessage;
import com.ai.food.service.conversation.ConversationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class ConversationWebSocketHandler extends TextWebSocketHandler {

    private final ConversationService conversationService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, ConversationState> conversationStates = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String sessionId = extractSessionId(session);
        if (sessionId == null) {
            closeSession(session, CloseStatus.NOT_ACCEPTABLE);
            return;
        }

        Long userId = (Long) session.getAttributes().get("userId");
        if (userId == null) {
            closeSession(session, CloseStatus.NOT_ACCEPTABLE);
            return;
        }

        try {
            conversationService.validateOwnership(sessionId, userId);
        } catch (Exception e) {
            log.warn("WebSocket ownership check failed for session {}: {}", sessionId, e.getMessage());
            closeSession(session, CloseStatus.NOT_ACCEPTABLE);
            return;
        }

        sessions.put(sessionId, session);
        log.debug("WebSocket connected and verified: {}", sessionId);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        log.debug("Received: {}", payload);

        try {
            ClientMessage clientMessage = objectMapper.readValue(payload, ClientMessage.class);
            String sessionId = clientMessage.getSessionId();
            if (sessionId == null) sessionId = extractSessionId(session);
            if (sessionId == null) {
                sendError(session, "Session ID required");
                return;
            }

            ConversationState state = conversationStates.get(sessionId);

            switch (clientMessage.getAction()) {
                case "start" -> handleStart(session, sessionId);
                case "answer" -> handleAnswer(session, sessionId, clientMessage.getContent(), state);
                case "complete" -> handleComplete(session, sessionId, state);
                case "cancel" -> handleCancel(session, sessionId, state);
                default -> sendError(session, "Unknown action");
            }
        } catch (Exception e) {
            log.error("Error handling message", e);
            sendError(session, "处理失败，请重试");
        }
    }

    // ==================== 开始对话 ====================

    private void handleStart(WebSocketSession session, String sessionId) throws IOException {
        ConversationState state = conversationService.initializeConversation(sessionId);
        conversationStates.put(sessionId, state);
        WebSocketMessage firstQuestion = conversationService.getFirstQuestion(state);
        sendMessage(session, firstQuestion);
        log.debug("Conversation started: {}", sessionId);
    }

    // ==================== 处理用户回答 ====================

    private void handleAnswer(WebSocketSession session, String sessionId, String content, ConversationState state) throws IOException {
        if (state == null) {
            sendError(session, "Conversation not started");
            return;
        }

        // AI 正在处理中 → 放入抢话队列
        if (state.getAiProcessing()) {
            if (state.canInterrupt()) {
                state.addPendingMessage(content);
                log.info("[{}] queued interrupt message (pending={})", sessionId, state.getPendingMessages().size());
            } else {
                log.warn("[{}] interrupt limit reached", sessionId);
            }
            return;
        }

        state.setAiProcessing(true);

        // 异步调用 AI
        new Thread(() -> {
            try {
                // 检查是否已取消
                if (state.isCancelled()) {
                    log.info("[{}] session cancelled before processing, skipping", sessionId);
                    state.setAiProcessing(false);
                    return;
                }

                List<WebSocketMessage> messages = conversationService.processAnswer(sessionId, content, state);

                // 处理完成后再检查一次是否已取消
                if (state.isCancelled()) {
                    log.info("[{}] session cancelled during processing, discarding results", sessionId);
                    state.setAiProcessing(false);
                    return;
                }

                // 发送第一条消息（AI 确认/闲聊）
                if (!messages.isEmpty()) {
                    safeSendMessage(session, messages.get(0));
                }

                // 检查是否有抢话消息
                if (state.hasPendingMessages()) {
                    state.incrementInterruptCount();
                    String combined = String.join("；", state.getPendingMessages());
                    state.clearPendingMessages();
                    log.info("[{}] processing {} interrupt messages: '{}'", sessionId, state.getInterruptCount(), combined);

                    // 发送 interrupt 回复
                    WebSocketMessage interruptMsg = conversationService.handleInterrupt(combined, state);
                    safeSendMessage(session, interruptMsg);

                    // 将抢话内容作为新的回答递归处理
                    state.setAiProcessing(false);
                    handleAnswer(session, sessionId, combined, state);
                    return;
                }

                // 发送第二条消息（下一个问题 或 推荐）
                if (messages.size() > 1) {
                    safeSendMessage(session, messages.get(1));
                    if ("recommend".equals(messages.get(1).getType())) {
                        conversationStates.remove(sessionId);
                        log.info("[{}] conversation completed", sessionId);
                    }
                }

                state.setAiProcessing(false);

            } catch (Exception e) {
                log.error("[{}] error processing answer", sessionId, e);
                state.setAiProcessing(false);
                safeSendError(session, "处理出错，请重试");
            }
        }).start();
    }

    // ==================== 手动结束 ====================

    private void handleComplete(WebSocketSession session, String sessionId, ConversationState state) throws IOException {
        if (state == null) {
            sendError(session, "Conversation not started");
            return;
        }
        WebSocketMessage recommend = conversationService.generateRecommendationMessage(sessionId, state);
        sendMessage(session, recommend);
        conversationStates.remove(sessionId);
    }

    // ==================== 取消对话（不保存数据） ====================

    private void handleCancel(WebSocketSession session, String sessionId, ConversationState state) {
        log.info("[{}] canceling conversation - no data will be saved", sessionId);

        // 先标记为已取消，阻止异步线程继续插入数据
        if (state != null) {
            state.setCancelled(true);
            state.setAiProcessing(false);
            state.clearPendingMessages();
        }

        conversationStates.remove(sessionId);

        // 删除数据库中的会话数据
        conversationService.cancelSession(sessionId);

        try {
            WebSocketMessage msg = new WebSocketMessage();
            msg.setType("system");
            msg.setContent("对话已取消");
            sendMessage(session, msg);
        } catch (IOException e) {
            log.error("Failed to send cancel confirmation", e);
        }
    }

    // ==================== 连接关闭 ====================

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String sessionId = extractSessionId(session);
        if (sessionId != null) {
            sessions.remove(sessionId);
            conversationStates.remove(sessionId);
            log.debug("WebSocket closed: {}", sessionId);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        String sessionId = extractSessionId(session);
        log.error("Transport error for session {}: {}", sessionId, exception.getMessage());
        if (sessionId != null) sessions.remove(sessionId);
    }

    // ==================== 工具方法 ====================

    private String extractSessionId(WebSocketSession session) {
        String path = session.getUri().getPath();
        String[] parts = path.split("/");
        if (parts.length >= 4) return parts[parts.length - 1];
        return null;
    }

    private void sendMessage(WebSocketSession session, WebSocketMessage message) throws IOException {
        String json = objectMapper.writeValueAsString(message);
        log.debug("Sending [{}]: {}", message.getType(),
                message.getContent() != null && message.getContent().length() > 80
                        ? message.getContent().substring(0, 80) + "..." : message.getContent());
        session.sendMessage(new TextMessage(json));
    }

    private void sendError(WebSocketSession session, String errorMessage) throws IOException {
        WebSocketMessage msg = new WebSocketMessage();
        msg.setType("system");
        msg.setContent(errorMessage);
        sendMessage(session, msg);
    }

    private void safeSendMessage(WebSocketSession session, WebSocketMessage message) {
        try {
            if (session.isOpen()) {
                sendMessage(session, message);
            }
        } catch (IOException e) {
            log.error("Failed to send message", e);
        }
    }

    private void safeSendError(WebSocketSession session, String errorMessage) {
        try {
            if (session.isOpen()) {
                sendError(session, errorMessage);
            }
        } catch (IOException e) {
            log.error("Failed to send error", e);
        }
    }

    private void closeSession(WebSocketSession session, CloseStatus status) {
        try {
            if (session.isOpen()) {
                session.close(status);
            }
        } catch (IOException e) {
            log.error("Failed to close session", e);
        }
    }
}

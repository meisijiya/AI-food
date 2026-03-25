package com.ai.food.config;

import com.ai.food.websocket.ChatWebSocketHandler;
import com.ai.food.websocket.ConversationWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {
    
    private final ConversationWebSocketHandler conversationWebSocketHandler;
    private final ChatWebSocketHandler chatWebSocketHandler;
    private final JwtHandshakeInterceptor jwtHandshakeInterceptor;
    
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(conversationWebSocketHandler, "/ws/conversation/{sessionId}")
                .addInterceptors(jwtHandshakeInterceptor)
                .setAllowedOriginPatterns("*");
        
        registry.addHandler(chatWebSocketHandler, "/ws/chat")
                .addInterceptors(jwtHandshakeInterceptor)
                .setAllowedOriginPatterns("*");
    }
}
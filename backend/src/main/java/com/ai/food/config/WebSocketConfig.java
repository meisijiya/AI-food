package com.ai.food.config;

import com.ai.food.common.service.auth.JwtHandshakeInterceptor;
import com.ai.food.websocket.ChatWebSocketHandler;
import com.ai.food.websocket.ConversationWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import java.util.List;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final ConversationWebSocketHandler conversationWebSocketHandler;
    private final ChatWebSocketHandler chatWebSocketHandler;
    private final JwtHandshakeInterceptor jwtHandshakeInterceptor;

    @Value("#{'${app.cors.allowed-origins:http://localhost:3000,http://localhost:8080}'.split(',')}")
    private List<String> allowedOrigins;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(conversationWebSocketHandler, "/ws/conversation/{sessionId}")
                .addInterceptors(jwtHandshakeInterceptor)
                .setAllowedOriginPatterns(allowedOrigins.toArray(new String[0]));

        registry.addHandler(chatWebSocketHandler, "/ws/chat")
                .addInterceptors(jwtHandshakeInterceptor)
                .setAllowedOriginPatterns(allowedOrigins.toArray(new String[0]));
    }
}

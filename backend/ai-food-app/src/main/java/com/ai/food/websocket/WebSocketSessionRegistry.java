package com.ai.food.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 集中记录活跃 WebSocket session 的 lastActiveAt,供 heartbeat job 扫 idle。
 * <p>ponytail: 避免 2 个 handler 各维护一份时间戳 + 各跑 Scheduled 线程,集中一处用一把 ConcurrentHashMap。</p>
 */
@Slf4j
@Component
public class WebSocketSessionRegistry {

    /** 5s 扫描周期,60s 未活跃即断开。 */
    private static final long IDLE_TIMEOUT_MS = 60_000L;

    /** ws key -> session,key 类型由调用方决定(ConversationHandler 用 path sessionId,ChatHandler 用 session.getId())。 */
    private final Map<Object, WebSocketSession> sessions = new ConcurrentHashMap<>();
    /** ws key -> lastActiveAt millis。 */
    private final Map<Object, Long> lastActiveAt = new ConcurrentHashMap<>();

    /** 注册一个新 session 并打初始时间戳。 */
    public void register(Object key, WebSocketSession session) {
        sessions.put(key, session);
        lastActiveAt.put(key, System.currentTimeMillis());
    }

    /** 收到任意 client 消息时调用,刷新 lastActiveAt。key 不存在时为 noop 但会写入条目 — 调用方应保证只 touch 已 register 的 key。 */
    public void touch(Object key) {
        if (key != null) lastActiveAt.put(key, System.currentTimeMillis());
    }

    /** 连接关闭/出错时清理。 */
    public void unregister(Object key) {
        if (key == null) return;
        sessions.remove(key);
        lastActiveAt.remove(key);
    }

    /**
     * 扫描 idle > 60s 的 session 并以 GOING_AWAY 关闭。
     * <p>ponytail: 全局一把 ConcurrentHashMap + 单线程 Scheduled,避免 2 个 handler 各跑造成 epoch race。</p>
     */
    public void sweepIdle() {
        long now = System.currentTimeMillis();
        lastActiveAt.forEach((key, last) -> {
            if (now - last <= IDLE_TIMEOUT_MS) return;
            WebSocketSession s = sessions.get(key);
            if (s != null && s.isOpen()) {
                try {
                    s.close(CloseStatus.GOING_AWAY.withReason("heartbeat timeout"));
                    log.info("Closed idle WS key={} (idle {}s)", key, (now - last) / 1000);
                } catch (IOException e) {
                    log.warn("Failed to close idle WS key={}", key, e);
                } finally {
                    unregister(key);
                }
            }
        });
    }
}

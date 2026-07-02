package com.ai.food.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 每 5s 扫一次 idle WebSocket session。
 * <p>ponytail: 全局一处 Scheduled 任务,集中扫描 — 单 connection 异常不会拖垮相邻 session。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketHeartbeatJob {

    private final WebSocketSessionRegistry registry;

    @Scheduled(fixedDelay = 5_000L)
    public void sweep() {
        registry.sweepIdle();
    }
}

package com.ai.food.util;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * RateLimiterUtil 烟雾测试。
 *
 * <p>只验证：第 N 次请求超限时抛 RuntimeException；阈值内通过；
 * 不同 key 互不干扰。不验证 expireAfterWrite 时序（那是 caffeine 内部逻辑）。
 */
@DisplayName("RateLimiterUtil 计数 + 阈值")
class RateLimiterUtilTest {

    private Cache<String, AtomicInteger> cache;

    @BeforeEach
    void setUp() {
        cache = Caffeine.newBuilder().maximumSize(100).build();
    }

    @Test
    @DisplayName("阈值内不抛异常")
    void withinLimit_doesNotThrow() {
        assertDoesNotThrow(() -> {
            for (int i = 0; i < 5; i++) {
                RateLimiterUtil.checkAndIncrement(cache, "user1:chat", 5);
            }
        });
    }

    @Test
    @DisplayName("超过阈值抛 RuntimeException")
    void overLimit_throws() {
        for (int i = 0; i < 5; i++) {
            RateLimiterUtil.checkAndIncrement(cache, "user2:chat", 5);
        }
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> RateLimiterUtil.checkAndIncrement(cache, "user2:chat", 5));
        assertEquals("AI 请求过于频繁，请稍后再试", ex.getMessage());
    }

    @Test
    @DisplayName("不同 key 互不干扰")
    void differentKeys_independent() {
        for (int i = 0; i < 5; i++) {
            RateLimiterUtil.checkAndIncrement(cache, "userA:chat", 5);
        }
        assertDoesNotThrow(() -> RateLimiterUtil.checkAndIncrement(cache, "userB:chat", 5));
    }

    @Test
    @DisplayName("计数器值在窗口内单调递增")
    void counterIncrementsMonotonically() {
        RateLimiterUtil.checkAndIncrement(cache, "user3:chat", 100);
        RateLimiterUtil.checkAndIncrement(cache, "user3:chat", 100);
        RateLimiterUtil.checkAndIncrement(cache, "user3:chat", 100);
        assertEquals(3, cache.getIfPresent("user3:chat").get());
    }
}

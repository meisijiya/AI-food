package com.ai.food.util;

import com.github.benmanes.caffeine.cache.Cache;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 简单滑动窗口计数器（基于 Caffeine AtomicInteger）。
 *
 * <p>典型用法：每 60s 内最多 30 次。counter 在 {@code expireAfterWrite(60s)}
 * 窗口内单调递增；窗口过期后 caffeine 自动重建条目并从 0 重新计数——这就是
 * "近似滑动窗口"的语义。对防 LLM 费用爆炸这类粗粒度限流足够。
 *
 * <p>注意：本类是单实例内存级限流，不跨节点同步。多实例部署时每个节点
 * 各有 30 次/60s 的额度，总量是 {@code 节点数 × 限流值}。对单机防护足够，
 * 多机协同请用 Redis 计数（见 {@link com.ai.food.common.config.RateLimitInterceptor}）。
 *
 * <p>并发安全：AtomicInteger.incrementAndGet 是原子操作，putIfAbsent 保证
 * 计数器只被初始化一次。
 */
public final class RateLimiterUtil {

    private RateLimiterUtil() {}

    /**
     * 计数器 +1，超过阈值抛 RuntimeException。
     *
     * @param cache  Caffeine 缓存（key=限流维度字符串，value=计数器）
     * @param key    限流维度（推荐格式：{@code "endpoint:userId"})
     * @param limit  窗口内最大次数
     */
    public static void checkAndIncrement(Cache<String, AtomicInteger> cache, String key, int limit) {
        AtomicInteger counter = cache.get(key, k -> new AtomicInteger(0));
        int current = counter.incrementAndGet();
        if (current > limit) {
            // ponytail: 不回退 counter——超限后短时间内反复请求仍会被拦，
            // 直到窗口过期；用户体验上 "试太多次就多等一会" 是合理反馈
            throw new RuntimeException("AI 请求过于频繁，请稍后再试");
        }
    }
}

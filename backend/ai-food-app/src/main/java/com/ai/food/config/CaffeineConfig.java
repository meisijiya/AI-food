package com.ai.food.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
public class CaffeineConfig {

    @Bean("emailLimitByUsername")
    public Cache<String, Boolean> emailLimitByUsername() {
        return Caffeine.newBuilder()
                .expireAfterWrite(60, TimeUnit.SECONDS)
                .maximumSize(10000)
                .build();
    }

    @Bean("emailLimitByIp")
    public Cache<String, Boolean> emailLimitByIp() {
        return Caffeine.newBuilder()
                .expireAfterWrite(60, TimeUnit.SECONDS)
                .maximumSize(50000)
                .build();
    }

    @Bean("hotPostLikeCache")
    public Cache<Long, Long> hotPostLikeCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(30, TimeUnit.SECONDS)
                .maximumSize(10000)
                .build();
    }

    @Bean("hotPostLikeStatusCache")
    public Cache<String, Boolean> hotPostLikeStatusCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(60, TimeUnit.SECONDS)
                .maximumSize(50000)
                .build();
    }

    // [P0-AI-限流] AI endpoint 滑动窗口计数器：key = userId+endpoint，60s 内 30 次
    // ponytail: 用 AtomicInteger 而不是 List<Long> 时间戳——Caffeine 不会因为大量过期
    // 时间戳数组触发 hot key GC，counter 在 put 时 expire-after-write 自带滑动效果
    @Bean("aiRateLimitCache")
    public Cache<String, AtomicInteger> aiRateLimitCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(60, TimeUnit.SECONDS)
                .maximumSize(100_000)
                .build();
    }

    // [P0-登录限流] AuthController login/register 用，key = email + ":" + ip，60s 内 5 次
    // ponytail: 复用现有 emailLimitByUsername / emailLimitByIp 模式（key 不同维度），无需新 bean

    // [P0-用户缓存] UserService.getUserInfo 缓存 5 分钟，update 时 evict
    @Bean("userInfoCache")
    public Cache<Long, com.ai.food.common.model.SysUser> userInfoCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .maximumSize(50_000)
                .build();
    }

    // [P0-关注缓存] FollowService.isFollowing 单条状态，60s 过期
    @Bean("followStatusCache")
    public Cache<String, Boolean> followStatusCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(60, TimeUnit.SECONDS)
                .maximumSize(100_000)
                .build();
    }

    // [P0-关注缓存] FollowService.getFollowingIds 关注列表，60s 过期
    @Bean("followingIdsCache")
    public Cache<Long, List<Long>> followingIdsCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(60, TimeUnit.SECONDS)
                .maximumSize(20_000)
                .build();
    }
}

package com.ai.food.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

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
}

package com.ai.food.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        return template;
    }

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        StringRedisTemplate template = new StringRedisTemplate();
        template.setConnectionFactory(connectionFactory);
        return template;
    }

    @Bean
    public RedisScript<Long> toggleLikeScript() {
        String script = """
            local setKey = KEYS[1]
            local userId = ARGV[1]
            local countKey = ARGV[2]
            local isMember = redis.call('SISMEMBER', setKey, userId)
            
            if isMember == 1 then
                redis.call('SREM', setKey, userId)
                local newCount = redis.call('DECR', countKey)
                if newCount < 0 then
                    redis.call('SET', countKey, 0)
                    newCount = 0
                end
                return -1
            else
                redis.call('SADD', setKey, userId)
                redis.call('INCR', countKey)
                return 1
            end
            """;
        return new DefaultRedisScript<>(script, Long.class);
    }

    @Bean
    public RedisScript<Long> batchIncrLikeCountScript() {
        String script = """
            local keys = KEYS
            local counts = ARGV
            local results = {}
            for i, countKey in ipairs(keys) do
                local current = tonumber(redis.call('GET', countKey) or '0')
                local increment = tonumber(counts[i] or '0')
                local newCount = current + increment
                redis.call('SET', countKey, newCount)
                table.insert(results, newCount)
            end
            return cjson.encode(results)
            """;
        return new DefaultRedisScript<>(script, Long.class);
    }
}
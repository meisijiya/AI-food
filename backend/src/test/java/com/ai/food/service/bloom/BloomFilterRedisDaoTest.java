package com.ai.food.service.bloom;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("BloomFilterRedisDao 随机候选读取")
class BloomFilterRedisDaoTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private SetOperations<String, String> setOperations;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Test
    @DisplayName("获取随机用户时不应破坏集合成员")
    void getRandomUserIdFromWithBitsSet_doesNotMutateSet() {
        BloomFilterRedisDao dao = new BloomFilterRedisDao(redisTemplate);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.size(anyString())).thenReturn(3L);
        when(setOperations.members(anyString())).thenReturn(Set.of("12", "15", "18"));

        Long result = dao.getRandomUserIdFromWithBitsSet(Set.of());

        org.junit.jupiter.api.Assertions.assertTrue(Set.of(12L, 15L, 18L).contains(result));
        verify(setOperations, never()).pop("bloom:users:with_bits");
        verify(setOperations, never()).randomMember("bloom:users:with_bits");
    }

    @Test
    @DisplayName("随机候选全被排除时返回空")
    void getRandomUserIdFromWithBitsSet_returnsNullWhenAllExcluded() {
        BloomFilterRedisDao dao = new BloomFilterRedisDao(redisTemplate);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.size(anyString())).thenReturn(1L);
        when(setOperations.members(anyString())).thenReturn(Set.of("7"));

        Long result = dao.getRandomUserIdFromWithBitsSet(Set.of(7L));

        assertNull(result);
    }

    @Test
    @DisplayName("Jaccard 相似度按交集除并集计算")
    void calculateSimilarity_usesIntersectionOverUnion() {
        BloomFilterRedisDao dao = new BloomFilterRedisDao(redisTemplate);
        byte[] left = new byte[32];
        byte[] right = new byte[32];
        left[0] = (byte) 0b11000000;
        right[0] = (byte) 0b10100000;

        double similarity = dao.calculateSimilarity(left, right);

        assertEquals(1.0 / 3.0, similarity, 0.0001);
    }

    @Test
    @DisplayName("候选集合较小时直接过滤排除列表后随机选择")
    void getRandomUserIdFromWithBitsSet_filtersMembersBeforeRandomPick() {
        BloomFilterRedisDao dao = new BloomFilterRedisDao(redisTemplate);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.size(anyString())).thenReturn(3L);
        when(setOperations.members(anyString())).thenReturn(Set.of("1", "6", "9"));

        Long result = dao.getRandomUserIdFromWithBitsSet(Set.of(1L, 6L));

        assertEquals(9L, result);
        verify(setOperations, never()).randomMember(anyString());
    }
}

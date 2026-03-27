package com.ai.food.service.bloom;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class BloomFilterRedisDao {

    private final StringRedisTemplate redisTemplate;

    private static final String BIT_ARRAY_KEY_PREFIX = "bloom:user:";
    private static final String QUEUE_KEY_PREFIX = "bloom:queue:";
    private static final String PENDING_SYNC_KEY = "bloom:pending:sync";
    private static final int BIT_SIZE = 256;
    private static final int BYTE_SIZE = 32;

    public byte[] getBitArray(Long userId) {
        String key = BIT_ARRAY_KEY_PREFIX + userId;
        String value = redisTemplate.opsForValue().get(key);
        if (value == null || value.isEmpty()) {
            return new byte[BYTE_SIZE];
        }
        return hexToBytes(value);
    }

    public void setBitArray(Long userId, byte[] bitArray) {
        String key = BIT_ARRAY_KEY_PREFIX + userId;
        redisTemplate.opsForValue().set(key, bytesToHex(bitArray));
    }

    public void setBit(Long userId, int position) {
        byte[] bitArray = getBitArray(userId);
        int byteIndex = position / 8;
        int bitIndex = position % 8;
        bitArray[byteIndex] |= (1 << (7 - bitIndex));
        setBitArray(userId, bitArray);
    }

    public void clearBit(Long userId, int position) {
        byte[] bitArray = getBitArray(userId);
        int byteIndex = position / 8;
        int bitIndex = position % 8;
        bitArray[byteIndex] &= ~(1 << (7 - bitIndex));
        setBitArray(userId, bitArray);
    }

    public void clearAllBits(Long userId) {
        setBitArray(userId, new byte[BYTE_SIZE]);
    }

    public List<String> getQueue(Long userId) {
        String key = QUEUE_KEY_PREFIX + userId;
        return redisTemplate.opsForList().range(key, 0, -1);
    }

    public Long pushToQueue(Long userId, String recordId) {
        String key = QUEUE_KEY_PREFIX + userId;
        return redisTemplate.opsForList().rightPush(key, recordId);
    }

    public String popFromQueue(Long userId) {
        String key = QUEUE_KEY_PREFIX + userId;
        return redisTemplate.opsForList().leftPop(key);
    }

    public Long getQueueSize(Long userId) {
        String key = QUEUE_KEY_PREFIX + userId;
        return redisTemplate.opsForList().size(key);
    }

    public void removeFromQueue(Long userId, String recordId) {
        String key = QUEUE_KEY_PREFIX + userId;
        redisTemplate.opsForList().remove(key, 1, recordId);
    }

    public void markPendingSync(Long userId) {
        redisTemplate.opsForSet().add(PENDING_SYNC_KEY, userId.toString());
    }

    public Set<String> getPendingSyncUsers() {
        return redisTemplate.opsForSet().members(PENDING_SYNC_KEY);
    }

    public void removePendingSync(Long userId) {
        redisTemplate.opsForSet().remove(PENDING_SYNC_KEY, userId.toString());
    }

    public void removePendingSyncBatch(List<Long> userIds) {
        for (Long userId : userIds) {
            removePendingSync(userId);
        }
    }

    public int bitCount(byte[] bitArray) {
        int count = 0;
        for (byte b : bitArray) {
            count += Integer.bitCount(b & 0xFF);
        }
        return count;
    }

    public double calculateSimilarity(byte[] arrayA, byte[] arrayB) {
        if (arrayA == null) arrayA = new byte[BYTE_SIZE];
        if (arrayB == null) arrayB = new byte[BYTE_SIZE];
        
        int commonBits = 0;
        for (int i = 0; i < BYTE_SIZE; i++) {
            int intersection = (arrayA[i] & 0xFF) & (arrayB[i] & 0xFF);
            commonBits += Integer.bitCount(intersection);
        }
        return (double) commonBits / BIT_SIZE;
    }

    private byte[] hexToBytes(String hex) {
        if (hex == null || hex.length() % 2 != 0) {
            return new byte[BYTE_SIZE];
        }
        byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
        }
        return bytes;
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b & 0xFF));
        }
        return sb.toString();
    }
}
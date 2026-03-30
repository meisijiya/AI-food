package com.ai.food.service.bloom;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;

@Slf4j
@Component
public class BloomFilterRedisDao {

    private final StringRedisTemplate redisTemplate;

    private static final String BIT_ARRAY_KEY_PREFIX = "bloom:user:";
    private static final String QUEUE_KEY_PREFIX = "bloom:queue:";
    private static final String RECORD_VALUE_KEY_PREFIX = "bloom:record:value:";
    private static final String PENDING_SYNC_KEY = "bloom:pending:sync";
    private static final String USERS_WITH_BITS_KEY = "bloom:users:with_bits";
    private static final int BIT_SIZE = 256;
    private static final int BYTE_SIZE = 32;

    /**
     * 显式构造器用于稳定测试和运行时注入。
     */
    public BloomFilterRedisDao(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 保存单条推荐记录的参数串，供窗口淘汰和删除时重建位图使用。
     */
    public void saveRecordValue(Long userId, String recordId, String paramValue) {
        redisTemplate.opsForHash().put(RECORD_VALUE_KEY_PREFIX + userId, recordId, paramValue);
    }

    /**
     * 读取单条推荐记录的参数串。
     */
    public String getRecordValue(Long userId, String recordId) {
        Object value = redisTemplate.opsForHash().get(RECORD_VALUE_KEY_PREFIX + userId, recordId);
        return value != null ? value.toString() : null;
    }

    /**
     * 删除单条推荐记录缓存的参数串。
     */
    public void removeRecordValue(Long userId, String recordId) {
        redisTemplate.opsForHash().delete(RECORD_VALUE_KEY_PREFIX + userId, recordId);
    }

    public byte[] getBitArray(Long userId) {
        String key = BIT_ARRAY_KEY_PREFIX + userId;
        String value = redisTemplate.opsForValue().get(key);
        if (value == null || value.isEmpty()) {
            log.debug("Bloom Redis miss: userId={}, key={}", userId, key);
            return new byte[BYTE_SIZE];
        }
        log.debug("Bloom Redis hit: userId={}, key={}, hexLength={}", userId, key, value.length());
        return hexToBytes(value);
    }

    public void setBitArray(Long userId, byte[] bitArray) {
        String key = BIT_ARRAY_KEY_PREFIX + userId;
        redisTemplate.opsForValue().set(key, bytesToHex(bitArray));
        log.debug("Bloom Redis write: userId={}, key={}, bitCount={}", userId, key, bitCount(bitArray));
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
        int unionBits = 0;
        for (int i = 0; i < BYTE_SIZE; i++) {
            int intersection = (arrayA[i] & 0xFF) & (arrayB[i] & 0xFF);
            int union = (arrayA[i] & 0xFF) | (arrayB[i] & 0xFF);
            commonBits += Integer.bitCount(intersection);
            unionBits += Integer.bitCount(union);
        }
        if (unionBits == 0) {
            return 0.0;
        }
        return (double) commonBits / unionBits;
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

    public void addUserToWithBitsSet(Long userId) {
        redisTemplate.opsForSet().add(USERS_WITH_BITS_KEY, userId.toString());
    }

    public void removeUserFromWithBitsSet(Long userId) {
        redisTemplate.opsForSet().remove(USERS_WITH_BITS_KEY, userId.toString());
    }

    public Long getWithBitsSetSize() {
        return redisTemplate.opsForSet().size(USERS_WITH_BITS_KEY);
    }

    public Long getRandomUserIdFromWithBitsSet(Set<Long> excludeIds) {
        Long size = getWithBitsSetSize();
        log.debug("Bloom candidate pool: size={}, excludeIds={}", size, excludeIds);
        if (size == null || size == 0) {
            log.warn("Bloom candidate pool empty");
            return null;
        }
        if (size <= 32) {
            Set<String> members = redisTemplate.opsForSet().members(USERS_WITH_BITS_KEY);
            if (members == null || members.isEmpty()) {
                log.warn("Bloom candidate pool members empty");
                return null;
            }
            List<Long> candidates = new ArrayList<>();
            for (String member : members) {
                Long userId = Long.parseLong(member);
                if (excludeIds != null && excludeIds.contains(userId)) {
                    continue;
                }
                candidates.add(userId);
            }
            if (candidates.isEmpty()) {
                log.warn("Bloom candidate pool exhausted after filtering: excludeIds={}", excludeIds);
                return null;
            }
            Long selected = candidates.get((int) (Math.random() * candidates.size()));
            log.debug("Bloom candidate selected from filtered members: candidateId={}", selected);
            return selected;
        }
        int maxRetries = 10;
        for (int i = 0; i < maxRetries; i++) {
            String userIdStr = redisTemplate.opsForSet().randomMember(USERS_WITH_BITS_KEY);
            log.debug("Bloom random member attempt {}: rawCandidate={}", i + 1, userIdStr);
            if (userIdStr == null) {
                log.warn("Bloom random member returned null on attempt {}", i + 1);
                return null;
            }
            Long userId = Long.parseLong(userIdStr);
            if (excludeIds != null && excludeIds.contains(userId)) {
                log.debug("Bloom candidate skipped by exclude list: candidateId={}", userId);
                continue;
            }
            log.debug("Bloom candidate selected: candidateId={}", userId);
            return userId;
        }
        log.warn("Bloom candidate selection exhausted retries: excludeIds={}", excludeIds);
        return null;
    }
}

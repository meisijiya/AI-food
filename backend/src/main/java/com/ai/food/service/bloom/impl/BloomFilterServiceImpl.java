package com.ai.food.service.bloom.impl;

import com.ai.food.dto.UserSimilarityDTO;
import com.ai.food.model.SysUser;
import com.ai.food.repository.UserRepository;
import com.ai.food.service.bloom.BloomFilterRedisDao;
import com.ai.food.service.bloom.BloomFilterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BloomFilterServiceImpl implements BloomFilterService {

    private final BloomFilterRedisDao redisDao;
    private final UserRepository userRepository;

    private static final int BIT_SIZE = 256;
    private static final int HASH_FUNCTIONS = 3;
    private static final int WINDOW_SIZE = 10;
    private static final String[] HASH_SALTS = {"bloom_salt1", "bloom_salt2", "bloom_salt3"};

    @Override
    public void addRecommendation(Long userId, String recordId, String paramValue) {
        Long queueSize = redisDao.getQueueSize(userId);
        if (queueSize != null && queueSize >= WINDOW_SIZE) {
            List<String> queue = redisDao.getQueue(userId);
            if (queue != null && !queue.isEmpty()) {
                String oldestRecordId = queue.get(0);
                removeRecommendation(userId, oldestRecordId, null);
            }
        }

        redisDao.pushToQueue(userId, recordId);

        int[] bitPositions = getBitPositions(paramValue);
        for (int pos : bitPositions) {
            redisDao.setBit(userId, pos);
        }

        redisDao.markPendingSync(userId);
        log.debug("Added recommendation for user {}, record {}, bits {}", userId, recordId, Arrays.toString(bitPositions));
    }

    @Override
    public void removeRecommendation(Long userId, String recordId, String paramValue) {
        if (paramValue == null || paramValue.isEmpty()) {
            redisDao.removeFromQueue(userId, recordId);
            return;
        }

        int[] bitPositions = getBitPositions(paramValue);
        for (int pos : bitPositions) {
            redisDao.clearBit(userId, pos);
        }

        redisDao.removeFromQueue(userId, recordId);
        redisDao.markPendingSync(userId);
        log.debug("Removed recommendation for user {}, record {}", userId, recordId);
    }

    @Override
    public byte[] getBitArray(Long userId) {
        return redisDao.getBitArray(userId);
    }

    @Override
    public double calculateSimilarity(Long userIdA, Long userIdB) {
        byte[] bitArrayA = redisDao.getBitArray(userIdA);
        byte[] bitArrayB = redisDao.getBitArray(userIdB);
        return redisDao.calculateSimilarity(bitArrayA, bitArrayB);
    }

    @Override
    public List<UserSimilarityDTO> getTopKSimilarUsers(Long userId, int k) {
        byte[] currentBitArray = redisDao.getBitArray(userId);
        
        List<SysUser> allUsers = userRepository.findAll();
        
        List<UserSimilarityDTO> similarities = new ArrayList<>();
        for (SysUser user : allUsers) {
            if (user.getId().equals(userId)) {
                continue;
            }
            
            byte[] userBitArray = redisDao.getBitArray(user.getId());
            if (userBitArray == null || isEmptyBitArray(userBitArray)) {
                continue;
            }
            
            double similarity = redisDao.calculateSimilarity(currentBitArray, userBitArray);
            if (similarity > 0) {
                similarities.add(new UserSimilarityDTO(
                        user.getId(),
                        user.getNickname(),
                        Math.round(similarity * 100.0) / 100.0
                ));
            }
        }

        return similarities.stream()
                .sorted(Comparator.comparingDouble(UserSimilarityDTO::getSimilarity).reversed())
                .limit(k)
                .collect(Collectors.toList());
    }

    @Override
    public int getRecordCount(Long userId) {
        Long size = redisDao.getQueueSize(userId);
        return size != null ? size.intValue() : 0;
    }

    @Override
    public List<String> getRecentRecords(Long userId) {
        List<String> queue = redisDao.getQueue(userId);
        return queue != null ? queue : Collections.emptyList();
    }

    private int[] getBitPositions(String value) {
        int[] positions = new int[HASH_FUNCTIONS];
        positions[0] = murmurHash(value) & 0xFF;
        positions[1] = murmurHash(value + HASH_SALTS[0]) & 0xFF;
        positions[2] = murmurHash(value + HASH_SALTS[1]) & 0xFF;
        return positions;
    }

    private int murmurHash(String value) {
        byte[] data = value.getBytes();
        int length = data.length;
        int seed = 0x9747b28c;

        final int m = 0x5bd1e995;
        final int r = 24;

        int h = seed ^ length;
        int len = length;
        int i = 0;

        while (len >= 4) {
            int k = (data[i] & 0xff)
                    | ((data[i + 1] & 0xff) << 8)
                    | ((data[i + 2] & 0xff) << 16)
                    | ((data[i + 3] & 0xff) << 24);

            k *= m;
            k ^= k >>> r;
            k *= m;
            h *= m;
            h ^= k;

            i += 4;
            len -= 4;
        }

        switch (len) {
            case 3:
                h ^= (data[i + 2] & 0xff) << 16;
            case 2:
                h ^= (data[i + 1] & 0xff) << 8;
            case 1:
                h ^= (data[i] & 0xff);
                h *= m;
        }

        h ^= h >>> 13;
        h *= m;
        h ^= h >>> 15;

        return h;
    }

    private boolean isEmptyBitArray(byte[] bitArray) {
        for (byte b : bitArray) {
            if (b != 0) {
                return false;
            }
        }
        return true;
    }
}
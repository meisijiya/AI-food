package com.ai.food.service.bloom.impl;

import com.ai.food.dto.MatchUserDetailDTO;
import com.ai.food.dto.UserSimilarityDTO;
import com.ai.food.model.CollectedParam;
import com.ai.food.model.ConversationSession;
import com.ai.food.model.SysUser;
import com.ai.food.repository.CollectedParamRepository;
import com.ai.food.repository.ConversationSessionRepository;
import com.ai.food.repository.RecommendationResultRepository;
import com.ai.food.repository.UserRepository;
import com.ai.food.service.bloom.BloomFilterRedisDao;
import com.ai.food.service.bloom.BloomFilterService;
import com.ai.food.service.follow.FollowService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class BloomFilterServiceImpl implements BloomFilterService {

    private final BloomFilterRedisDao redisDao;
    private final UserRepository userRepository;
    private final ConversationSessionRepository conversationSessionRepository;
    private final CollectedParamRepository collectedParamRepository;
    private final RecommendationResultRepository recommendationResultRepository;
    private final FollowService followService;

    private static final int BIT_SIZE = 256;
    private static final int HASH_FUNCTIONS = 3;
    private static final int WINDOW_SIZE = 10;
    private static final String[] HASH_SALTS = {"bloom_salt1", "bloom_salt2", "bloom_salt3"};

    /**
     * 显式构造器用于稳定测试和依赖注入。
     */
    public BloomFilterServiceImpl(BloomFilterRedisDao redisDao,
                                  UserRepository userRepository,
                                  ConversationSessionRepository conversationSessionRepository,
                                  CollectedParamRepository collectedParamRepository,
                                  RecommendationResultRepository recommendationResultRepository,
                                  FollowService followService) {
        this.redisDao = redisDao;
        this.userRepository = userRepository;
        this.conversationSessionRepository = conversationSessionRepository;
        this.collectedParamRepository = collectedParamRepository;
        this.recommendationResultRepository = recommendationResultRepository;
        this.followService = followService;
    }

    @Override
    public void addRecommendation(Long userId, String recordId, String paramValue) {
        log.debug("Bloom addRecommendation start: userId={}, recordId={}, paramValue={}", userId, recordId, paramValue);
        Long queueSize = redisDao.getQueueSize(userId);
        if (queueSize != null && queueSize >= WINDOW_SIZE) {
            List<String> queue = redisDao.getQueue(userId);
            if (queue != null && !queue.isEmpty()) {
                String oldestRecordId = queue.get(0);
                removeRecommendation(userId, oldestRecordId, null);
            }
        }

        redisDao.pushToQueue(userId, recordId);
        redisDao.saveRecordValue(userId, recordId, paramValue);
        rebuildBitArrayFromQueue(userId);
        log.debug("Added recommendation for user {}, record {}", userId, recordId);
    }

    @Override
    public void removeRecommendation(Long userId, String recordId, String paramValue) {
        redisDao.removeRecordValue(userId, recordId);
        redisDao.removeFromQueue(userId, recordId);
        rebuildBitArrayFromQueue(userId);
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

    @Override
    public MatchUserDetailDTO getRandomSimilarUser(Long userId, Set<Long> excludeIds) {
        byte[] currentBitArray = redisDao.getBitArray(userId);
        log.debug("Bloom random-match start: userId={}, excludeIds={}, currentBitCount={}",
                userId, excludeIds, redisDao.bitCount(currentBitArray));
        if (currentBitArray == null || isEmptyBitArray(currentBitArray)) {
            log.warn("Bloom random-match aborted: current user bit array empty, userId={}", userId);
            return null;
        }

        Set<Long> excludeWithSelf = new java.util.HashSet<>();
        if (excludeIds != null) {
            excludeWithSelf.addAll(excludeIds);
        }
        excludeWithSelf.add(userId);

        int maxRetries = 10;
        for (int i = 0; i < maxRetries; i++) {
            Long candidateId = redisDao.getRandomUserIdFromWithBitsSet(excludeWithSelf);
            if (candidateId == null) {
                log.warn("Bloom random-match aborted: no candidate found, userId={}, excludeIds={}", userId, excludeWithSelf);
                return null;
            }

            byte[] candidateBitArray = redisDao.getBitArray(candidateId);
            log.debug("Bloom random-match candidate: attempt={}, candidateId={}, candidateBitCount={}",
                    i + 1, candidateId, redisDao.bitCount(candidateBitArray));
            if (candidateBitArray == null || isEmptyBitArray(candidateBitArray)) {
                redisDao.removeUserFromWithBitsSet(candidateId);
                log.warn("Bloom random-match candidate removed due to empty bit array: candidateId={}", candidateId);
                continue;
            }

            double similarity = redisDao.calculateSimilarity(currentBitArray, candidateBitArray);
            log.debug("Bloom random-match similarity: userId={}, candidateId={}, similarity={}",
                    userId, candidateId, similarity);
            if (similarity > 0) {
                SysUser targetUser = userRepository.findById(candidateId).orElse(null);
                if (targetUser == null) {
                    log.warn("Bloom random-match candidate missing in database: candidateId={}", candidateId);
                    continue;
                }

                boolean isFollowing = followService.isFollowing(userId, candidateId);
                Map<String, String> collectedParams = getUserCollectedParams(candidateId);
                String foodName = getLatestFoodName(candidateId);

                return new MatchUserDetailDTO(
                        targetUser.getId(),
                        targetUser.getNickname(),
                        targetUser.getAvatar(),
                        foodName,
                        Math.round(similarity * 100.0) / 100.0,
                        isFollowing,
                        collectedParams
                );
            }

            excludeWithSelf.add(candidateId);
            log.debug("Bloom random-match candidate excluded after zero similarity: candidateId={}, excludeIds={}",
                    candidateId, excludeWithSelf);
        }

        log.warn("Bloom random-match exhausted retries with no match: userId={}, excludeIds={}", userId, excludeWithSelf);
        return null;
    }

    private Map<String, String> getUserCollectedParams(Long userId) {
        Map<String, String> params = new LinkedHashMap<>();

        List<ConversationSession> sessions = conversationSessionRepository.findTop10ByUserIdOrderByCreatedAtDesc(userId);
        if (sessions.isEmpty()) {
            return params;
        }

        Set<String> paramNames = new LinkedHashSet<>();
        for (ConversationSession session : sessions) {
            List<CollectedParam> collectedParams = collectedParamRepository.findBySessionId(session.getSessionId());
            for (CollectedParam param : collectedParams) {
                if (param.getParamName() != null && param.getParamValue() != null) {
                    paramNames.add(param.getParamName());
                }
            }
        }

        for (String paramName : paramNames) {
            for (ConversationSession session : sessions) {
                Optional<CollectedParam> param = collectedParamRepository.findBySessionIdAndParamName(
                        session.getSessionId(), paramName);
                if (param.isPresent() && param.get().getParamValue() != null) {
                    params.put(paramName, param.get().getParamValue());
                    break;
                }
            }
        }

        return params;
    }

    /**
     * 获取目标用户最近一次推荐结果里的食物名，供匹配页直观展示。
     */
    private String getLatestFoodName(Long userId) {
        List<ConversationSession> sessions = conversationSessionRepository.findTop10ByUserIdOrderByCreatedAtDesc(userId);
        for (ConversationSession session : sessions) {
            Optional<String> foodName = recommendationResultRepository.findBySessionId(session.getSessionId())
                    .map(result -> result.getFoodName())
                    .filter(name -> name != null && !name.isBlank());
            if (foodName.isPresent()) {
                return foodName.get();
            }
        }
        return null;
    }

    /**
     * 以当前窗口内的记录为准重建位图，避免删除和哈希碰撞导致画像失真。
     */
    private void rebuildBitArrayFromQueue(Long userId) {
        redisDao.clearAllBits(userId);

        List<String> queue = redisDao.getQueue(userId);
        log.debug("Bloom rebuild start: userId={}, queue={}", userId, queue);
        if (queue == null || queue.isEmpty()) {
            redisDao.removeUserFromWithBitsSet(userId);
            redisDao.markPendingSync(userId);
            log.debug("Bloom rebuild finished with empty queue: userId={}", userId);
            return;
        }

        boolean hasBits = false;
        for (String queuedRecordId : queue) {
            String storedValue = redisDao.getRecordValue(userId, queuedRecordId);
            log.debug("Bloom rebuild record: userId={}, recordId={}, storedValue={}", userId, queuedRecordId, storedValue);
            if (storedValue == null || storedValue.isBlank()) {
                log.warn("Bloom rebuild skipped blank stored value: userId={}, recordId={}", userId, queuedRecordId);
                continue;
            }

            for (String token : tokenizeStoredValue(storedValue)) {
                int[] bitPositions = getBitPositions(token);
                for (int pos : bitPositions) {
                    redisDao.setBit(userId, pos);
                }
                hasBits = true;
            }
        }

        if (hasBits) {
            redisDao.addUserToWithBitsSet(userId);
        } else {
            redisDao.removeUserFromWithBitsSet(userId);
        }
        redisDao.markPendingSync(userId);
        log.debug("Bloom rebuild finished: userId={}, hasBits={}, finalBitCount={}",
                userId, hasBits, redisDao.bitCount(redisDao.getBitArray(userId)));
    }

    private int[] getBitPositions(String value) {
        int[] positions = new int[HASH_FUNCTIONS];
        positions[0] = murmurHash(value) & 0xFF;
        positions[1] = murmurHash(value + HASH_SALTS[0]) & 0xFF;
        positions[2] = murmurHash(value + HASH_SALTS[1]) & 0xFF;
        return positions;
    }

    /**
     * 将标准化后的记录值拆为稳定 token 列表，兼容每条推荐记录参数个数不同的情况。
     */
    private List<String> tokenizeStoredValue(String storedValue) {
        if (storedValue == null || storedValue.isBlank()) {
            return List.of();
        }
        return Arrays.stream(storedValue.split("\\|"))
                .map(String::trim)
                .filter(token -> !token.isBlank())
                .distinct()
                .sorted()
                .toList();
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

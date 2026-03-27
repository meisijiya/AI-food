package com.ai.food.service.bloom.impl;

import com.ai.food.model.BloomSyncLog;
import com.ai.food.model.UserBloomFilter;
import com.ai.food.repository.BloomSyncLogRepository;
import com.ai.food.repository.UserBloomFilterRepository;
import com.ai.food.service.bloom.BloomFilterRedisDao;
import com.ai.food.service.bloom.BloomPersistenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class BloomPersistenceServiceImpl implements BloomPersistenceService {

    private final UserBloomFilterRepository userBloomFilterRepository;
    private final BloomSyncLogRepository bloomSyncLogRepository;
    private final BloomFilterRedisDao redisDao;

    @Override
    @Transactional
    public void syncRedisToMySQL() {
        Set<String> pendingUsers = redisDao.getPendingSyncUsers();
        if (pendingUsers == null || pendingUsers.isEmpty()) {
            log.debug("No pending users to sync");
            return;
        }

        log.info("Starting sync for {} users", pendingUsers.size());
        int successCount = 0;
        int failCount = 0;

        for (String userIdStr : pendingUsers) {
            try {
                Long userId = Long.parseLong(userIdStr);
                syncUser(userId);
                redisDao.removePendingSync(userId);
                successCount++;
            } catch (Exception e) {
                log.error("Failed to sync user {}: {}", userIdStr, e.getMessage());
                failCount++;
                saveSyncLog(Long.parseLong(userIdStr), "redis_to_mysql", "failed", e.getMessage());
            }
        }

        log.info("Sync completed: success={}, failed={}", successCount, failCount);
    }

    @Override
    @Transactional
    public void restoreRedisFromMySQL(Long userId) {
        Optional<UserBloomFilter> optFilter = userBloomFilterRepository.findByUserId(userId);
        if (optFilter.isPresent()) {
            UserBloomFilter filter = optFilter.get();
            redisDao.setBitArray(userId, filter.getBitArray());
            List<String> queue = redisDao.getQueue(userId);
            if (queue != null && !queue.isEmpty()) {
                for (String recordId : queue) {
                    redisDao.pushToQueue(userId, recordId);
                }
            }
            log.info("Restored bloom filter for user {} from MySQL", userId);
            saveSyncLog(userId, "mysql_to_redis", "success", null);
        }
    }

    @Override
    @Transactional
    public void restoreAllFromMySQL() {
        List<UserBloomFilter> allFilters = userBloomFilterRepository.findAll();
        log.info("Restoring {} bloom filters from MySQL", allFilters.size());
        
        for (UserBloomFilter filter : allFilters) {
            try {
                redisDao.setBitArray(filter.getUserId(), filter.getBitArray());
            } catch (Exception e) {
                log.error("Failed to restore bloom filter for user {}: {}", filter.getUserId(), e.getMessage());
            }
        }
    }

    @Override
    @Transactional
    public void syncUser(Long userId) {
        byte[] bitArray = redisDao.getBitArray(userId);
        Long queueSize = redisDao.getQueueSize(userId);
        List<String> queue = redisDao.getQueue(userId);
        String lastRecordId = (queue != null && !queue.isEmpty()) ? queue.get(queue.size() - 1) : null;

        Optional<UserBloomFilter> optFilter = userBloomFilterRepository.findByUserId(userId);
        UserBloomFilter filter;
        
        if (optFilter.isPresent()) {
            filter = optFilter.get();
            filter.setBitArray(bitArray);
            filter.setRecordCount(queueSize != null ? queueSize.intValue() : 0);
            filter.setLastRecordId(lastRecordId);
        } else {
            filter = new UserBloomFilter();
            filter.setUserId(userId);
            filter.setBitArray(bitArray);
            filter.setRecordCount(queueSize != null ? queueSize.intValue() : 0);
            filter.setLastRecordId(lastRecordId);
        }

        userBloomFilterRepository.save(filter);
        saveSyncLog(userId, "redis_to_mysql", "success", null);
        log.debug("Synced user {} to MySQL", userId);
    }

    private void saveSyncLog(Long userId, String syncType, String status, String errorMsg) {
        try {
            BloomSyncLog syncLog = new BloomSyncLog();
            syncLog.setUserId(userId);
            syncLog.setSyncType(syncType);
            syncLog.setStatus(status);
            syncLog.setErrorMsg(errorMsg);
            bloomSyncLogRepository.save(syncLog);
        } catch (Exception e) {
            log.error("Failed to save sync log: {}", e.getMessage());
        }
    }
}
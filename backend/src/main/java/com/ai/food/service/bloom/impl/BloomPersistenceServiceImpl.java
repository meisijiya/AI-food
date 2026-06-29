package com.ai.food.service.bloom.impl;

import com.ai.food.common.mapper.BloomSyncLogMapper;
import com.ai.food.common.mapper.UserBloomFilterMapper;
import com.ai.food.common.model.BloomSyncLog;
import com.ai.food.common.model.UserBloomFilter;
import com.ai.food.service.bloom.BloomFilterRedisDao;
import com.ai.food.service.bloom.BloomPersistenceService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 布隆过滤器持久化服务（MyBatis-Plus 迁移版）。
 * <p>
 * 继承 {@link ServiceImpl} 后，{@code baseMapper} 指向 {@link UserBloomFilterMapper}；
 * 同步日志走注入的 {@link BloomSyncLogMapper}。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BloomPersistenceServiceImpl extends ServiceImpl<UserBloomFilterMapper, UserBloomFilter> implements BloomPersistenceService {

    private final BloomSyncLogMapper bloomSyncLogMapper;
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
        UserBloomFilter filter = baseMapper.findByUserId(userId);
        if (filter != null) {
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
        List<UserBloomFilter> allFilters = baseMapper.selectList(null);
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

        UserBloomFilter existing = baseMapper.findByUserId(userId);
        UserBloomFilter filter;

        if (existing != null) {
            filter = existing;
            filter.setBitArray(bitArray);
            filter.setRecordCount(queueSize != null ? queueSize.intValue() : 0);
            filter.setLastRecordId(lastRecordId);
            baseMapper.updateById(filter);
        } else {
            filter = new UserBloomFilter();
            filter.setUserId(userId);
            filter.setBitArray(bitArray);
            filter.setRecordCount(queueSize != null ? queueSize.intValue() : 0);
            filter.setLastRecordId(lastRecordId);
            baseMapper.insert(filter);
        }

        saveSyncLog(userId, "redis_to_mysql", "success", null);
        log.debug("Synced user {} to MySQL", userId);
    }

    /**
     * 写入一条同步日志（失败也不抛错，避免掩盖主流程异常）。
     */
    private void saveSyncLog(Long userId, String syncType, String status, String errorMsg) {
        try {
            BloomSyncLog syncLog = new BloomSyncLog();
            syncLog.setUserId(userId);
            syncLog.setSyncType(syncType);
            syncLog.setStatus(status);
            syncLog.setErrorMsg(errorMsg);
            bloomSyncLogMapper.insert(syncLog);
        } catch (Exception e) {
            log.error("Failed to save sync log: {}", e.getMessage());
        }
    }
}

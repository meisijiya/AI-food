package com.ai.food.job;

import com.ai.food.service.bloom.BloomPersistenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BloomSyncJob extends QuartzJobBean {

    private final BloomPersistenceService bloomPersistenceService;

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        log.info("=== 开始同步布隆过滤器到MySQL ===");
        try {
            bloomPersistenceService.syncRedisToMySQL();
            log.info("=== 布隆过滤器同步完成 ===");
        } catch (Exception e) {
            log.error("布隆过滤器同步失败: {}", e.getMessage(), e);
            throw new JobExecutionException(e);
        }
    }
}
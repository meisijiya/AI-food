package com.ai.food.config;

import com.ai.food.job.BloomSyncJob;
import com.ai.food.job.CleanupSoftDeletedJob;
import org.quartz.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QuartzConfig {

    @Bean
    public JobDetail cleanupJobDetail() {
        return JobBuilder.newJob(CleanupSoftDeletedJob.class)
                .withIdentity("cleanupSoftDeletedJob")
                .withDescription("每天凌晨3点清理软删除记录")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger cleanupJobTrigger() {
        CronScheduleBuilder schedule = CronScheduleBuilder
                .cronSchedule("0 0 3 * * ?")
                .withMisfireHandlingInstructionFireAndProceed();

        return TriggerBuilder.newTrigger()
                .forJob(cleanupJobDetail())
                .withIdentity("cleanupSoftDeletedTrigger")
                .withSchedule(schedule)
                .build();
    }

    @Bean
    public JobDetail bloomSyncJobDetail() {
        return JobBuilder.newJob(BloomSyncJob.class)
                .withIdentity("bloomSyncJob")
                .withDescription("每小时同步布隆过滤器到MySQL")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger bloomSyncJobTrigger() {
        CronScheduleBuilder schedule = CronScheduleBuilder
                .cronSchedule("0 0 * * * ?")
                .withMisfireHandlingInstructionFireAndProceed();

        return TriggerBuilder.newTrigger()
                .forJob(bloomSyncJobDetail())
                .withIdentity("bloomSyncTrigger")
                .withSchedule(schedule)
                .build();
    }
}

package com.ai.food.config;

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
        // 每天凌晨 3:00 执行
        CronScheduleBuilder schedule = CronScheduleBuilder
                .cronSchedule("0 0 3 * * ?")
                .withMisfireHandlingInstructionFireAndProceed();

        return TriggerBuilder.newTrigger()
                .forJob(cleanupJobDetail())
                .withIdentity("cleanupSoftDeletedTrigger")
                .withSchedule(schedule)
                .build();
    }
}

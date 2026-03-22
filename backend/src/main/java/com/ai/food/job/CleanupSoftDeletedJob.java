package com.ai.food.job;

import com.ai.food.repository.CollectedParamRepository;
import com.ai.food.repository.ConversationSessionRepository;
import com.ai.food.repository.QaRecordRepository;
import com.ai.food.repository.RecommendationResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CleanupSoftDeletedJob extends QuartzJobBean {

    private final ConversationSessionRepository conversationSessionRepository;
    private final QaRecordRepository qaRecordRepository;
    private final CollectedParamRepository collectedParamRepository;
    private final RecommendationResultRepository recommendationResultRepository;

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        log.info("=== 开始清理软删除记录 ===");

        int deleted = 0;

        // 按依赖顺序删除：子表先删，主表后删
        deleted += qaRecordRepository.hardDeleteAllSoftDeleted();
        log.info("清理 qa_record: {} 条", deleted);

        int cpDeleted = collectedParamRepository.hardDeleteAllSoftDeleted();
        log.info("清理 collected_params: {} 条", cpDeleted);
        deleted += cpDeleted;

        int rrDeleted = recommendationResultRepository.hardDeleteAllSoftDeleted();
        log.info("清理 recommendation_result: {} 条", rrDeleted);
        deleted += rrDeleted;

        int csDeleted = conversationSessionRepository.hardDeleteAllSoftDeleted();
        log.info("清理 conversation_session: {} 条", csDeleted);
        deleted += csDeleted;

        log.info("=== 软删除记录清理完成，共清理 {} 条 ===", deleted);
    }
}

package com.ai.food.job;

import com.ai.food.model.Photo;
import com.ai.food.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CleanupSoftDeletedJob extends QuartzJobBean {

    private final FeedCommentRepository feedCommentRepository;
    private final FeedPostRepository feedPostRepository;
    private final PhotoRepository photoRepository;
    private final QaRecordRepository qaRecordRepository;
    private final CollectedParamRepository collectedParamRepository;
    private final RecommendationResultRepository recommendationResultRepository;
    private final ConversationSessionRepository conversationSessionRepository;

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        log.info("=== 开始清理软删除记录 ===");
        int total = 0;

        // 1. 先清理评论（依赖 feed_post 的外键）
        int fcDeleted = feedCommentRepository.hardDeleteAllSoftDeleted();
        log.info("清理 feed_comment: {} 条", fcDeleted);
        total += fcDeleted;

        // 2. 清理动态（依赖 session）
        int fpDeleted = feedPostRepository.hardDeleteAllSoftDeleted();
        log.info("清理 feed_post: {} 条", fpDeleted);
        total += fpDeleted;

        // 3. 清理照片（先删物理文件，再删数据库记录）
        int photoDeleted = cleanupPhotos();
        log.info("清理 photo: {} 条（含物理文件）", photoDeleted);
        total += photoDeleted;

        // 4. 清理推荐相关子表
        total += qaRecordRepository.hardDeleteAllSoftDeleted();
        log.info("清理 qa_record");

        total += collectedParamRepository.hardDeleteAllSoftDeleted();
        log.info("清理 collected_params");

        total += recommendationResultRepository.hardDeleteAllSoftDeleted();
        log.info("清理 recommendation_result");

        total += conversationSessionRepository.hardDeleteAllSoftDeleted();
        log.info("清理 conversation_session");

        log.info("=== 软删除记录清理完成，共清理 {} 条 ===", total);
    }

    private int cleanupPhotos() {
        List<Photo> photos = photoRepository.findAllByIsDeletedTrue();
        Path uploadRoot = Paths.get(System.getProperty("user.dir"), "uploads").toAbsolutePath();

        for (Photo photo : photos) {
            deletePhysicalFile(uploadRoot, photo.getOriginalPath());
            deletePhysicalFile(uploadRoot, photo.getThumbnailPath());
        }

        return photoRepository.hardDeleteAllSoftDeleted();
    }

    private void deletePhysicalFile(Path uploadRoot, String relativePath) {
        if (relativePath == null || relativePath.isBlank()) return;
        try {
            String relative = relativePath.startsWith("/uploads")
                    ? relativePath.substring("/uploads".length())
                    : relativePath;
            if (relative.startsWith("/")) relative = relative.substring(1);
            Path filePath = uploadRoot.resolve(relative);
            Files.deleteIfExists(filePath);
        } catch (Exception e) {
            log.warn("清理物理文件失败: {}", relativePath, e);
        }
    }
}

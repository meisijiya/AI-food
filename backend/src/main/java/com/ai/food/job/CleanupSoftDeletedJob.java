package com.ai.food.job;

import com.ai.food.model.ChatFile;
import com.ai.food.model.ChatPhoto;
import com.ai.food.model.FeedPost;
import com.ai.food.model.FeedComment;
import com.ai.food.model.Photo;
import com.ai.food.repository.*;
import com.ai.food.service.chat.ChatService;
import com.ai.food.service.upload.FileUploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CleanupSoftDeletedJob extends QuartzJobBean {

    private final FeedCommentRepository feedCommentRepository;
    private final FeedPostRepository feedPostRepository;
    private final PhotoRepository photoRepository;
    private final ChatPhotoRepository chatPhotoRepository;
    private final ChatFileRepository chatFileRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatConversationRepository chatConversationRepository;
    private final ChatService chatService;
    private final QaRecordRepository qaRecordRepository;
    private final CollectedParamRepository collectedParamRepository;
    private final RecommendationResultRepository recommendationResultRepository;
    private final ConversationSessionRepository conversationSessionRepository;
    private final FileUploadService fileUploadService;

    private static final int CHAT_MEDIA_TTL_DAYS = 30;

    @Override
    @Transactional
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        log.info("=== 开始清理软删除记录 ===");
        int total = 0;

        // 1. 先清理评论（依赖 feed_post 的外键）
        total += cleanupCommentImages();
        log.info("清理 feed_comment 图片文件");
        total += feedCommentRepository.hardDeleteAllSoftDeleted();
        log.info("清理 feed_comment");

        // 1.5 清理动态嵌入的照片文件（thumbnailUrl / originalPhotoUrl）
        total += cleanupFeedPostPhotos();
        log.info("清理 feed_post 嵌入照片文件");

        // 2. 清理动态
        total += feedPostRepository.hardDeleteAllSoftDeleted();
        log.info("清理 feed_post");

        // 3. 清理照片（先删物理文件，再删数据库记录）
        total += cleanupPhotos();
        log.info("清理 photo（含物理文件）");

        // 4. 清理推荐相关子表
        total += qaRecordRepository.hardDeleteAllSoftDeleted();
        total += collectedParamRepository.hardDeleteAllSoftDeleted();
        total += recommendationResultRepository.hardDeleteAllSoftDeleted();
        total += conversationSessionRepository.hardDeleteAllSoftDeleted();
        log.info("清理推荐相关子表");

        // 5. 清理聊天 — 双方都已清除的对话，硬删除软删除记录
        total += cleanupBothClearedConversations();
        log.info("清理双方已清除的聊天记录");

        // 6. 清理过期的聊天软删除消息（30天前）
        total += cleanupOldChatMessages();
        log.info("清理过期的 chat_message");

        // 7. 清理聊天照片（软删除 + 30天过期）
        total += cleanupChatPhotos();
        log.info("清理 chat_photo（含物理文件）");

        // 8. 清理聊天文件（软删除 + 30天过期）
        total += cleanupChatFiles();
        log.info("清理 chat_file（含物理文件）");

        log.info("=== 软删除记录清理完成，共清理约 {} 条 ===", total);
    }

    private int cleanupPhotos() {
        List<Photo> photos = photoRepository.findAllByIsDeletedTrue();
        Path uploadRoot = getUploadRoot();
        for (Photo photo : photos) {
            deletePhysicalFile(uploadRoot, photo.getOriginalPath());
            deletePhysicalFile(uploadRoot, photo.getThumbnailPath());
        }
        return photoRepository.hardDeleteAllSoftDeleted();
    }

    private int cleanupCommentImages() {
        List<FeedComment> comments = feedCommentRepository.findAllByIsDeletedTrue();
        for (FeedComment comment : comments) {
            fileUploadService.deletePhysicalFile(comment.getImageUrl());
        }
        return comments.size();
    }

    private int cleanupFeedPostPhotos() {
        List<FeedPost> posts = feedPostRepository.findAllByIsDeletedTrue();
        Path uploadRoot = getUploadRoot();
        for (FeedPost post : posts) {
            deletePhysicalFile(uploadRoot, post.getThumbnailUrl());
            deletePhysicalFile(uploadRoot, post.getOriginalPhotoUrl());
        }
        return posts.size();
    }

    private int cleanupChatPhotos() {
        Path uploadRoot = getUploadRoot();

        // 先软删除过期的（30天前创建的）
        LocalDateTime cutoff = LocalDateTime.now().minusDays(CHAT_MEDIA_TTL_DAYS);
        chatPhotoRepository.softDeleteExpired(cutoff);
        chatFileRepository.softDeleteExpired(cutoff);

        // 再硬删除所有软删除的（含刚软删除的过期记录）
        List<ChatPhoto> photos = chatPhotoRepository.findAllByIsDeletedTrue();
        for (ChatPhoto photo : photos) {
            deletePhysicalFile(uploadRoot, photo.getOriginalPath());
            deletePhysicalFile(uploadRoot, photo.getThumbnailPath());
        }
        return chatPhotoRepository.hardDeleteAllSoftDeleted();
    }

    private int cleanupChatFiles() {
        Path uploadRoot = getUploadRoot();
        LocalDateTime cutoff = LocalDateTime.now().minusDays(CHAT_MEDIA_TTL_DAYS);

        // 过期的已在 cleanupChatPhotos 中软删除，这里硬删除
        List<ChatFile> files = chatFileRepository.findAllByIsDeletedTrue();
        for (ChatFile file : files) {
            deletePhysicalFile(uploadRoot, file.getFilePath());
        }
        return chatFileRepository.hardDeleteAllSoftDeleted();
    }

    /**
     * 清理双方都已清除的对话 — 硬删除所有软删除的聊天记录
     */
    private int cleanupBothClearedConversations() {
        int total = 0;
        var conversations = chatConversationRepository.findAllBothCleared();
        for (var conv : conversations) {
            chatService.hardDeleteClearedMessages(conv.getId());
            total++;
        }
        return total;
    }

    /**
     * 清理过期的聊天软删除消息（30天前创建的，兜底清理）
     */
    private int cleanupOldChatMessages() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(CHAT_MEDIA_TTL_DAYS);
        return chatMessageRepository.hardDeleteOldSoftDeleted(cutoff);
    }

    private Path getUploadRoot() {
        return Paths.get(System.getProperty("user.dir"), "uploads").toAbsolutePath();
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

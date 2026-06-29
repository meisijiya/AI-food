package com.ai.food.job;

import com.ai.food.config.UploadPathProperties;
import com.ai.food.model.ChatFile;
import com.ai.food.model.ChatPhoto;
import com.ai.food.model.FeedPost;
import com.ai.food.model.FeedComment;
import com.ai.food.model.Photo;
import com.ai.food.mapper.ChatConversationMapper;
import com.ai.food.mapper.ChatFileMapper;
import com.ai.food.mapper.ChatMessageMapper;
import com.ai.food.mapper.ChatPhotoMapper;
import com.ai.food.mapper.CollectedParamMapper;
import com.ai.food.mapper.ConversationSessionMapper;
import com.ai.food.mapper.FeedCommentMapper;
import com.ai.food.mapper.FeedPostMapper;
import com.ai.food.mapper.PhotoMapper;
import com.ai.food.mapper.QaRecordMapper;
import com.ai.food.mapper.RecommendationResultMapper;
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

    private final FeedCommentMapper feedCommentMapper;
    private final FeedPostMapper feedPostMapper;
    private final PhotoMapper photoMapper;
    private final ChatPhotoMapper chatPhotoMapper;
    private final ChatFileMapper chatFileMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final ChatConversationMapper chatConversationMapper;
    private final ChatService chatService;
    private final QaRecordMapper qaRecordMapper;
    private final CollectedParamMapper collectedParamMapper;
    private final RecommendationResultMapper recommendationResultMapper;
    private final ConversationSessionMapper conversationSessionMapper;
    private final FileUploadService fileUploadService;
    private final UploadPathProperties uploadPathProperties;

    private static final int CHAT_MEDIA_TTL_DAYS = 30;

    @Override
    @Transactional
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        log.info("=== 开始清理软删除记录 ===");
        int total = 0;

        // 1. 先清理评论（依赖 feed_post 的外键）
        total += cleanupCommentImages();
        log.info("清理 feed_comment 图片文件");
        total += feedCommentMapper.hardDeleteAllSoftDeleted();
        log.info("清理 feed_comment");

        // 1.5 清理动态嵌入的照片文件（thumbnailUrl / originalPhotoUrl）
        total += cleanupFeedPostPhotos();
        log.info("清理 feed_post 嵌入照片文件");

        // 2. 清理动态
        total += feedPostMapper.hardDeleteAllSoftDeleted();
        log.info("清理 feed_post");

        // 3. 清理照片（先删物理文件，再删数据库记录）
        total += cleanupPhotos();
        log.info("清理 photo（含物理文件）");

        // 4. 清理推荐相关子表
        total += qaRecordMapper.hardDeleteAllSoftDeleted();
        total += collectedParamMapper.hardDeleteAllSoftDeleted();
        total += recommendationResultMapper.hardDeleteAllSoftDeleted();
        total += conversationSessionMapper.hardDeleteAllSoftDeleted();
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
        List<Photo> photos = photoMapper.findAllByIsDeletedTrue();
        Path uploadRoot = getUploadRoot();
        for (Photo photo : photos) {
            deletePhysicalFile(uploadRoot, photo.getOriginalPath());
            deletePhysicalFile(uploadRoot, photo.getThumbnailPath());
        }
        return photoMapper.hardDeleteAllSoftDeleted();
    }

    private int cleanupCommentImages() {
        List<FeedComment> comments = feedCommentMapper.findAllByIsDeletedTrue();
        for (FeedComment comment : comments) {
            fileUploadService.deletePhysicalFile(comment.getImageUrl());
        }
        return comments.size();
    }

    private int cleanupFeedPostPhotos() {
        List<FeedPost> posts = feedPostMapper.findAllByIsDeletedTrue();
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
        chatPhotoMapper.softDeleteExpired(cutoff);
        chatFileMapper.softDeleteExpired(cutoff);

        // 再硬删除所有软删除的（含刚软删除的过期记录）
        List<ChatPhoto> photos = chatPhotoMapper.findAllByIsDeletedTrue();
        for (ChatPhoto photo : photos) {
            deletePhysicalFile(uploadRoot, photo.getOriginalPath());
            deletePhysicalFile(uploadRoot, photo.getThumbnailPath());
        }
        return chatPhotoMapper.hardDeleteAllSoftDeleted();
    }

    private int cleanupChatFiles() {
        Path uploadRoot = getUploadRoot();
        LocalDateTime cutoff = LocalDateTime.now().minusDays(CHAT_MEDIA_TTL_DAYS);

        // 过期的已在 cleanupChatPhotos 中软删除，这里硬删除
        List<ChatFile> files = chatFileMapper.findAllByIsDeletedTrue();
        for (ChatFile file : files) {
            deletePhysicalFile(uploadRoot, file.getFilePath());
        }
        return chatFileMapper.hardDeleteAllSoftDeleted();
    }

    /**
     * 清理双方都已清除的对话 — 硬删除所有软删除的聊天记录
     */
    private int cleanupBothClearedConversations() {
        int total = 0;
        var conversations = chatConversationMapper.findAllBothCleared();
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
        return chatMessageMapper.hardDeleteOldSoftDeleted(cutoff);
    }

    private Path getUploadRoot() {
        return Paths.get(uploadPathProperties.getDir()).toAbsolutePath();
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

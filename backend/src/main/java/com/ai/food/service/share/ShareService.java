package com.ai.food.service.share;

import com.ai.food.model.*;
import com.ai.food.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShareService {

    private final ShareRecordRepository shareRecordRepository;
    private final ConversationSessionRepository conversationSessionRepository;
    private final RecommendationResultRepository recommendationResultRepository;
    private final PhotoRepository photoRepository;
    private final CollectedParamRepository collectedParamRepository;
    private final QaRecordRepository qaRecordRepository;
    private final UserRepository userRepository;

    /**
     * 创建或获取分享链接（同一记录同一用户只创建一个）
     */
    @Transactional
    public Map<String, Object> createShare(String sessionId, Long userId) {
        // 检查是否已存在
        Optional<ShareRecord> existing = shareRecordRepository.findBySessionIdAndUserId(sessionId, userId);
        if (existing.isPresent()) {
            return buildShareResponse(existing.get());
        }

        // 验证 session 属于该用户
        Optional<ConversationSession> optSession = conversationSessionRepository.findBySessionId(sessionId);
        if (optSession.isEmpty() || !userId.equals(optSession.get().getUserId())) {
            throw new RuntimeException("无权分享此记录");
        }

        // 生成分享 token
        String shareToken = UUID.randomUUID().toString().replace("-", "");

        ShareRecord shareRecord = new ShareRecord();
        shareRecord.setShareToken(shareToken);
        shareRecord.setUserId(userId);
        shareRecord.setSessionId(sessionId);
        shareRecord.setViewCount(0);
        ShareRecord saved = shareRecordRepository.save(shareRecord);

        log.info("Share created: token={}, session={}, user={}", shareToken, sessionId, userId);

        return buildShareResponse(saved);
    }

    /**
     * 获取分享内容（公开接口，无需登录）
     */
    @Transactional
    public Map<String, Object> getShareDetail(String shareToken) {
        ShareRecord share = shareRecordRepository.findByShareToken(shareToken)
                .orElseThrow(() -> new RuntimeException("分享链接不存在或已失效"));

        // 增加浏览次数
        share.setViewCount(share.getViewCount() + 1);
        shareRecordRepository.save(share);

        String sessionId = share.getSessionId();
        Long userId = share.getUserId();

        // 获取推荐信息
        Optional<RecommendationResult> optRec = recommendationResultRepository.findBySessionId(sessionId);
        Optional<Photo> optPhoto = photoRepository.findFirstByRelatedSessionIdOrderByCreatedAtDesc(sessionId);
        List<CollectedParam> params = collectedParamRepository.findBySessionId(sessionId);
        List<QaRecord> qaRecords = qaRecordRepository.findBySessionIdOrderByQuestionOrderAsc(sessionId);

        // 获取分享者信息
        Optional<SysUser> optUser = userRepository.findById(userId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("shareToken", shareToken);
        result.put("viewCount", share.getViewCount());
        result.put("createdAt", share.getCreatedAt());

        // 分享者信息
        if (optUser.isPresent()) {
            result.put("sharerNickname", optUser.get().getNickname() != null ? optUser.get().getNickname() : "匿名用户");
            result.put("sharerAvatar", optUser.get().getAvatar());
        } else {
            result.put("sharerNickname", "匿名用户");
            result.put("sharerAvatar", null);
        }

        // 推荐信息
        if (optRec.isPresent()) {
            RecommendationResult rec = optRec.get();
            result.put("foodName", rec.getFoodName());
            result.put("reason", rec.getReason());
            result.put("comment", rec.getComment());
            result.put("mode", rec.getMode());
        }

        // 照片
        if (optPhoto.isPresent()) {
            Photo photo = optPhoto.get();
            result.put("photoUrl", photo.getOriginalPath());
            result.put("thumbnailUrl", photo.getThumbnailPath());
        }

        // 已收集参数
        List<Map<String, String>> paramList = new ArrayList<>();
        for (CollectedParam p : params) {
            Map<String, String> pm = new LinkedHashMap<>();
            pm.put("name", p.getParamName());
            pm.put("value", p.getParamValue());
            paramList.add(pm);
        }
        result.put("collectedParams", paramList);

        // 对话记录
        List<Map<String, Object>> qaList = new ArrayList<>();
        for (QaRecord qa : qaRecords) {
            Map<String, Object> qm = new LinkedHashMap<>();
            qm.put("question", qa.getAiQuestion());
            qm.put("answer", qa.getUserAnswer());
            qm.put("paramName", qa.getParamName());
            qaList.add(qm);
        }
        result.put("qaRecords", qaList);

        return result;
    }

    /**
     * 检查记录是否已分享
     */
    public Map<String, Object> checkShare(String sessionId, Long userId) {
        Optional<ShareRecord> existing = shareRecordRepository.findBySessionIdAndUserId(sessionId, userId);
        if (existing.isPresent()) {
            return buildShareResponse(existing.get());
        }
        return Map.of("shared", false);
    }

    private Map<String, Object> buildShareResponse(ShareRecord record) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("shared", true);
        result.put("shareToken", record.getShareToken());
        result.put("shareUrl", "/share/" + record.getShareToken());
        result.put("viewCount", record.getViewCount());
        result.put("createdAt", record.getCreatedAt());
        return result;
    }
}

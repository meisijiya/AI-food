package com.ai.food.service.share;

import com.ai.food.common.mapper.CollectedParamMapper;
import com.ai.food.common.mapper.ConversationSessionMapper;
import com.ai.food.common.mapper.PhotoMapper;
import com.ai.food.common.mapper.QaRecordMapper;
import com.ai.food.common.mapper.RecommendationResultMapper;
import com.ai.food.common.mapper.ShareRecordMapper;
import com.ai.food.common.mapper.UserMapper;
import com.ai.food.common.model.CollectedParam;
import com.ai.food.common.model.ConversationSession;
import com.ai.food.common.model.Photo;
import com.ai.food.common.model.QaRecord;
import com.ai.food.common.model.RecommendationResult;
import com.ai.food.common.model.ShareRecord;
import com.ai.food.common.model.SysUser;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * 分享链接业务（创建 / 公开查看 / 检查是否已分享）。
 * <p>主实体为 {@link ShareRecord}，{@code baseMapper} 由 ServiceImpl 父类注入；
 * 因聚合展示需要联查 6 张相邻业务表，其余 Mapper 通过构造函数注入。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShareService extends ServiceImpl<ShareRecordMapper, ShareRecord> {

    private final ConversationSessionMapper conversationSessionMapper;
    private final RecommendationResultMapper recommendationResultMapper;
    private final PhotoMapper photoMapper;
    private final CollectedParamMapper collectedParamMapper;
    private final QaRecordMapper qaRecordMapper;
    private final UserMapper userMapper;

    /**
     * 创建或获取分享链接（同一记录同一用户只创建一个）
     */
    @Transactional
    public Map<String, Object> createShare(String sessionId, Long userId) {
        // 检查是否已存在
        Optional<ShareRecord> existing = Optional.ofNullable(
                baseMapper.findBySessionIdAndUserId(sessionId, userId));
        if (existing.isPresent()) {
            return buildShareResponse(existing.get());
        }

        // 验证 session 属于该用户
        Optional<ConversationSession> optSession = Optional.ofNullable(
                conversationSessionMapper.findBySessionId(sessionId));
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
        // ponytail: 显式 insert 语义清晰，避免 ServiceImpl.save 的 select-then-decide
        ShareRecord saved = baseMapper.insert(shareRecord) > 0 ? shareRecord : null;
        if (saved == null) {
            throw new RuntimeException("分享记录创建失败");
        }

        log.debug("Share created: token={}, user={}", shareToken, userId);

        return buildShareResponse(saved);
    }

    /**
     * 获取分享内容（公开接口，无需登录）
     */
    @Transactional
    public Map<String, Object> getShareDetail(String shareToken) {
        ShareRecord share = Optional.ofNullable(baseMapper.findByShareToken(shareToken))
                .orElseThrow(() -> new RuntimeException("分享链接不存在或已失效"));

        // 增加浏览次数
        share.setViewCount(share.getViewCount() + 1);
        // ponytail: 显式 updateById，保留乐观锁 + version 自增语义
        baseMapper.updateById(share);

        String sessionId = share.getSessionId();
        Long userId = share.getUserId();

        // 获取推荐信息
        Optional<RecommendationResult> optRec = Optional.ofNullable(
                recommendationResultMapper.findBySessionId(sessionId));
        Optional<Photo> optPhoto = Optional.ofNullable(
                photoMapper.findFirstByRelatedSessionIdOrderByCreatedAtDesc(sessionId));
        List<CollectedParam> params = collectedParamMapper.findBySessionId(sessionId);
        List<QaRecord> qaRecords = qaRecordMapper.findBySessionIdOrderByQuestionOrderAsc(sessionId);

        // 获取分享者信息
        Optional<SysUser> optUser = Optional.ofNullable(userMapper.selectById(userId));

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
        Optional<ShareRecord> existing = Optional.ofNullable(
                baseMapper.findBySessionIdAndUserId(sessionId, userId));
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

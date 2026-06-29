package com.aifood.admin.service;

import com.ai.food.common.mapper.ConversationSessionMapper;
import com.ai.food.common.mapper.QaRecordMapper;
import com.ai.food.common.model.ConversationSession;
import com.ai.food.common.model.QaRecord;
import com.aifood.admin.common.AdminException;
import com.aifood.admin.dto.ConversationQueryReq;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * 管理后台 AI 对话会话服务。
 *
 * <p>管理 AI 用户（{@code conversation_session}）与其问答记录（{@code qa_record}）。
 * 两个实体都带 {@code @TableLogic} 软删字段,BaseMapper 内置方法（{@code selectPage} /
 * {@code selectById}）会自动追加 {@code is_deleted = 0} 条件;写操作用
 * {@link LambdaUpdateWrapper} 显式 set,以绕开 {@code @TableLogic} 对 {@code updateById}
 * 的字段排除。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationSessionMapper conversationSessionMapper;
    private final QaRecordMapper qaRecordMapper;

    /**
     * 分页查询会话列表。
     *
     * <p>过滤项:userId / mode / status / createdAt 闭区间。结果按 createdAt 倒序,
     * 与 {@code userService.page} 风格一致。</p>
     *
     * @param req 查询条件
     * @return MyBatis-Plus 分页结果,默认按 createdAt DESC
     */
    public Page<ConversationSession> page(ConversationQueryReq req) {
        Page<ConversationSession> page = new Page<>(req.getPage(), req.getSize());
        LambdaQueryWrapper<ConversationSession> w = new LambdaQueryWrapper<>();
        if (req.getUserId() != null) w.eq(ConversationSession::getUserId, req.getUserId());
        if (StringUtils.hasText(req.getMode())) w.eq(ConversationSession::getMode, req.getMode());
        if (StringUtils.hasText(req.getStatus())) w.eq(ConversationSession::getStatus, req.getStatus());
        if (StringUtils.hasText(req.getStartDate())) {
            LocalDateTime s = LocalDateTime.of(
                    LocalDate.parse(req.getStartDate(), DateTimeFormatter.ISO_DATE), LocalTime.MIN);
            w.ge(ConversationSession::getCreatedAt, s);
        }
        if (StringUtils.hasText(req.getEndDate())) {
            LocalDateTime e = LocalDateTime.of(
                    LocalDate.parse(req.getEndDate(), DateTimeFormatter.ISO_DATE), LocalTime.MAX);
            w.le(ConversationSession::getCreatedAt, e);
        }
        w.orderByDesc(ConversationSession::getCreatedAt);
        return conversationSessionMapper.selectPage(page, w);
    }

    /**
     * 查询单个会话详情。已被软删的会话返回 null 触发 404,便于审计追溯。
     *
     * @param id 会话主键 (Long)
     * @return 会话实体
     * @throws AdminException 404 当会话不存在或已删除
     */
    public ConversationSession getDetail(Long id) {
        ConversationSession s = conversationSessionMapper.selectById(id);
        if (s == null) throw new AdminException(404, "对话不存在");
        return s;
    }

    /**
     * 分页查询某会话下的问答记录。
     *
     * <p>{@code conversation_session} 的主键是 Long {@code id},但 {@code qa_record} 的外键是
     * String {@code sessionId}。这里先按 Long id 查到 session,再以其 sessionId 查 qa_record,
     * 保持 URL 用 Long id、底层跨表用 String 业务键的约定。</p>
     *
     * @param conversationId 会话主键 (Long)
     * @param page          页码
     * @param size          每页条数
     * @return MyBatis-Plus 分页结果,按 createdAt ASC
     */
    public Page<QaRecord> getMessages(Long conversationId, int page, int size) {
        ConversationSession s = getDetail(conversationId); // 顺便 404
        Page<QaRecord> p = new Page<>(page, size);
        LambdaQueryWrapper<QaRecord> w = new LambdaQueryWrapper<>();
        w.eq(QaRecord::getSessionId, s.getSessionId());
        w.orderByAsc(QaRecord::getCreatedAt);
        return qaRecordMapper.selectPage(p, w);
    }

    /**
     * 软删除会话:set {@code is_deleted=1} + {@code deleted_at=now} + version 自动 +1。
     *
     * <p>走 {@link LambdaUpdateWrapper} 而非 {@code updateById(entity)}:后者会被
     * {@code @TableLogic} 字段排除,无法将 is_deleted 写入 SQL。与 {@code UserService.disable}
     * 同款写法。</p>
     */
    public void delete(Long id) {
        if (conversationSessionMapper.selectById(id) == null) {
            throw new AdminException(404, "对话不存在");
        }
        LambdaUpdateWrapper<ConversationSession> w = new LambdaUpdateWrapper<ConversationSession>()
                .eq(ConversationSession::getId, id)
                .set(ConversationSession::getIsDeleted, 1)
                .set(ConversationSession::getDeletedAt, LocalDateTime.now());
        conversationSessionMapper.update(null, w);
        log.info("soft-deleted conversation session id={}", id);
    }
}

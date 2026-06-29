package com.ai.food.service.record;

import com.ai.food.common.mapper.CollectedParamMapper;
import com.ai.food.common.mapper.ConversationSessionMapper;
import com.ai.food.common.mapper.FeedCommentMapper;
import com.ai.food.common.mapper.FeedPostMapper;
import com.ai.food.common.mapper.PhotoMapper;
import com.ai.food.common.mapper.QaRecordMapper;
import com.ai.food.common.mapper.RecommendationResultMapper;
import com.ai.food.common.model.ConversationSession;
import com.ai.food.common.model.RecommendationResult;
import com.ai.food.service.bloom.BloomFilterService;
import com.ai.food.service.feed.FeedService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RecordService 记录删除")
class RecordServiceTest {

    @Mock
    private ConversationSessionMapper conversationSessionMapper;

    @Mock
    private RecommendationResultMapper recommendationResultMapper;

    @Mock
    private CollectedParamMapper collectedParamMapper;

    @Mock
    private QaRecordMapper qaRecordMapper;

    @Mock
    private PhotoMapper photoMapper;

    @Mock
    private FeedPostMapper feedPostMapper;

    @Mock
    private FeedCommentMapper feedCommentMapper;

    @Mock
    private FeedService feedService;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private BloomFilterService bloomFilterService;

    @Test
    @DisplayName("删除记录时同步移除匹配画像")
    void deleteRecord_removesBloomRecommendation() {
        RecordService service = new RecordService(
                recommendationResultMapper,
                collectedParamMapper,
                qaRecordMapper,
                photoMapper,
                feedPostMapper,
                feedCommentMapper,
                feedService, redisTemplate, bloomFilterService
        );
        ReflectionTestUtils.setField(service, "baseMapper", conversationSessionMapper);
        ConversationSession session = new ConversationSession();
        session.setSessionId("s-1");
        session.setUserId(9L);
        RecommendationResult result = new RecommendationResult();
        result.setId(88L);
        when(conversationSessionMapper.findBySessionId("s-1")).thenReturn(session);
        when(recommendationResultMapper.findBySessionId("s-1")).thenReturn(result);

        service.deleteRecord("s-1");

        verify(bloomFilterService).removeRecommendation(9L, "88", null);
    }

    @Test
    @DisplayName("缺少推荐结果时不调用匹配画像删除")
    void deleteRecord_skipsBloomRemovalWhenRecommendationMissing() {
        RecordService service = new RecordService(
                recommendationResultMapper,
                collectedParamMapper,
                qaRecordMapper,
                photoMapper,
                feedPostMapper,
                feedCommentMapper,
                feedService,
                redisTemplate,
                bloomFilterService
        );
        ReflectionTestUtils.setField(service, "baseMapper", conversationSessionMapper);
        ConversationSession session = new ConversationSession();
        session.setSessionId("s-2");
        session.setUserId(9L);
        when(conversationSessionMapper.findBySessionId("s-2")).thenReturn(session);
        when(recommendationResultMapper.findBySessionId("s-2")).thenReturn(null);

        service.deleteRecord("s-2");

        verify(bloomFilterService, never()).removeRecommendation(9L, "s-2", null);
    }

    @Test
    @DisplayName("批量删除记录时逐条移除匹配画像")
    void batchDeleteRecords_removesBloomRecommendations() {
        RecordService service = new RecordService(
                recommendationResultMapper,
                collectedParamMapper,
                qaRecordMapper,
                photoMapper,
                feedPostMapper,
                feedCommentMapper,
                feedService,
                redisTemplate,
                bloomFilterService
        );
        ReflectionTestUtils.setField(service, "baseMapper", conversationSessionMapper);

        ConversationSession session1 = new ConversationSession();
        session1.setSessionId("s-1");
        session1.setUserId(9L);
        ConversationSession session2 = new ConversationSession();
        session2.setSessionId("s-2");
        session2.setUserId(10L);

        RecommendationResult result1 = new RecommendationResult();
        result1.setId(88L);
        RecommendationResult result2 = new RecommendationResult();
        result2.setId(99L);

        when(conversationSessionMapper.findBySessionId("s-1")).thenReturn(session1);
        when(conversationSessionMapper.findBySessionId("s-2")).thenReturn(session2);
        when(recommendationResultMapper.findBySessionId("s-1")).thenReturn(result1);
        when(recommendationResultMapper.findBySessionId("s-2")).thenReturn(result2);
        when(feedPostMapper.findBySessionIdIn(List.of("s-1", "s-2"))).thenReturn(List.of());
        when(photoMapper.findByRelatedSessionIdInOrderByCreatedAtDesc(List.of("s-1", "s-2"))).thenReturn(List.of());

        service.batchDeleteRecords(List.of("s-1", "s-2"));

        verify(bloomFilterService).removeRecommendation(9L, "88", null);
        verify(bloomFilterService).removeRecommendation(10L, "99", null);
    }
}

package com.ai.food.service.record;

import com.ai.food.model.ConversationSession;
import com.ai.food.model.RecommendationResult;
import com.ai.food.repository.CollectedParamRepository;
import com.ai.food.repository.ConversationSessionRepository;
import com.ai.food.repository.FeedCommentRepository;
import com.ai.food.repository.FeedPostRepository;
import com.ai.food.repository.PhotoRepository;
import com.ai.food.repository.QaRecordRepository;
import com.ai.food.repository.RecommendationResultRepository;
import com.ai.food.service.bloom.BloomFilterService;
import com.ai.food.service.feed.FeedService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RecordService 记录删除")
class RecordServiceTest {

    @Mock
    private ConversationSessionRepository conversationSessionRepository;

    @Mock
    private RecommendationResultRepository recommendationResultRepository;

    @Mock
    private CollectedParamRepository collectedParamRepository;

    @Mock
    private QaRecordRepository qaRecordRepository;

    @Mock
    private PhotoRepository photoRepository;

    @Mock
    private FeedPostRepository feedPostRepository;

    @Mock
    private FeedCommentRepository feedCommentRepository;

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
                conversationSessionRepository,
                recommendationResultRepository,
                collectedParamRepository,
                qaRecordRepository,
                photoRepository,
                feedPostRepository,
                feedCommentRepository,
                feedService,
                redisTemplate,
                bloomFilterService
        );
        ConversationSession session = new ConversationSession();
        session.setSessionId("s-1");
        session.setUserId(9L);
        RecommendationResult result = new RecommendationResult();
        result.setId(88L);
        when(conversationSessionRepository.findBySessionId("s-1")).thenReturn(Optional.of(session));
        when(recommendationResultRepository.findBySessionId("s-1")).thenReturn(Optional.of(result));

        service.deleteRecord("s-1");

        verify(bloomFilterService).removeRecommendation(9L, "88", null);
    }

    @Test
    @DisplayName("缺少推荐结果时不调用匹配画像删除")
    void deleteRecord_skipsBloomRemovalWhenRecommendationMissing() {
        RecordService service = new RecordService(
                conversationSessionRepository,
                recommendationResultRepository,
                collectedParamRepository,
                qaRecordRepository,
                photoRepository,
                feedPostRepository,
                feedCommentRepository,
                feedService,
                redisTemplate,
                bloomFilterService
        );
        ConversationSession session = new ConversationSession();
        session.setSessionId("s-2");
        session.setUserId(9L);
        when(conversationSessionRepository.findBySessionId("s-2")).thenReturn(Optional.of(session));
        when(recommendationResultRepository.findBySessionId("s-2")).thenReturn(Optional.empty());

        service.deleteRecord("s-2");

        verify(bloomFilterService, never()).removeRecommendation(9L, "s-2", null);
    }

    @Test
    @DisplayName("批量删除记录时逐条移除匹配画像")
    void batchDeleteRecords_removesBloomRecommendations() {
        RecordService service = new RecordService(
                conversationSessionRepository,
                recommendationResultRepository,
                collectedParamRepository,
                qaRecordRepository,
                photoRepository,
                feedPostRepository,
                feedCommentRepository,
                feedService,
                redisTemplate,
                bloomFilterService
        );

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

        when(conversationSessionRepository.findBySessionId("s-1")).thenReturn(Optional.of(session1));
        when(conversationSessionRepository.findBySessionId("s-2")).thenReturn(Optional.of(session2));
        when(recommendationResultRepository.findBySessionId("s-1")).thenReturn(Optional.of(result1));
        when(recommendationResultRepository.findBySessionId("s-2")).thenReturn(Optional.of(result2));
        when(feedPostRepository.findBySessionIdIn(List.of("s-1", "s-2"))).thenReturn(List.of());
        when(photoRepository.findByRelatedSessionIdInOrderByCreatedAtDesc(List.of("s-1", "s-2"))).thenReturn(List.of());

        service.batchDeleteRecords(List.of("s-1", "s-2"));

        verify(bloomFilterService).removeRecommendation(9L, "88", null);
        verify(bloomFilterService).removeRecommendation(10L, "99", null);
    }
}

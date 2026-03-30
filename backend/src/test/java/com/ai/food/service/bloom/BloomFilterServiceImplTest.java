package com.ai.food.service.bloom;

import com.ai.food.repository.CollectedParamRepository;
import com.ai.food.repository.ConversationSessionRepository;
import com.ai.food.repository.RecommendationResultRepository;
import com.ai.food.repository.UserRepository;
import com.ai.food.service.bloom.impl.BloomFilterServiceImpl;
import com.ai.food.service.follow.FollowService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("BloomFilterServiceImpl 位图维护")
class BloomFilterServiceImplTest {

    @Mock
    private BloomFilterRedisDao redisDao;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ConversationSessionRepository conversationSessionRepository;

    @Mock
    private CollectedParamRepository collectedParamRepository;

    @Mock
    private RecommendationResultRepository recommendationResultRepository;

    @Mock
    private FollowService followService;

    @Test
    @DisplayName("删除推荐记录时应根据剩余记录重建位图")
    void removeRecommendation_rebuildsBitArrayFromRemainingValues() {
        BloomFilterServiceImpl service = new BloomFilterServiceImpl(
                redisDao,
                userRepository,
                conversationSessionRepository,
                collectedParamRepository,
                recommendationResultRepository,
                followService
        );
        when(redisDao.getQueue(1L)).thenReturn(List.of("r2"));
        when(redisDao.getRecordValue(1L, "r2")).thenReturn("taste=辣|time=晚上");

        service.removeRecommendation(1L, "r1", null);

        var order = inOrder(redisDao);
        order.verify(redisDao).removeRecordValue(1L, "r1");
        order.verify(redisDao).removeFromQueue(1L, "r1");
        order.verify(redisDao).clearAllBits(1L);
        verify(redisDao).getRecordValue(1L, "r2");
        verify(redisDao, atLeastOnce()).setBit(eq(1L), any(Integer.class));
        verify(redisDao).markPendingSync(1L);
    }
}

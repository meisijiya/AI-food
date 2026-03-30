package com.ai.food.service.bloom;

import com.ai.food.dto.MatchUserDetailDTO;
import com.ai.food.dto.UserSimilarityDTO;

import java.util.List;
import java.util.Set;

public interface BloomFilterService {

    void addRecommendation(Long userId, String recordId, String paramValue);

    void removeRecommendation(Long userId, String recordId, String paramValue);

    byte[] getBitArray(Long userId);

    double calculateSimilarity(Long userIdA, Long userIdB);

    List<UserSimilarityDTO> getTopKSimilarUsers(Long userId, int k);

    MatchUserDetailDTO getRandomSimilarUser(Long userId, Set<Long> excludeIds);

    int getRecordCount(Long userId);

    List<String> getRecentRecords(Long userId);
}
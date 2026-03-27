package com.ai.food.service.bloom;

public interface BloomPersistenceService {
    
    void syncRedisToMySQL();
    
    void restoreRedisFromMySQL(Long userId);
    
    void restoreAllFromMySQL();
    
    void syncUser(Long userId);
}
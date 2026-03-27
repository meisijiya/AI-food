package com.ai.food.repository;

import com.ai.food.model.UserBloomFilter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserBloomFilterRepository extends JpaRepository<UserBloomFilter, Long> {
    
    Optional<UserBloomFilter> findByUserId(Long userId);
    
    boolean existsByUserId(Long userId);
}
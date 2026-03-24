package com.ai.food.repository;

import com.ai.food.model.FeedPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface FeedPostRepository extends JpaRepository<FeedPost, Long> {

    Optional<FeedPost> findBySessionIdAndUserId(String sessionId, Long userId);

    Page<FeedPost> findByOrderByPublishedAtDesc(Pageable pageable);

    Page<FeedPost> findByFoodNameContainingOrderByPublishedAtDesc(String foodName, Pageable pageable);

    Page<FeedPost> findByUserIdOrderByPublishedAtDesc(Long userId, Pageable pageable);

    @Query("SELECT f FROM FeedPost f WHERE " +
           "(:foodName IS NULL OR f.foodName LIKE %:foodName%) AND " +
           "(:paramValue IS NULL OR f.collectedParams LIKE %:paramValue%) " +
           "ORDER BY f.publishedAt DESC")
    Page<FeedPost> findByFilters(@Param("foodName") String foodName,
                                 @Param("paramValue") String paramValue,
                                 Pageable pageable);
}

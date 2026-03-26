package com.ai.food.repository;

import com.ai.food.model.FeedPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

    @Query("SELECT f FROM FeedPost f WHERE f.visibility = 'public' AND " +
           "(:foodName IS NULL OR f.foodName LIKE %:foodName%) AND " +
           "(:paramValue IS NULL OR f.collectedParams LIKE %:paramValue%) " +
           "ORDER BY f.publishedAt DESC")
    Page<FeedPost> findPublicByFilters(@Param("foodName") String foodName,
                                       @Param("paramValue") String paramValue,
                                       Pageable pageable);

    @Query("SELECT f FROM FeedPost f WHERE " +
           "((f.visibility = 'public') OR (f.visibility = 'friends' AND f.userId IN :followingIds)) AND " +
           "(:foodName IS NULL OR f.foodName LIKE %:foodName%) AND " +
           "(:paramValue IS NULL OR f.collectedParams LIKE %:paramValue%) " +
           "ORDER BY f.publishedAt DESC")
    Page<FeedPost> findPublicAndFansOnlyByFilters(@Param("followingIds") List<Long> followingIds,
                                                   @Param("foodName") String foodName,
                                                   @Param("paramValue") String paramValue,
                                                   Pageable pageable);

    @Query("SELECT f FROM FeedPost f WHERE f.userId IN :userIds " +
           "ORDER BY f.publishedAt DESC")
    Page<FeedPost> findByUserIdsOrderByPublishedAtDesc(@Param("userIds") List<Long> userIds, Pageable pageable);

    List<FeedPost> findByIdIn(List<Long> ids);

    @Modifying
    @Query("UPDATE FeedPost f SET f.isDeleted = true WHERE f.sessionId = :sessionId")
    void softDeleteBySessionId(@Param("sessionId") String sessionId);

    @Modifying
    @Query("UPDATE FeedPost f SET f.isDeleted = true WHERE f.id = :postId")
    void softDeleteByPostId(@Param("postId") Long postId);

    @Query("SELECT f FROM FeedPost f WHERE f.sessionId = :sessionId AND f.isDeleted = false")
    Optional<FeedPost> findBySessionId(@Param("sessionId") String sessionId);

    @Modifying
    @Query("DELETE FROM FeedPost f WHERE f.isDeleted = true")
    int hardDeleteAllSoftDeleted();

    @Query("SELECT f FROM FeedPost f WHERE f.isDeleted = true")
    List<FeedPost> findAllByIsDeletedTrue();
}

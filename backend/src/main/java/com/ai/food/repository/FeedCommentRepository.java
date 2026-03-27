package com.ai.food.repository;

import com.ai.food.model.FeedComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface FeedCommentRepository extends JpaRepository<FeedComment, Long> {

    Page<FeedComment> findByPostIdOrderByCreatedAtDesc(Long postId, Pageable pageable);

    java.util.Optional<FeedComment> findByIdAndUserId(Long id, Long userId);

    List<FeedComment> findAllByIsDeletedTrue();

    long countByPostId(Long postId);

    @Modifying
    @Query("UPDATE FeedComment c SET c.isDeleted = true WHERE c.id = :commentId AND c.userId = :userId")
    int softDeleteByIdAndUserId(@Param("commentId") Long commentId, @Param("userId") Long userId);

    @Modifying
    @Query("UPDATE FeedComment c SET c.isDeleted = true WHERE c.postId = :postId")
    void softDeleteByPostId(@Param("postId") Long postId);
    
    @Modifying
    @Query("UPDATE FeedComment c SET c.isDeleted = true WHERE c.postId IN :postIds")
    void softDeleteByPostIdIn(@Param("postIds") List<Long> postIds);

    @Modifying
    @Query("DELETE FROM FeedComment c WHERE c.isDeleted = true")
    int hardDeleteAllSoftDeleted();
}

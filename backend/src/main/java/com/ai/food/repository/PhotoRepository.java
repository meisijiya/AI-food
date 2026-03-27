package com.ai.food.repository;

import com.ai.food.model.Photo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface PhotoRepository extends JpaRepository<Photo, Long> {

    List<Photo> findByUserIdOrderByCreatedAtDesc(Long userId);

    // 取该 session 最新一条照片（一个 session 可能上传多次）
    Optional<Photo> findFirstByRelatedSessionIdOrderByCreatedAtDesc(String sessionId);

    List<Photo> findByRelatedSessionIdInOrderByCreatedAtDesc(List<String> sessionIds);

    List<Photo> findByUserIdAndRelatedSessionId(Long userId, String sessionId);

    Optional<Photo> findByUserIdAndOriginalPath(Long userId, String originalPath);

    Optional<Photo> findByThumbnailPath(String thumbnailPath);

    List<Photo> findAllByIsDeletedTrue();

    @Modifying
    @Query("DELETE FROM Photo p WHERE p.isDeleted = true")
    int hardDeleteAllSoftDeleted();
}

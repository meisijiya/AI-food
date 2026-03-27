package com.ai.food.repository;

import com.ai.food.model.UserFollow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserFollowRepository extends JpaRepository<UserFollow, Long> {
    
    Optional<UserFollow> findByFollowerIdAndFollowingId(Long followerId, Long followingId);
    
    List<UserFollow> findByFollowerId(Long followerId);
    
    List<UserFollow> findByFollowingId(Long followingId);
    
    long countByFollowerId(Long followerId);
    
    long countByFollowingId(Long followingId);
    
    void deleteByFollowerIdAndFollowingId(Long followerId, Long followingId);
    
    boolean existsByFollowerIdAndFollowingId(Long followerId, Long followingId);
    
    @Query("SELECT COUNT(uf) = 2 FROM UserFollow uf WHERE (uf.followerId = :id1 AND uf.followingId = :id2) OR (uf.followerId = :id2 AND uf.followingId = :id1)")
    boolean isMutualFollow(@Param("id1") Long userId1, @Param("id2") Long userId2);
    
    @Query("SELECT uf.followingId FROM UserFollow uf WHERE uf.followerId = :userId")
    List<Long> findFollowingIdsByUserId(@Param("userId") Long userId);
    
    @Query("SELECT uf.followerId FROM UserFollow uf WHERE uf.followingId = :userId")
    List<Long> findFollowerIdsByUserId(@Param("userId") Long userId);
}

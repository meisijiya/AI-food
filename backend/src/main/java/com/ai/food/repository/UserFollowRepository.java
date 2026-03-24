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
    
    @Query("SELECT uf.followingId FROM UserFollow uf WHERE uf.followerId = :userId")
    List<Long> findFollowingIdsByUserId(@Param("userId") Long userId);
    
    @Query("SELECT uf.followerId FROM UserFollow uf WHERE uf.followingId = :userId")
    List<Long> findFollowerIdsByUserId(@Param("userId") Long userId);
}

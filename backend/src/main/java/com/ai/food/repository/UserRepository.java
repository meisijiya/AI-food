package com.ai.food.repository;

import com.ai.food.model.SysUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<SysUser, Long> {
    Optional<SysUser> findByUsername(String username);
    Optional<SysUser> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    @Query("SELECT u FROM SysUser u WHERE u.nickname LIKE %:keyword% AND u.id != :excludeId")
    Page<SysUser> searchUsers(@Param("keyword") String keyword, @Param("excludeId") Long excludeId, Pageable pageable);

    @Query("SELECT u FROM SysUser u WHERE u.id IN :ids")
    List<SysUser> findByIdIn(@Param("ids") List<Long> ids);
}

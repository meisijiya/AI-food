package com.ai.food.repository;

import com.ai.food.model.SysUser;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<SysUser, Long> {
    Optional<SysUser> findByUsername(String username);
    Optional<SysUser> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}

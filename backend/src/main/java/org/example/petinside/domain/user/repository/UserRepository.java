package org.example.petinside.domain.user.repository;

import org.example.petinside.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
    boolean existsByNickname(String nickname);
    boolean existsByNicknameAndIdNot(String nickname, Long id);
    boolean existsByNicknameAndIdNot(String nickname, Long id);
    long countByCreatedAtAfter(LocalDateTime startOfDay);

    @Modifying
    @Query("UPDATE User u SET u.role = :role WHERE u.id = :userId")
    int updateRole(@Param("userId") Long userId, @Param("role") String role);
}
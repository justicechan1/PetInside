package org.example.petinside.domain.auth.repository;

import org.example.petinside.domain.auth.entity.RefreshToken;
import org.example.petinside.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {

    @Query("SELECT COUNT(DISTINCT r.user.id) FROM RefreshToken r WHERE r.createdAt >= :startOfDay")
    long countDistinctUsersLoggedInSince(@Param("startOfDay") LocalDateTime startOfDay);

    // refreshToken 제거
    void deleteByUser(User user);

    // 재발급/로그아웃 시 DB에 살아있는 토큰인지 대조 (해시값 기준)
    Optional<RefreshToken> findByTokenValue(String tokenValue);
}

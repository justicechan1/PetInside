package org.example.petinside.domain.auth.repository;

import org.example.petinside.domain.auth.entity.RefreshToken;
import org.example.petinside.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {

    @Query("SELECT COUNT(DISTINCT r.user.id) FROM RefreshToken r WHERE r.createdAt >= :startOfDay")
    long countDistinctUsersLoggedInSince(@Param("startOfDay") LocalDateTime startOfDay);

    void deleteByUser(User user);
}

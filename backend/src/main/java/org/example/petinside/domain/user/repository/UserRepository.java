package org.example.petinside.domain.user.repository;

import jakarta.persistence.LockModeType;
import org.example.petinside.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
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
    long countByCreatedAtAfter(LocalDateTime startOfDay);

    @Modifying
    @Query("UPDATE User u SET u.role = :role WHERE u.id = :userId")
    int updateRole(@Param("userId") Long userId, @Param("role") String role);

    // 같은 유저의 "구독 있는지 확인 → 없으면 결제" 요청이 동시에 여러 번 들어와도 한 번에 하나씩만
    // 처리되도록, 트랜잭션이 끝날 때까지 이 유저 row에 락을 건다(정기구독 시작 시 이중결제 방지용).
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.id = :id")
    Optional<User> findByIdForUpdate(@Param("id") Long id);
}
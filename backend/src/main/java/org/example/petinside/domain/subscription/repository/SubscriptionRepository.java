package org.example.petinside.domain.subscription.repository;

import org.example.petinside.domain.subscription.entity.Subscription;
import org.example.petinside.domain.subscription.entity.SubscriptionStatus;
import org.example.petinside.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    boolean existsByUserAndStatus(User user, SubscriptionStatus status);

    // F-25: 구독 상태 조회용. 정기/단건 구분 없이 가장 최근 구독 1건.
    Optional<Subscription> findFirstByUserIdOrderByIdDesc(Long userId);

    // 해지 예약(canceledAt 존재)됐고 만료 시점(nextBillingAt)이 지난 구독 - 정기구독 해지분/단건 이용권 만료 공통 대상
    List<Subscription> findByStatusAndCanceledAtIsNotNullAndNextBillingAtBefore(SubscriptionStatus status, LocalDateTime now);

    // 구독 상태/가입일 필터링 용
    Page<Subscription> findByStatus(SubscriptionStatus status, Pageable pageable);
    Page<Subscription> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);
}

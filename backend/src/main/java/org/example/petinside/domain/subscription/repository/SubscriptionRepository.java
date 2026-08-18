package org.example.petinside.domain.subscription.repository;

import org.example.petinside.domain.subscription.entity.Subscription;
import org.example.petinside.domain.subscription.entity.SubscriptionStatus;
import org.example.petinside.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    boolean existsByUserAndStatus(User user, SubscriptionStatus status);

    // 해지 예약(canceledAt 존재)됐고 만료 시점(nextBillingAt)이 지난 구독 - 정기구독 해지분/단건 이용권 만료 공통 대상
    List<Subscription> findByStatusAndCanceledAtIsNotNullAndNextBillingAtBefore(SubscriptionStatus status, LocalDateTime now);
}

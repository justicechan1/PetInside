package org.example.petinside.domain.subscription.scheduler;

import lombok.RequiredArgsConstructor;
import org.example.petinside.domain.subscription.entity.Subscription;
import org.example.petinside.domain.subscription.entity.SubscriptionStatus;
import org.example.petinside.domain.subscription.repository.SubscriptionRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

// 매일 자정: 해지 예약(canceledAt)됐고 만료 시점(nextBillingAt)이 지난 구독을 EXPIRED로 전환.
// 1개월 단건 이용권은 생성 시점에 이미 canceledAt이 채워져 있어 이 배치로 자동 만료된다.
@Component
@RequiredArgsConstructor
public class SubscriptionExpirationScheduler {

    private final SubscriptionRepository subscriptionRepository;

    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void expireLapsedSubscriptions() {
        for (Subscription subscription : subscriptionRepository.findByStatusAndCanceledAtIsNotNullAndNextBillingAtBefore(SubscriptionStatus.ACTIVE, LocalDateTime.now())) {
            subscription.expireImmediately();
        }
    }
}

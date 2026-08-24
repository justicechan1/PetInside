package org.example.petinside.domain.subscription.scheduler;

import lombok.RequiredArgsConstructor;
import org.example.petinside.domain.subscription.entity.Subscription;
import org.example.petinside.domain.subscription.entity.SubscriptionStatus;
import org.example.petinside.domain.subscription.repository.SubscriptionRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

// 만료 대상 구독 상태를 자동 정리하는 스케줄러
@Component
@RequiredArgsConstructor
public class SubscriptionExpirationScheduler {

    // 결제 실패 시 재시도를 기다려주는 유예기간
    private static final int PAYMENT_FAILURE_GRACE_PERIOD_DAYS = 3;

    private final SubscriptionRepository subscriptionRepository;

    // 매일 자정 만료 대상 구독 상태 일괄 변경
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void expireLapsedSubscriptions() {
        // 해지 예약을 신청하고 이용 만료일이 끝난 구독 만료 처리
        for (Subscription subscription : subscriptionRepository.findByStatusAndCanceledAtIsNotNullAndNextBillingAtBefore(SubscriptionStatus.ACTIVE, LocalDateTime.now())) {
            subscription.expireImmediately();
        }

        // 결제 실패 상태로 전환된지 3일이 지난 구독 만료 처리
        LocalDateTime graceThreshold = LocalDateTime.now().minusDays(PAYMENT_FAILURE_GRACE_PERIOD_DAYS);
        for (Subscription subscription : subscriptionRepository.findByStatusAndPaymentFailedAtBefore(SubscriptionStatus.PAST_DUE, graceThreshold)) {
            subscription.expireImmediately();
        }
    }
}

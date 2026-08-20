package org.example.petinside.domain.subscription.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.petinside.domain.subscription.entity.Subscription;
import org.example.petinside.domain.subscription.entity.SubscriptionStatus;
import org.example.petinside.domain.subscription.repository.SubscriptionRepository;
import org.example.petinside.domain.subscription.service.SubscriptionService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

// F-22: 매일 01:00(만료 배치 이후) 정기결제(자동갱신) 중 다음 결제일이 지났고 아직 결제 실패 기록이
// 없는 구독을 찾아 최초 청구. 한 번 실패하면 이 배치는 더 이상 재시도하지 않고, 유예기간 동안은
// 사용자의 [다시 결제](retry-payment API)로만 재시도됨 - 자동으로 계속 찔러보지 않는다.
// SubscriptionService.chargeNextRound가 건별로 성공/실패를 독립 트랜잭션에 커밋하므로,
// 한 건이 실패해도 나머지 대상은 계속 처리됨.
@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionBillingScheduler {

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionService subscriptionService;

    @Scheduled(cron = "0 0 1 * * *")
    public void chargeDueSubscriptions() {
        for (Subscription subscription : subscriptionRepository.findDueForRecurringCharge(SubscriptionStatus.ACTIVE, LocalDateTime.now())) {
            try {
                subscriptionService.chargeNextRound(subscription);
            } catch (RuntimeException e) {
                log.warn("정기결제 재청구 배치 중 예상치 못한 오류: subscriptionId={}, reason={}", subscription.getId(), e.getMessage());
            }
        }
    }
}

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

// 정기결제 자동 청구를 수행하는 크론 스케줄러
@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionBillingScheduler {

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionService subscriptionService;

    // TODO: 배포 환경 스케줄러 테스트용 임시 변경 — 테스트 끝나면 "0 0 1 * * *"로 되돌릴 것
    @Scheduled(cron = "0 * * * * *")
    public void chargeDueSubscriptions() {
        // 현재 시각 기준, 결제 예정일이 도래한 ACTIVE 상태 정기 구독 목록 조회
        for (Subscription subscription : subscriptionRepository.findDueForRecurringCharge(SubscriptionStatus.ACTIVE, LocalDateTime.now())) {
            try {
                // 개별 구독 정기결제 요청
                subscriptionService.chargeNextRound(subscription);
            } catch (RuntimeException e) {
                // 결제 실패 시 경고 로그 작성 후 다음 결제 대상건으로 진행
                log.warn("정기결제 재청구 배치 중 예상치 못한 오류: subscriptionId={}, reason={}", subscription.getId(), e.getMessage());
            }
        }
    }
}

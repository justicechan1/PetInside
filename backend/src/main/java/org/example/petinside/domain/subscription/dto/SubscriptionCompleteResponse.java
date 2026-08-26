package org.example.petinside.domain.subscription.dto;

import org.example.petinside.domain.subscription.entity.SubscriptionStatus;

import java.time.LocalDateTime;

// 구독 시작(정기결제/1개월 이용권 공통) 완료 응답.
public record SubscriptionCompleteResponse(
        Long subscriptionId,
        SubscriptionStatus status,
        LocalDateTime nextBillingAt
) {
}

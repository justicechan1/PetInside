package org.example.petinside.domain.subscription.dto;

import org.example.petinside.domain.subscription.entity.Subscription;
import org.example.petinside.domain.subscription.entity.SubscriptionStatus;

import java.time.LocalDateTime;

// billingKey 유무로 정기결제(RECURRING)/ 1개월 이용권(ONE_TIME)을 구분. 구독이 없으면 none()으로 hasSubscription=false만 채워 반환.
public record SubscriptionMeResponse(
        boolean hasSubscription,
        Long subscriptionId,
        String type,
        SubscriptionStatus status,
        LocalDateTime startAt,
        LocalDateTime nextBillingAt,
        LocalDateTime canceledAt,
        LocalDateTime paymentFailedAt
) {
    private static final String RECURRING = "RECURRING";
    private static final String ONE_TIME = "ONE_TIME";

    public static SubscriptionMeResponse none() {
        return new SubscriptionMeResponse(false, null, null, null, null, null, null, null);
    }

    public static SubscriptionMeResponse from(Subscription subscription) {
        String type = subscription.getBillingKey() != null ? RECURRING : ONE_TIME;
        return new SubscriptionMeResponse(
                true, subscription.getId(), type, subscription.getStatus(),
                subscription.getStartAt(), subscription.getNextBillingAt(), subscription.getCanceledAt(),
                subscription.getPaymentFailedAt());
    }
}

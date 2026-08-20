package org.example.petinside.domain.subscription.entity;

public enum SubscriptionStatus {
    ACTIVE,
    // F-22: 정기결제 실패 후 유예기간. 혜택은 즉시 차단되지만, 유예기간(3일) 동안은 재시도([다시 결제])로 ACTIVE 복귀 가능.
    PAST_DUE,
    EXPIRED
}

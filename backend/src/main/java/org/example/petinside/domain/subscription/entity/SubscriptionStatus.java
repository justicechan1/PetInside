package org.example.petinside.domain.subscription.entity;

// 사용자의 구독 상태
public enum SubscriptionStatus {
    ACTIVE,     //정상이용
    PAST_DUE,   //결제실패 상태(유예)
    EXPIRED     //구독 종료
}

package org.example.petinside.domain.subscription.entity;

// 사용자의 구독 상태
public enum SubscriptionStatus {
    PENDING,    //1회차 결제 진행 중(동시 구독 시작 차단용 선점)
    ACTIVE,     //정상이용
    PAST_DUE,   //결제실패 상태(유예)
    EXPIRED     //구독 종료
}

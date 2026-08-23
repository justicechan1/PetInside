package org.example.petinside.domain.payment.entity;

// 주문 하나의 진행 상태.
public enum OrderStatus {
    READY,          //대기
    COMPLETED,      //완료
    FAILED          //실패
}

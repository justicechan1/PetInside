package org.example.petinside.domain.payment.entity;

// 결제 시도 한 건의 결과 상태.
public enum PaymentStatus {
    READY,         //대기
    PAID,          //승인
    FAILED         //실패
}
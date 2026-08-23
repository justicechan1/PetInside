package org.example.petinside.domain.payment.dto;

import org.example.petinside.domain.payment.entity.PaymentStatus;

import java.time.LocalDateTime;

// 결제 검증 결과를 프론트에 돌려줄 때 쓰는 응답 DTO.
public record PaymentCompleteResponse(
        String paymentId,
        PaymentStatus status,
        int amount,
        String currency,
        LocalDateTime paidAt
) {
}
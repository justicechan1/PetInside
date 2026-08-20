package org.example.petinside.domain.payment.dto;

import org.example.petinside.domain.payment.entity.PaymentStatus;

import java.time.LocalDateTime;

public record PaymentCompleteResponse(
        String paymentId,
        PaymentStatus status,
        int amount,
        String currency,
        LocalDateTime paidAt
) {
}
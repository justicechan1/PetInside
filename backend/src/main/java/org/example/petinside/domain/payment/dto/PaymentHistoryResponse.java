package org.example.petinside.domain.payment.dto;

import org.example.petinside.domain.payment.entity.Payment;
import org.example.petinside.domain.payment.entity.PaymentStatus;

import java.time.LocalDateTime;

public record PaymentHistoryResponse(
        String paymentId,
        PaymentStatus status,
        int round,
        int amount,
        String currency,
        LocalDateTime paidAt,
        LocalDateTime createdAt
) {
    public static PaymentHistoryResponse from(Payment payment) {
        return new PaymentHistoryResponse(
                payment.getPaymentId(), payment.getStatus(), payment.getRound(),
                payment.getAmount(), payment.getCurrency(), payment.getPaidAt(), payment.getCreatedAt());
    }
}

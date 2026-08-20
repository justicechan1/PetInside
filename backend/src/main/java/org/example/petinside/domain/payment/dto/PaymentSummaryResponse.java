package org.example.petinside.domain.payment.dto;

import org.example.petinside.domain.payment.entity.Payment;

import java.time.LocalDateTime;

public record PaymentSummaryResponse(
        Long id,
        Long userId,
        String username,
        int amount,
        String currency,
        int round,
        String status,
        LocalDateTime paidAt
) {
    public static PaymentSummaryResponse from(Payment payment) {
        return new PaymentSummaryResponse(
                payment.getId(),
                payment.getUser().getId(),
                payment.getUser().getUsername(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getRound(),
                payment.getStatus().name(),
                payment.getPaidAt()
        );
    }
}

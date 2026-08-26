package org.example.petinside.domain.payment.dto;

// 결제 준비 응답
public record PaymentPrepareResponse(
        String paymentId,
        String storeId,
        String channelKey,
        int amount,
        String currency
) {
}
package org.example.petinside.domain.payment.dto;

public record PaymentPrepareResponse(
        String paymentId,
        String storeId,
        String channelKey,
        int amount,
        String currency
) {
}
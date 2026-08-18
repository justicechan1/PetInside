package org.example.petinside.domain.subscription.dto;

public record SubscriptionPrepareResponse(
        String paymentId,
        String storeId,
        String channelKey,
        int amount,
        String currency
) {
}

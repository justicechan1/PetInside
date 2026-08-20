package org.example.petinside.domain.subscription.dto;

public record BillingKeyPrepareResponse(
        String issueId,
        String storeId,
        String channelKey
) {
}

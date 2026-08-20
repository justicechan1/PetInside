package org.example.petinside.domain.subscription.dto;

import org.example.petinside.domain.subscription.entity.SubscriptionStatus;

import java.time.LocalDateTime;

public record SubscriptionCompleteResponse(
        Long subscriptionId,
        SubscriptionStatus status,
        LocalDateTime nextBillingAt
) {
}

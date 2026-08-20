package org.example.petinside.domain.subscription.dto;
import org.example.petinside.domain.subscription.entity.Subscription;
import org.example.petinside.domain.subscription.entity.SubscriptionStatus;

import java.time.LocalDateTime;

public record SubscriptionSummaryResponse (
        Long subscriptionId,
        Long userId,
        String username,
        String nickname,
        SubscriptionStatus status,
        LocalDateTime nextBillingAt,
        LocalDateTime canceledAt
) {
    public static SubscriptionSummaryResponse from(Subscription subscription) {
        return new SubscriptionSummaryResponse(
                subscription.getId(),
                subscription.getUser().getId(),
                subscription.getUser().getUsername(),
                subscription.getUser().getNickname(),
                subscription.getStatus(),
                subscription.getNextBillingAt(),
                subscription.getCanceledAt()
        );
    }
}

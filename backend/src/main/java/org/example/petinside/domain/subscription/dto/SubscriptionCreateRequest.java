package org.example.petinside.domain.subscription.dto;

import jakarta.validation.constraints.NotNull;

public record SubscriptionCreateRequest(
        @NotNull(message = "billingKeyId는 필수입니다.") Long billingKeyId
) {
}

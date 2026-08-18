package org.example.petinside.domain.subscription.dto;

import jakarta.validation.constraints.NotBlank;

public record SubscriptionCompleteRequest(
        @NotBlank(message = "paymentId는 필수입니다.") String paymentId,
        @NotBlank(message = "billingKey는 필수입니다.") String billingKey
) {
}

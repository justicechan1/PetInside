package org.example.petinside.domain.subscription.dto;

import jakarta.validation.constraints.NotNull;

// 정기구독 시작 요청
public record SubscriptionCreateRequest(
        @NotNull(message = "billingKeyId는 필수입니다.") Long billingKeyId
) {
}

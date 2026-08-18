package org.example.petinside.domain.subscription.dto;

import jakarta.validation.constraints.NotBlank;

public record OneTimePurchaseCompleteRequest(
        @NotBlank(message = "paymentId는 필수입니다.") String paymentId
) {
}

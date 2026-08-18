package org.example.petinside.domain.payment.dto;

import jakarta.validation.constraints.NotBlank;

public record PaymentCompleteRequest(
        @NotBlank(message = "paymentId는 필수입니다.") String paymentId
) {
}
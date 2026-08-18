package org.example.petinside.domain.subscription.dto;

import jakarta.validation.constraints.NotBlank;

public record BillingKeyCreateRequest(
        @NotBlank(message = "issueId는 필수입니다.") String issueId,
        @NotBlank(message = "billingKey는 필수입니다.") String billingKey
) {
}

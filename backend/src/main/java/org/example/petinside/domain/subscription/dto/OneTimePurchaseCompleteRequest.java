package org.example.petinside.domain.subscription.dto;

import jakarta.validation.constraints.NotBlank;

// 1개월 이용권(단건) 결제 완료 확인 요청
public record OneTimePurchaseCompleteRequest(
        @NotBlank(message = "paymentId는 필수입니다.") String paymentId
) {
}

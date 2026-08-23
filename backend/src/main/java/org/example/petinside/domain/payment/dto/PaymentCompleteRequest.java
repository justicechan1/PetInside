package org.example.petinside.domain.payment.dto;

import jakarta.validation.constraints.NotBlank;

// 프론트가 결제창에서 결제를 마친 뒤, 이 paymentId를 서버가 재조회해서 확정해줘라고 요청할 때 쓰는 DTO.
public record PaymentCompleteRequest(
        @NotBlank(message = "paymentId는 필수입니다.") String paymentId
) {
}
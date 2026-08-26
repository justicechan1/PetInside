package org.example.petinside.domain.subscription.dto;

import jakarta.validation.constraints.NotBlank;

// 프론트가 PortOne SDK로 카드 등록(빌링키 발급)을 마친 뒤, 그 결과를 서버에 검증·저장 요청할 때 쓰는 DTO.
// issueId는 prepare 단계에서 서버가 미리 발급한 값과 대조해 누가 발급을 시도했는지 확인하는 데 쓰임.
public record BillingKeyCreateRequest(
        @NotBlank(message = "issueId는 필수입니다.") String issueId,
        @NotBlank(message = "billingKey는 필수입니다.") String billingKey
) {
}

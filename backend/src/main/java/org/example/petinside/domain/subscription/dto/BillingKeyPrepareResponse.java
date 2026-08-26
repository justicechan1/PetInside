package org.example.petinside.domain.subscription.dto;

// 빌링키 발급 준비 응답
public record BillingKeyPrepareResponse(
        String issueId,
        String storeId,
        String channelKey
) {
}

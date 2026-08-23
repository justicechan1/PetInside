package org.example.petinside.global.portone.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// PortOne 웹훅 본문(JSON)을 매핑하는 DTO
@JsonIgnoreProperties(ignoreUnknown = true)
public record PortOneWebhookPayload(
        String type,
        Data data
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Data(
            String paymentId,
            String billingKey,
            String issueId
    ) {
    }
}

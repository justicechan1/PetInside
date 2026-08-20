package org.example.petinside.global.portone.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

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

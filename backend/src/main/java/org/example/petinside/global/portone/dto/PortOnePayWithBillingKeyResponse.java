package org.example.petinside.global.portone.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// 빌링키 결제 요청의 즉시 응답
@JsonIgnoreProperties(ignoreUnknown = true)
public record PortOnePayWithBillingKeyResponse(
        PortOnePaymentDetail payment
) {
}

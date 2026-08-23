package org.example.petinside.global.portone.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// PortOne 결제 단건조회 API 응답을 매핑하는 DTO
@JsonIgnoreProperties(ignoreUnknown = true)
public record PortOnePaymentDetail(
        String id,
        String status,
        String storeId,
        String currency,
        String transactionId,
        Amount amount,
        Channel channel
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Amount(long total) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Channel(String type, String key) {
    }
}
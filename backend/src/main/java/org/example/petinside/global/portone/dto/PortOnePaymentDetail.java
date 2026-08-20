package org.example.petinside.global.portone.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

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
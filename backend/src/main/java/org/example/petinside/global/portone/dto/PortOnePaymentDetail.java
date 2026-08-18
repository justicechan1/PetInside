package org.example.petinside.global.portone.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PortOnePaymentDetail(
        String id,
        String status,
        String storeId,
        String currency,
        Amount amount
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Amount(long total) {
    }
}
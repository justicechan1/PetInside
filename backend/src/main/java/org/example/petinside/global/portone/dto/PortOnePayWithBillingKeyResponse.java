package org.example.petinside.global.portone.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PortOnePayWithBillingKeyResponse(
        PortOnePaymentDetail payment
) {
}

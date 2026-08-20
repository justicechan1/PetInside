package org.example.petinside.global.portone.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PortOneBillingKeyDetail(
        String billingKey,
        String status,
        String storeId,
        List<Channel> channels,
        Customer customer
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Channel(String type, String key) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Customer(String id) {
    }
}
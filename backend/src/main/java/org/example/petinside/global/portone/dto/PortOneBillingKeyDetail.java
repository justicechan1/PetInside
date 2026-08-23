package org.example.petinside.global.portone.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

// PortOne 빌링키 단건조회 API 응답. 발급된 빌링키가 진짜 우리 상점/채널/고객 것인지 대조.
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
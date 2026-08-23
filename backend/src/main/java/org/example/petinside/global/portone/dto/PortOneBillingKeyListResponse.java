package org.example.petinside.global.portone.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

// 빌링키 목록조회 응답.
@JsonIgnoreProperties(ignoreUnknown = true)
public record PortOneBillingKeyListResponse(
        List<PortOneBillingKeyDetail> items
) {
}

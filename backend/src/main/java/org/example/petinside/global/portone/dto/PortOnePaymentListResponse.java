package org.example.petinside.global.portone.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

// 결제 목록조회 응답.
@JsonIgnoreProperties(ignoreUnknown = true)
public record PortOnePaymentListResponse(
        List<PortOnePaymentDetail> items
) {
}

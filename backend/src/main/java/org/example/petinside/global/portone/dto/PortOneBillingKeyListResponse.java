package org.example.petinside.global.portone.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PortOneBillingKeyListResponse(
        List<PortOneBillingKeyDetail> items
) {
}

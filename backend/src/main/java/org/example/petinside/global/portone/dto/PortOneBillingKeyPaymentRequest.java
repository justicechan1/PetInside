package org.example.petinside.global.portone.dto;

import java.util.List;

public record PortOneBillingKeyPaymentRequest(
        String billingKey,
        String storeId,
        String channelKey,
        String orderName,
        Customer customer,
        Amount amount,
        String currency,
        List<String> noticeUrls
) {
    public record Customer(String id) {
    }

    public record Amount(long total) {
    }
}

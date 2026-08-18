package org.example.petinside.global.portone.dto;

public record PortOneBillingKeyPaymentRequest(
        String billingKey,
        String storeId,
        String channelKey,
        String orderName,
        Customer customer,
        Amount amount,
        String currency
) {
    public record Customer(String id) {
    }

    public record Amount(long total) {
    }
}

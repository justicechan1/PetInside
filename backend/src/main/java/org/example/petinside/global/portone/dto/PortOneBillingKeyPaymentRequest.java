package org.example.petinside.global.portone.dto;

import java.util.List;

// 서버가 PortOne에 빌링키 결제를 직접 요청할 때 보내는 바디(빌링키 결제 API).
// 프론트 결제창을 거치지 않고 서버→PortOne 직접 호출
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

package org.example.petinside.global.portone;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "portone")
public record PortOneProperties(
        String storeId,
        String channelKey,
        String channelKeySubscription,
        String apiSecret,
        String webhookSecret,
        String paymentIdPrefix,
        String billingKeyEncryptionSecret,
        String webhookNoticeUrl
) {
}
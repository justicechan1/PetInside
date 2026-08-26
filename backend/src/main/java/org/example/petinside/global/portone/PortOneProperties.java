package org.example.petinside.global.portone;

import org.springframework.boot.context.properties.ConfigurationProperties;

// application.yaml의 portone.* 값을 바인딩하는 설정 객체. 값 자체는 .env/GitHub Secrets로만 관리
@ConfigurationProperties(prefix = "portone")
public record PortOneProperties(
        String storeId,
        String channelKey,
        String channelKeySubscription,
        String apiSecret,
        String webhookSecret,
        String paymentIdPrefix,
        String billingKeyEncryptionSecret,
        // PortOne 결제 요청 시 noticeUrls로 실어 보낼 웹훅 수신 주소
        String webhookNoticeUrl,
        // true면 결제 검증 시 채널이 TEST 타입인지까지 확인(로컬/개발 전용, 배포는 false)
        boolean requireTestChannel
) {
}
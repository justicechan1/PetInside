package org.example.petinside.global.portone;

import io.portone.sdk.server.errors.WebhookVerificationException;
import io.portone.sdk.server.webhook.WebhookVerifier;
import org.example.petinside.global.exception.CustomException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

// PortOne PG 웹훅 요청의 무결성 및 위변조 여부를 검증
@Component
public class PortOneWebhookVerifier {

    private final WebhookVerifier verifier;

    // PortOneWebhookVerifier 생성자
    public PortOneWebhookVerifier(PortOneProperties portOneProperties) {
        this.verifier = new WebhookVerifier(portOneProperties.webhookSecret());
    }

    // 수신된 PortOne 웹훅의 HTTP 헤더 및 Raw Body를 검증
    public void verify(String rawBody, String webhookId, String webhookSignature, String webhookTimestamp) {
        try {
            // SDK 내부 서명 재계산 및 검증 수행
            verifier.verify(rawBody, webhookId, webhookSignature, webhookTimestamp);
        } catch (WebhookVerificationException e) {
            // 서명이 불일치하거나 timestamp가 조작된 경우
            throw new CustomException(HttpStatus.BAD_REQUEST.value(), "웹훅 서명 검증에 실패했습니다.");
        } catch (RuntimeException e) {
            // JSON 바기 포맷 오류, 유효하지 않은 인자 전달 시
            throw new CustomException(HttpStatus.BAD_REQUEST.value(), "웹훅 본문을 해석할 수 없습니다.");
        }
    }
}

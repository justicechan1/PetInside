package org.example.petinside.global.portone;

import io.portone.sdk.server.errors.WebhookVerificationException;
import io.portone.sdk.server.webhook.WebhookVerifier;
import org.example.petinside.global.exception.CustomException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

// PortOne 웹훅은 Standard Webhooks 스펙(HMAC-SHA256)을 따르며, 공식 서버 SDK로 검증한다.
// 서명 검증을 통과하지 못한 요청은 위조되었을 수 있으므로 절대 처리하지 않는다.
@Component
public class PortOneWebhookVerifier {

    private final WebhookVerifier verifier;

    public PortOneWebhookVerifier(PortOneProperties portOneProperties) {
        this.verifier = new WebhookVerifier(portOneProperties.webhookSecret());
    }

    public void verify(String rawBody, String webhookId, String webhookSignature, String webhookTimestamp) {
        try {
            verifier.verify(rawBody, webhookId, webhookSignature, webhookTimestamp);
        } catch (WebhookVerificationException e) {
            throw new CustomException(HttpStatus.BAD_REQUEST.value(), "웹훅 서명 검증에 실패했습니다.");
        } catch (RuntimeException e) {
            // SDK가 서명 검증 뒤 body를 자체 타입으로도 파싱하는데, 알 수 없는 이벤트/필드 구조면 여기서 실패할 수 있다.
            // 서명 자체는 유효할 수 있으므로 500이 아니라 400으로 처리한다.
            throw new CustomException(HttpStatus.BAD_REQUEST.value(), "웹훅 본문을 해석할 수 없습니다.");
        }
    }
}

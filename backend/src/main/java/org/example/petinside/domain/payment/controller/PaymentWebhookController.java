package org.example.petinside.domain.payment.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.petinside.domain.payment.service.PaymentWebhookService;
import org.example.petinside.global.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "결제 웹훅", description = "PortOne 결제/취소 상태 변경 웹훅 수신 (F-20)")
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentWebhookController {

    private final PaymentWebhookService paymentWebhookService;

    // PortOne 웹훅 수신 및 이벤트 처리 API
    @PostMapping("/webhook")
    public ResponseEntity<ApiResponse<Void>> webhook(
            @RequestBody String rawBody,
            @RequestHeader(value = "webhook-id", required = false) String webhookId,
            @RequestHeader(value = "webhook-signature", required = false) String webhookSignature,
            @RequestHeader(value = "webhook-timestamp", required = false) String webhookTimestamp) {
        // 서명검증 및 PortOne REST API 단선 재조회 수행
        paymentWebhookService.handle(rawBody, webhookId, webhookSignature, webhookTimestamp);

        // PortOne 서버에 성공 응답(200 OK) 반환
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "OK"));
    }
}

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

    // 서명 검증에 원본 바디 문자열이 그대로 필요하므로 DTO가 아닌 String으로 받음.
    @PostMapping("/webhook")
    public ResponseEntity<ApiResponse<Void>> webhook(
            @RequestBody String rawBody,
            @RequestHeader("webhook-id") String webhookId,
            @RequestHeader("webhook-signature") String webhookSignature,
            @RequestHeader("webhook-timestamp") String webhookTimestamp) {
        paymentWebhookService.handle(rawBody, webhookId, webhookSignature, webhookTimestamp);

        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "OK"));
    }
}

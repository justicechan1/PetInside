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
    // PortOne이 noticeUrls 경유 웹훅은 서명 헤더 없이 보내는 경우가 있어 필수(required)로 두지 않음.
    // 헤더가 없으면 서명 검증 없이 처리하되, 서비스 단에서 반드시 PortOne 재조회로 확정.
    @PostMapping("/webhook")
    public ResponseEntity<ApiResponse<Void>> webhook(
            @RequestBody String rawBody,
            @RequestHeader(value = "webhook-id", required = false) String webhookId,
            @RequestHeader(value = "webhook-signature", required = false) String webhookSignature,
            @RequestHeader(value = "webhook-timestamp", required = false) String webhookTimestamp) {
        paymentWebhookService.handle(rawBody, webhookId, webhookSignature, webhookTimestamp);

        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "OK"));
    }
}

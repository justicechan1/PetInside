package org.example.petinside.domain.payment.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.petinside.domain.payment.dto.PaymentCompleteRequest;
import org.example.petinside.domain.payment.dto.PaymentCompleteResponse;
import org.example.petinside.domain.payment.dto.PaymentHistoryResponse;
import org.example.petinside.domain.payment.dto.PaymentPrepareResponse;
import org.example.petinside.domain.payment.service.PaymentService;
import org.example.petinside.global.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// 빌링키/정기결제가 붙기 전 단계: 결제 준비 + PortOne 단건조회 완료검증까지만 다룸.
// 6단계(빌링키)에서 /api/v1/subscriptions/prepare, complete 로 흡수될 예정
@Tag(name = "결제(임시)", description = "구독 없이 결제 준비/완료검증만 먼저 검증하는 단계용 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/prepare")
    public ResponseEntity<ApiResponse<PaymentPrepareResponse>> prepare(@AuthenticationPrincipal Long userId) {
        PaymentPrepareResponse response = paymentService.prepare(userId);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED.value(), "결제 준비 완료", response));
    }

    @PostMapping("/complete")
    public ResponseEntity<ApiResponse<PaymentCompleteResponse>> complete(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody PaymentCompleteRequest request) {
        PaymentCompleteResponse response = paymentService.complete(userId, request.paymentId());

        return ResponseEntity
                .ok(ApiResponse.success(HttpStatus.OK.value(), "결제가 확인되었습니다", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PaymentHistoryResponse>>> history(@AuthenticationPrincipal Long userId) {
        List<PaymentHistoryResponse> response = paymentService.getHistory(userId);

        return ResponseEntity
                .ok(ApiResponse.success(HttpStatus.OK.value(), "결제 내역 조회 성공", response));
    }
}
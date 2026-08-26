package org.example.petinside.domain.subscription.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.petinside.domain.payment.dto.PaymentPrepareResponse;
import org.example.petinside.domain.subscription.dto.OneTimePurchaseCompleteRequest;
import org.example.petinside.domain.subscription.dto.SubscriptionCompleteResponse;
import org.example.petinside.domain.subscription.dto.SubscriptionCreateRequest;
import org.example.petinside.domain.subscription.dto.SubscriptionMeResponse;
import org.example.petinside.domain.subscription.service.SubscriptionService;
import org.example.petinside.global.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "구독", description = "정기결제(빌링키) 구독 및 1개월 이용권 단건결제 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    // 내 현재 구독 상태 조회 API
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<SubscriptionMeResponse>> me(@AuthenticationPrincipal Long userId) {
        SubscriptionMeResponse response = subscriptionService.getMySubscription(userId);

        return ResponseEntity
                .ok(ApiResponse.success(HttpStatus.OK.value(), "구독 상태 조회 성공", response));
    }

    // 빌링키 기반 정기 구독 신규 신청 API
    @PostMapping
    public ResponseEntity<ApiResponse<SubscriptionCompleteResponse>> create(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody SubscriptionCreateRequest request) {
        SubscriptionCompleteResponse response = subscriptionService.create(userId, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED.value(), "구독이 시작되었습니다", response));
    }

    // 1개월 이용권(단건 결제) 결제 준비 API
    @PostMapping("/one-time/prepare")
    public ResponseEntity<ApiResponse<PaymentPrepareResponse>> prepareOneTime(@AuthenticationPrincipal Long userId) {
        PaymentPrepareResponse response = subscriptionService.prepareOneTime(userId);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED.value(), "결제 준비 완료", response));
    }

    // 1개월 이용권(단건 결제) 승인 완료 및 이용권 등록 API
    @PostMapping("/one-time/complete")
    public ResponseEntity<ApiResponse<SubscriptionCompleteResponse>> completeOneTime(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody OneTimePurchaseCompleteRequest request) {
        SubscriptionCompleteResponse response = subscriptionService.completeOneTime(userId, request.paymentId());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED.value(), "1개월 이용권이 시작되었습니다", response));
    }

    // 정기구독 해지 예약 API
    @PatchMapping("/{subscriptionId}/cancel")
    public ResponseEntity<ApiResponse<SubscriptionMeResponse>> cancel(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long subscriptionId) {
        SubscriptionMeResponse response = subscriptionService.cancel(userId, subscriptionId);

        return ResponseEntity
                .ok(ApiResponse.success(HttpStatus.OK.value(), "구독 해지가 예약되었습니다", response));
    }

    // 정기구독 해지 예약 취소 (구독 재개) API
    @PatchMapping("/{subscriptionId}/resume")
    public ResponseEntity<ApiResponse<SubscriptionMeResponse>> resume(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long subscriptionId) {
        SubscriptionMeResponse response = subscriptionService.resume(userId, subscriptionId);

        return ResponseEntity
                .ok(ApiResponse.success(HttpStatus.OK.value(), "구독이 재개되었습니다", response));
    }

    // 결제 실패 구독의 수동 재결제 시도 API
    @PostMapping("/{subscriptionId}/retry-payment")
    public ResponseEntity<ApiResponse<SubscriptionMeResponse>> retryPayment(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long subscriptionId) {
        SubscriptionMeResponse response = subscriptionService.retryPayment(userId, subscriptionId);

        return ResponseEntity
                .ok(ApiResponse.success(HttpStatus.OK.value(), "결제를 재시도했습니다", response));
    }
}

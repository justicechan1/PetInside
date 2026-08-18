package org.example.petinside.domain.subscription.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.petinside.domain.payment.dto.PaymentPrepareResponse;
import org.example.petinside.domain.subscription.dto.OneTimePurchaseCompleteRequest;
import org.example.petinside.domain.subscription.dto.SubscriptionCompleteResponse;
import org.example.petinside.domain.subscription.dto.SubscriptionCreateRequest;
import org.example.petinside.domain.subscription.service.SubscriptionService;
import org.example.petinside.global.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

    @PostMapping
    public ResponseEntity<ApiResponse<SubscriptionCompleteResponse>> create(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody SubscriptionCreateRequest request) {
        SubscriptionCompleteResponse response = subscriptionService.create(userId, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED.value(), "구독이 시작되었습니다", response));
    }

    // 자동 갱신 없는 1개월 이용권. 빌링키 없이 결제창을 바로 연다.
    @PostMapping("/one-time/prepare")
    public ResponseEntity<ApiResponse<PaymentPrepareResponse>> prepareOneTime(@AuthenticationPrincipal Long userId) {
        PaymentPrepareResponse response = subscriptionService.prepareOneTime(userId);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED.value(), "결제 준비 완료", response));
    }

    @PostMapping("/one-time/complete")
    public ResponseEntity<ApiResponse<SubscriptionCompleteResponse>> completeOneTime(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody OneTimePurchaseCompleteRequest request) {
        SubscriptionCompleteResponse response = subscriptionService.completeOneTime(userId, request.paymentId());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED.value(), "1개월 이용권이 시작되었습니다", response));
    }
}

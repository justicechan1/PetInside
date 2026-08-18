package org.example.petinside.domain.subscription.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.petinside.domain.subscription.dto.SubscriptionCompleteRequest;
import org.example.petinside.domain.subscription.dto.SubscriptionCompleteResponse;
import org.example.petinside.domain.subscription.dto.SubscriptionPrepareResponse;
import org.example.petinside.domain.subscription.service.SubscriptionService;
import org.example.petinside.global.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "구독", description = "빌링키 발급 + 정기결제 준비/완료검증 API (F-21)")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @PostMapping("/prepare")
    public ResponseEntity<ApiResponse<SubscriptionPrepareResponse>> prepare(@AuthenticationPrincipal Long userId) {
        SubscriptionPrepareResponse response = subscriptionService.prepare(userId);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED.value(), "결제 준비 완료", response));
    }

    @PostMapping("/complete")
    public ResponseEntity<ApiResponse<SubscriptionCompleteResponse>> complete(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody SubscriptionCompleteRequest request) {
        SubscriptionCompleteResponse response = subscriptionService.complete(userId, request);

        return ResponseEntity
                .ok(ApiResponse.success(HttpStatus.OK.value(), "구독이 시작되었습니다", response));
    }
}

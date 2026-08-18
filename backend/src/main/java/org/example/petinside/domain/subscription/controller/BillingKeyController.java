package org.example.petinside.domain.subscription.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.petinside.domain.subscription.dto.BillingKeyCreateRequest;
import org.example.petinside.domain.subscription.dto.BillingKeyCreateResponse;
import org.example.petinside.domain.subscription.dto.BillingKeyPrepareResponse;
import org.example.petinside.domain.subscription.service.BillingKeyService;
import org.example.petinside.global.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "빌링키", description = "카드 등록(빌링키 발급) 준비/완료 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/billing-keys")
@RequiredArgsConstructor
public class BillingKeyController {

    private final BillingKeyService billingKeyService;

    @PostMapping("/prepare")
    public ResponseEntity<ApiResponse<BillingKeyPrepareResponse>> prepare(@AuthenticationPrincipal Long userId) {
        BillingKeyPrepareResponse response = billingKeyService.prepare(userId);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED.value(), "빌링키 발급 준비 완료", response));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BillingKeyCreateResponse>> create(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody BillingKeyCreateRequest request) {
        BillingKeyCreateResponse response = billingKeyService.create(userId, request);

        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "카드가 등록되었습니다", response));
    }
}

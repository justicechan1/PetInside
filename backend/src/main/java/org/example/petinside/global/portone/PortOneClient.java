package org.example.petinside.global.portone;

import lombok.RequiredArgsConstructor;
import org.example.petinside.global.exception.CustomException;
import org.example.petinside.global.portone.dto.PortOneBillingKeyDetail;
import org.example.petinside.global.portone.dto.PortOneBillingKeyListResponse;
import org.example.petinside.global.portone.dto.PortOneBillingKeyPaymentRequest;
import org.example.petinside.global.portone.dto.PortOnePayWithBillingKeyResponse;
import org.example.petinside.global.portone.dto.PortOnePaymentDetail;
import org.example.petinside.global.portone.dto.PortOnePaymentListResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Optional;
import java.util.Set;

// PortOne V2 REST API 연동 클라이언트.
@Component
@RequiredArgsConstructor
public class PortOneClient {

    private static final String BASE_URL = "https://api.portone.io";
    // PG 처리 결과 지연에 대응하기 위한 재시도 및 대기 설정
    private static final int SETTLE_RETRY_COUNT = 5;
    private static final long SETTLE_RETRY_DELAY_MS = 1500;
    // 상태
    private static final Set<String> TERMINAL_PAYMENT_STATUSES = Set.of("PAID", "FAILED", "CANCELLED");
    private static final Set<String> TERMINAL_BILLING_KEY_STATUSES = Set.of("ISSUED", "FAILED", "DELETED");

    private final PortOneProperties portOneProperties;

    // PortOne 결제 상세 정보 조회
    public PortOnePaymentDetail getPaymentDetail(String paymentId) {
        PortOnePaymentDetail last = null;
        for (int attempt = 1; attempt <= SETTLE_RETRY_COUNT; attempt++) {
            last = fetchPaymentDetail(paymentId).orElse(null);
            // 최종 상태가 확인되면 즉시 결과 반환
            if (last != null && TERMINAL_PAYMENT_STATUSES.contains(last.status())) {
                return last;
            }
            sleepUnlessLastAttempt(attempt);
        }
        // 최대 재시도 횟수를 초과하더라도 수신된 마지막 상태가 존재하면 반환하여 판단
        if (last != null) {
            return last;
        }
        throw new CustomException(HttpStatus.UNPROCESSABLE_ENTITY.value(), "PortOne 결제 정보를 조회할 수 없습니다.");
    }

    // 단건 조회 및 폴백 목록 조회 조합 메서드
    private Optional<PortOnePaymentDetail> fetchPaymentDetail(String paymentId) {
        try {
            return Optional.ofNullable(client().get()
                    .uri("/payments/{paymentId}", paymentId)
                    .retrieve()
                    .body(PortOnePaymentDetail.class));
        } catch (RestClientException e) {
            // 단건 조회 API 404/실패 시 목록 조회
            return findInPaymentList(paymentId);
        }
    }

    // PortOne 전체 결제 목록 API를 조회하여 paymentId 매칭
    private Optional<PortOnePaymentDetail> findInPaymentList(String paymentId) {
        try {
            PortOnePaymentListResponse response = client().get()
                    .uri("/payments?page.size=1000&sort.by=REQUESTED_AT&sort.order=DESC")
                    .retrieve()
                    .body(PortOnePaymentListResponse.class);
            return response.items().stream()
                    .filter(item -> paymentId.equals(item.id()))
                    .findFirst();
        } catch (RestClientException e) {
            return Optional.empty();
        }
    }

    // 결제 건 존재 여부 확인용 단건 조회
    public Optional<PortOnePaymentDetail> findPaymentDetail(String paymentId) {
        return findInPaymentList(paymentId);
    }

    // PortOne 빌링키 상세 정보 조회
    public PortOneBillingKeyDetail getBillingKeyDetail(String billingKey) {
        PortOneBillingKeyDetail last = null;
        for (int attempt = 1; attempt <= SETTLE_RETRY_COUNT; attempt++) {
            last = fetchBillingKeyDetail(billingKey).orElse(null);
            if (last != null && TERMINAL_BILLING_KEY_STATUSES.contains(last.status())) {
                return last;
            }
            sleepUnlessLastAttempt(attempt);
        }
        if (last != null) {
            return last;
        }
        throw new CustomException(HttpStatus.UNPROCESSABLE_ENTITY.value(), "PortOne 빌링키 정보를 조회할 수 없습니다.");
    }

    // 빌링키 단건 조회 및 폴백 목록 조회 조합 메서드
    private Optional<PortOneBillingKeyDetail> fetchBillingKeyDetail(String billingKey) {
        try {
            return Optional.ofNullable(client().get()
                    .uri("/billing-keys/{billingKey}", billingKey)
                    .retrieve()
                    .body(PortOneBillingKeyDetail.class));
        } catch (RestClientException e) {
            // 단건 조회 실패시 목록 조회
            return findInBillingKeyList(billingKey);
        }
    }

    // PortOne 전체 빌링키 목록 API를 조회하여 billingKey 매칭
    private Optional<PortOneBillingKeyDetail> findInBillingKeyList(String billingKey) {
        try {
            PortOneBillingKeyListResponse response = client().get()
                    .uri("/billing-keys?page.size=1000")
                    .retrieve()
                    .body(PortOneBillingKeyListResponse.class);
            return response.items().stream()
                    .filter(item -> billingKey.equals(item.billingKey()))
                    .findFirst();
        } catch (RestClientException e) {
            return Optional.empty();
        }
    }

    // 재시도 간격(1.5초) 동안 스레드 대기 처리
    private void sleepUnlessLastAttempt(int attempt) {
        if (attempt == SETTLE_RETRY_COUNT) {
            return;
        }
        try {
            Thread.sleep(SETTLE_RETRY_DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // 프론트엔드 결제창 호출 없이, 정기 결제 주기마다 서버 단독으로 카드사에 결제 승인을 요청
    public PortOnePaymentDetail payWithBillingKey(String paymentId, PortOneBillingKeyPaymentRequest request) {
        try {
            PortOnePayWithBillingKeyResponse response = client().post()
                    .uri("/payments/{paymentId}/billing-key", paymentId)
                    .body(request)
                    .retrieve()
                    .body(PortOnePayWithBillingKeyResponse.class);
            return response.payment();
        } catch (RestClientException e) {
            throw new CustomException(HttpStatus.UNPROCESSABLE_ENTITY.value(), "빌링키 결제 요청에 실패했습니다.");
        }
    }

    // PortOne V2 API 인증 규격에 맞는 인스턴스 생성
    private RestClient client() {
        // V2 API Secret은 별도 토큰 교환 없이 "PortOne {API_SECRET}" 형식으로 바로 사용
        return RestClient.create(BASE_URL).mutate()
                .defaultHeader("Authorization", "PortOne " + portOneProperties.apiSecret())
                .build();
    }
}

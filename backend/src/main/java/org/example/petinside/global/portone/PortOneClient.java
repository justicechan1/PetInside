package org.example.petinside.global.portone;

import lombok.RequiredArgsConstructor;
import org.example.petinside.global.exception.CustomException;
import org.example.petinside.global.portone.dto.PortOneBillingKeyDetail;
import org.example.petinside.global.portone.dto.PortOneBillingKeyPaymentRequest;
import org.example.petinside.global.portone.dto.PortOnePayWithBillingKeyResponse;
import org.example.petinside.global.portone.dto.PortOnePaymentDetail;
import org.example.petinside.global.portone.dto.PortOnePaymentListResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PortOneClient {

    private static final String BASE_URL = "https://api.portone.io";

    private final PortOneProperties portOneProperties;

    // PortOne 결제 단건 조회. 프론트/웹훅의 결과를 그대로 믿지 않고 이 응답으로 최종 확정한다.
    // 주의: 이 테스트 환경에서는 GET /payments/{id}가 실제 존재하는 건도 404를 내는 경우가 있어(목록 조회엔 정상적으로 나옴),
    // 단건조회가 실패하면 목록조회로 한 번 더 확인한다.
    public PortOnePaymentDetail getPaymentDetail(String paymentId) {
        try {
            return client().get()
                    .uri("/payments/{paymentId}", paymentId)
                    .retrieve()
                    .body(PortOnePaymentDetail.class);
        } catch (RestClientException e) {
            return findInPaymentList(paymentId)
                    .orElseThrow(() -> new CustomException(HttpStatus.UNPROCESSABLE_ENTITY.value(), "PortOne 결제 정보를 조회할 수 없습니다."));
        }
    }

    private Optional<PortOnePaymentDetail> findInPaymentList(String paymentId) {
        try {
            PortOnePaymentListResponse response = client().get()
                    .uri("/payments?page.size=100")
                    .retrieve()
                    .body(PortOnePaymentListResponse.class);
            return response.items().stream()
                    .filter(item -> paymentId.equals(item.id()))
                    .findFirst();
        } catch (RestClientException e) {
            return Optional.empty();
        }
    }

    // requestIssueBillingKeyAndPay(휴대폰 인증)처럼 프론트 단계에서 이미 1회차 결제까지 끝난 경우를 구분하기 위한 조회.
    // 아직 결제가 실행되지 않은 paymentId(PortOne이 모르는 값)면 Optional.empty()로 구분한다.
    public Optional<PortOnePaymentDetail> findPaymentDetail(String paymentId) {
        return findInPaymentList(paymentId);
    }

    // 프론트에서 발급된 빌링키를 그대로 신뢰하지 않고, 발급 상태(ISSUED)와 소속 Store를 재확인한다.
    public PortOneBillingKeyDetail getBillingKeyDetail(String billingKey) {
        try {
            return client().get()
                    .uri("/billing-keys/{billingKey}", billingKey)
                    .retrieve()
                    .body(PortOneBillingKeyDetail.class);
        } catch (RestClientException e) {
            throw new CustomException(HttpStatus.UNPROCESSABLE_ENTITY.value(), "PortOne 빌링키 정보를 조회할 수 없습니다.");
        }
    }

    // 빌링키로 1회차 결제를 실행(서버→PortOne 직접 호출이라 프론트 결제창을 거치지 않는다)
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

    private RestClient client() {
        // V2 API Secret은 별도 토큰 교환 없이 "PortOne {API_SECRET}" 형식으로 바로 사용
        return RestClient.create(BASE_URL).mutate()
                .defaultHeader("Authorization", "PortOne " + portOneProperties.apiSecret())
                .build();
    }
}
package org.example.petinside.domain.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.petinside.domain.payment.entity.Payment;
import org.example.petinside.domain.payment.entity.PaymentStatus;
import org.example.petinside.domain.payment.repository.PaymentRepository;
import org.example.petinside.domain.subscription.entity.BillingKeyIssuanceIntent;
import org.example.petinside.domain.subscription.entity.Subscription;
import org.example.petinside.domain.subscription.repository.BillingKeyIssuanceIntentRepository;
import org.example.petinside.domain.subscription.service.BillingKeyService;
import org.example.petinside.global.exception.CustomException;
import org.example.petinside.global.portone.PortOneClient;
import org.example.petinside.global.portone.PortOneWebhookVerifier;
import org.example.petinside.global.portone.dto.PortOnePaymentDetail;
import org.example.petinside.global.portone.dto.PortOneWebhookPayload;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// PortOne이 결제/빌링키 관련 이벤트가 생겼을 때 보내는 웹훅을 처리하는 서비스.
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentWebhookService {

    private static final String CANCELLED_EVENT = "Transaction.Cancelled";
    private static final String BILLING_KEY_ISSUED_EVENT = "BillingKey.Issued";

    private final PortOneWebhookVerifier webhookVerifier;
    private final ObjectMapper objectMapper;
    private final PaymentRepository paymentRepository;
    private final BillingKeyIssuanceIntentRepository intentRepository;
    private final PortOneClient portOneClient;
    private final PaymentService paymentService;
    private final BillingKeyService billingKeyService;

    // PortOne 웹훅 이벤트 통합 처리
    @Transactional
    public void handle(String rawBody, String webhookId, String webhookSignature, String webhookTimestamp) {
        boolean hasSignatureHeaders = webhookId != null && webhookSignature != null && webhookTimestamp != null;
        log.info("웹훅 수신: 서명헤더존재={}", hasSignatureHeaders);

        // 웹훅 서명 헤더 검증 (헤더가 없는 경우는 생략
        if (hasSignatureHeaders) {
            webhookVerifier.verify(rawBody, webhookId, webhookSignature, webhookTimestamp);
        }

        // Json Body 파싱
        PortOneWebhookPayload payload = parse(rawBody);
        if (payload.type() == null || payload.data() == null) {
            return;
        }
        log.info("웹훅 이벤트 처리: type={}, paymentId={}", payload.type(), payload.data().paymentId());

        // 빌링크 발급 완료 이벤트 분기 처리
        if (BILLING_KEY_ISSUED_EVENT.equals(payload.type())) {
            handleBillingKeyIssued(payload.data());
            return;
        }

        // 결제 관련 이벤트 검증
        if (payload.data().paymentId() == null) {
            return;
        }

        // DB에서 해당 결제 건 조회
        Payment payment = paymentRepository.findByPaymentId(payload.data().paymentId()).orElse(null);
        if (payment == null) {
            return;
        }

        // 결제 취소 이벤트 분기 처리
        if (CANCELLED_EVENT.equals(payload.type())) {
            handleCancelled(payment);
            return;
        }

        // 결제 완료 승인 처리(웹훅 본문을 그대로 믿지 않고 PortOne을 재조회해서 동기화.)
        if (payment.getStatus() == PaymentStatus.READY) {
            PortOnePaymentDetail detail = portOneClient.getPaymentDetail(payment.getPaymentId());
            if (PaymentService.PAID_STATUS.equalsIgnoreCase(detail.status())) {
                // 완료 API(verifyAndMarkPaid)와 동시에 같은 결제를 확정하려는 경합을 막기 위해
                // 락을 걸고 재조회 - 락 획득 후 다시 READY인지 확인해서, 그 사이 API 경로가
                // 이미 확정했다면 중복 처리하지 않는다(2026-08-26, payment_transaction INSERT
                // 락 대기시간 초과로 재현됨).
                Payment locked = paymentRepository.findByPaymentIdForUpdate(payment.getPaymentId()).orElse(null);
                if (locked != null && locked.getStatus() == PaymentStatus.READY) {
                    paymentService.finalizeByDetail(locked, detail);
                }
            }
        }
        // 이미 PAID/FAILED로 처리된 결제에 대한 중복 웹훅은 별도 처리 없이 무처리(멱등)
    }

    // 빌링키 발급 비동기 복구 처리
    private void handleBillingKeyIssued(PortOneWebhookPayload.Data data) {
        if (data.issueId() == null || data.billingKey() == null) {
            return;
        }

        // 사전에 등록해둔 발급 의도 조회
        BillingKeyIssuanceIntent intent = intentRepository.findByIssueId(data.issueId()).orElse(null);
        if (intent == null || intent.isCompleted()) {
            return; // 이미 완료 API로 처리됐거나 우리가 모르는 발급 의도
        }

        billingKeyService.verifyAndStore(intent, data.billingKey());
    }

    // 결제 취소 및 환불 처리
    private void handleCancelled(Payment payment) {
        Subscription subscription = payment.getSubscription();
        if (subscription == null) {
            return; // 연관돈 구독 정보가 없으면 처리 스킵
        }

        // PortOne REST API 재조회로 실제 취소 여부 교차 검증
        PortOnePaymentDetail detail = portOneClient.getPaymentDetail(payment.getPaymentId());
        if (!"CANCELLED".equals(detail.status())) {
            return;
        }

        // 환불/취소 확인 시 유예 기간 없이 구독 즉시 만료
        subscription.expireImmediately();
    }

    // 웹훅 Request Body -> DTO 파싱
    private PortOneWebhookPayload parse(String rawBody) {
        try {
            return objectMapper.readValue(rawBody, PortOneWebhookPayload.class);
        } catch (Exception e) {
            throw new CustomException(HttpStatus.BAD_REQUEST.value(), "웹훅 본문을 해석할 수 없습니다.");
        }
    }
}

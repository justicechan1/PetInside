package org.example.petinside.domain.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
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

    @Transactional
    public void handle(String rawBody, String webhookId, String webhookSignature, String webhookTimestamp) {
        // PortOne이 noticeUrls 경유 웹훅은 서명 헤더 없이 보내는 경우가 있음.
        // 그 경우 서명 검증은 생략하되, 아래 로직이 어차피 본문을 그대로 믿지 않고
        // PortOne 재조회로 확정하므로 안전함(취소 이벤트도 handleCancelled에서 재조회로 확인).
        if (webhookId != null && webhookSignature != null && webhookTimestamp != null) {
            webhookVerifier.verify(rawBody, webhookId, webhookSignature, webhookTimestamp);
        }

        PortOneWebhookPayload payload = parse(rawBody);
        if (payload.type() == null || payload.data() == null) {
            return; // 우리가 다루지 않는 이벤트는 안전하게 무시
        }

        if (BILLING_KEY_ISSUED_EVENT.equals(payload.type())) {
            handleBillingKeyIssued(payload.data());
            return;
        }

        if (payload.data().paymentId() == null) {
            return;
        }

        Payment payment = paymentRepository.findByPaymentId(payload.data().paymentId()).orElse(null);
        if (payment == null) {
            return; // 다른 팀/다른 흐름의 결제일 수 있으므로 무시
        }

        if (CANCELLED_EVENT.equals(payload.type())) {
            handleCancelled(payment);
            return;
        }

        // 완료 API와 동일하게: 웹훅 본문을 그대로 믿지 않고 PortOne을 재조회해서 동기화.
        if (payment.getStatus() == PaymentStatus.READY) {
            PortOnePaymentDetail detail = portOneClient.getPaymentDetail(payment.getPaymentId());
            paymentService.finalizeByDetail(payment, detail);
        }
        // 이미 PAID/FAILED로 처리된 결제에 대한 중복 웹훅은 별도 처리 없이 무처리(멱등)
    }

    // 프론트가 requestIssueBillingKey 성공 후 /billing-keys 호출 전에 이탈한 경우를 복구.
    private void handleBillingKeyIssued(PortOneWebhookPayload.Data data) {
        if (data.issueId() == null || data.billingKey() == null) {
            return;
        }

        BillingKeyIssuanceIntent intent = intentRepository.findByIssueId(data.issueId()).orElse(null);
        if (intent == null || intent.isCompleted()) {
            return; // 이미 완료 API로 처리됐거나 우리가 모르는 발급 의도
        }

        billingKeyService.verifyAndStore(intent, data.billingKey());
    }

    // 관리자 콘솔 수동 환불 등으로 발생한 취소는 해지 유예 없이 구독을 즉시 차단.
    // 웹훅 본문의 이벤트 타입을 그대로 믿지 않고, PortOne 재조회로 실제 취소 여부를 확인한 뒤에만 반영.
    private void handleCancelled(Payment payment) {
        Subscription subscription = payment.getSubscription();
        if (subscription == null) {
            return;
        }

        PortOnePaymentDetail detail = portOneClient.getPaymentDetail(payment.getPaymentId());
        if (!"CANCELLED".equals(detail.status())) {
            return;
        }

        subscription.expireImmediately();
    }

    private PortOneWebhookPayload parse(String rawBody) {
        try {
            return objectMapper.readValue(rawBody, PortOneWebhookPayload.class);
        } catch (Exception e) {
            throw new CustomException(HttpStatus.BAD_REQUEST.value(), "웹훅 본문을 해석할 수 없습니다.");
        }
    }
}

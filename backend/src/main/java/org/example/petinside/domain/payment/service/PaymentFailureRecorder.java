package org.example.petinside.domain.payment.service;

import lombok.RequiredArgsConstructor;
import org.example.petinside.domain.payment.entity.Payment;
import org.example.petinside.domain.payment.entity.PaymentStatus;
import org.example.petinside.domain.payment.entity.PaymentTransaction;
import org.example.petinside.domain.payment.repository.PaymentRepository;
import org.example.petinside.domain.payment.repository.PaymentTransactionRepository;
import org.example.petinside.global.exception.CustomException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

// PaymentService.finalizeByDetail()의 검증 실패 경로 전용. 별도 빈으로 분리해야
// REQUIRES_NEW가 프록시를 거쳐 실제로 적용된다(같은 클래스 내부 self-invocation은 AOP가 안 먹음).
@Component
@RequiredArgsConstructor
public class PaymentFailureRecorder {

    private final PaymentRepository paymentRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;

    // 호출부(웹훅 핸들러 등)의 트랜잭션이 이후 던지는 예외로 롤백되더라도, 검증 실패 기록만은
    // 별도 트랜잭션으로 즉시 커밋해 남긴다. 안 그러면 FAILED 마킹이 통째로 사라지고
    // 결제/주문이 영영 READY로 남아 PaymentExpirationScheduler에만 의존하게 된다.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordVerificationFailure(Long paymentId, String transactionId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND.value(), "결제 내역을 찾을 수 없습니다."));
        paymentTransactionRepository.save(PaymentTransaction.record(payment, transactionId, PaymentStatus.FAILED));
        payment.markFailed();
        payment.getOrder().markFailed();
    }
}

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

// 결제 검증 실패 발생 시, 상위 트랜잭션의 롤백 여부와 무관하게 실패 상태를 독립 커밋 기록.
// AOP가 적용되지 않아 별도 구현..
@Component
@RequiredArgsConstructor
public class PaymentFailureRecorder {

    private final PaymentRepository paymentRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;

    // 결제 검증 실패 기록 및 상태 변환 (FAILED) 독립 커밋
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordVerificationFailure(Long paymentId, String transactionId) {
        // 대상 결제 건 조회
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND.value(), "결제 내역을 찾을 수 없습니다."));
        // FAILED 상태의 결제 트랜잭션 내역 기록
        paymentTransactionRepository.save(PaymentTransaction.record(payment, transactionId, PaymentStatus.FAILED));
        // Payment 및 연관된 Order 엔티티의 상태를 FAILED로 변경
        payment.markFailed();
        payment.getOrder().markFailed();
    }
}

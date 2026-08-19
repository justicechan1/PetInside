package org.example.petinside.domain.payment.repository;

import org.example.petinside.domain.payment.entity.Payment;
import org.example.petinside.domain.payment.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByPaymentId(String paymentId);

    List<Payment> findByUserIdOrderByCreatedAtDesc(Long userId);

    // 결제창을 열어놓고 이탈해서 영영 READY로 남는 시도를 자동 만료 처리할 때 사용
    List<Payment> findByStatusAndCreatedAtBefore(PaymentStatus status, LocalDateTime threshold);
}
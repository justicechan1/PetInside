package org.example.petinside.domain.payment.repository;

import org.example.petinside.domain.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByPaymentId(String paymentId);

    List<Payment> findByUserIdOrderByCreatedAtDesc(Long userId);
}
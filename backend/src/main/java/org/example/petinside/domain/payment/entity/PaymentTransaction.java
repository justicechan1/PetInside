package org.example.petinside.domain.payment.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

// 같은 paymentId로 PortOne에 실제 승인을 시도한 각 건. PortOne이 부여하는 transactionId로 식별.
@Entity
@Table(name = "payment_transaction")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class PaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @Column(name = "transaction_id")
    private String transactionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @CreatedDate
    @Column(name = "recorded_at", nullable = false, updatable = false)
    private LocalDateTime recordedAt;

    @Builder
    private PaymentTransaction(Payment payment, String transactionId, PaymentStatus status) {
        this.payment = payment;
        this.transactionId = transactionId;
        this.status = status;
    }

    public static PaymentTransaction record(Payment payment, String transactionId, PaymentStatus status) {
        return PaymentTransaction.builder()
                .payment(payment)
                .transactionId(transactionId)
                .status(status)
                .build();
    }
}

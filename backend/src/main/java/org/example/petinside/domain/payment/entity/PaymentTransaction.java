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

    // 성공/실패 결과와 무관하게 승인을 시도했다는 사실 자체를 기록. 실패한 시도도 남겨야
    // 재시도 이력을 추적할 수 있으므로, 호출부에서 결과가 확정된 뒤 이 메서드로 저장.
    public static PaymentTransaction record(Payment payment, String transactionId, PaymentStatus status) {
        return PaymentTransaction.builder()
                .payment(payment)
                .transactionId(transactionId)
                .status(status)
                .build();
    }
}

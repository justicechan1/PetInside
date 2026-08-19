package org.example.petinside.domain.payment.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.petinside.domain.subscription.entity.Subscription;
import org.example.petinside.domain.user.entity.User;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "payment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 이 결제 시도가 어떤 주문에 대한 것인지. 주문과 결제 시도를 분리해서, 한 주문에 결제 재시도가
    // 여러 번 있었던 이력을 남길 수 있게 한다(portone1.md 1단계 원칙).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    // prepare 시점엔 구독이 아직 없어 nullable. complete에서 빌링키 검증까지 끝나야 채워짐(linkSubscription).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id")
    private Subscription subscription;

    @Column(name = "payment_id", nullable = false, unique = true)
    private String paymentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Column(nullable = false)
    private int round;

    @Column(nullable = false)
    private int amount;

    @Column(nullable = false)
    private String currency;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private Payment(User user, Order order, String paymentId, int round, int amount, String currency) {
        this.user = user;
        this.order = order;
        this.paymentId = paymentId;
        this.status = PaymentStatus.READY;
        this.round = round;
        this.amount = amount;
        this.currency = currency;
    }

    public static Payment createReady(User user, Order order, String paymentId, int round, int amount, String currency) {
        return Payment.builder()
                .user(user)
                .order(order)
                .paymentId(paymentId)
                .round(round)
                .amount(amount)
                .currency(currency)
                .build();
    }

    public void markPaid(LocalDateTime paidAt) {
        this.status = PaymentStatus.PAID;
        this.paidAt = paidAt;
    }

    public void markFailed() {
        this.status = PaymentStatus.FAILED;
    }

    public void linkSubscription(Subscription subscription) {
        this.subscription = subscription;
    }

    public boolean isReady() {
        return this.status == PaymentStatus.READY;
    }
}
package org.example.petinside.domain.payment.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.petinside.domain.user.entity.User;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

// 사용자가 "결제하고 싶다"고 시작한 구매 의도(서버 확정 금액 포함) 하나. 결제 시도(Payment)와 분리해서,
// 한 주문에 결제 재시도가 여러 번 있었던 이력을 남길 수 있게 한다(portone1.md 1단계 원칙).
@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(nullable = false)
    private int amount;

    @Column(nullable = false)
    private String currency;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private Order(User user, int amount, String currency) {
        this.user = user;
        this.status = OrderStatus.READY;
        this.amount = amount;
        this.currency = currency;
    }

    public static Order ready(User user, int amount, String currency) {
        return Order.builder()
                .user(user)
                .amount(amount)
                .currency(currency)
                .build();
    }

    public void markCompleted() {
        this.status = OrderStatus.COMPLETED;
    }

    public void markFailed() {
        this.status = OrderStatus.FAILED;
    }
}

package org.example.petinside.domain.subscription.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.petinside.domain.user.entity.User;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "subscription")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionStatus status;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "next_billing_at", nullable = false)
    private LocalDateTime nextBillingAt;

    @Column(name = "canceled_at")
    private LocalDateTime canceledAt;

    @Column(name = "payment_failed_at")
    private LocalDateTime paymentFailedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    private Subscription(User user, LocalDateTime startAt, LocalDateTime nextBillingAt) {
        this.user = user;
        this.status = SubscriptionStatus.ACTIVE;
        this.startAt = startAt;
        this.nextBillingAt = nextBillingAt;
    }

    // 빌링키 발급 + 1회차 결제 성공 시 구독 시작 (F-21)
    public static Subscription activate(User user, LocalDateTime startAt, LocalDateTime nextBillingAt) {
        return Subscription.builder()
                .user(user)
                .startAt(startAt)
                .nextBillingAt(nextBillingAt)
                .build();
    }

    // PortOne 관리자 콘솔에서 수동 환불된 경우(Transaction.Cancelled 웹훅) 유예 없이 즉시 만료
    public void expireImmediately() {
        this.status = SubscriptionStatus.EXPIRED;
    }
}

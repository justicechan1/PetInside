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

    // 정기결제(자동갱신) 구독만 빌링키를 가짐. 단건(1개월 이용권) 구매는 카드 등록 자체가 없어 null.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "billing_key_id")
    private BillingKey billingKey;

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
    private Subscription(User user, BillingKey billingKey, LocalDateTime startAt, LocalDateTime nextBillingAt, LocalDateTime canceledAt) {
        this.user = user;
        this.billingKey = billingKey;
        this.status = SubscriptionStatus.ACTIVE;
        this.startAt = startAt;
        this.nextBillingAt = nextBillingAt;
        this.canceledAt = canceledAt;
    }

    // 이미 검증·저장된 빌링키로 1회차 결제 성공 시 정기구독 시작 (F-21)
    public static Subscription activate(User user, BillingKey billingKey, LocalDateTime startAt, LocalDateTime nextBillingAt) {
        return Subscription.builder()
                .user(user)
                .billingKey(billingKey)
                .startAt(startAt)
                .nextBillingAt(nextBillingAt)
                .build();
    }

    // 1개월 이용권 단건 구매. 자동 갱신이 없으므로 생성 시점에 이미 해지 예약된 상태로 시작해
    // 다음 배치 때 canceledAt+nextBillingAt(=만료일) 기준으로 자동 만료되게 함.
    public static Subscription purchaseOneTime(User user, LocalDateTime startAt, LocalDateTime expiresAt) {
        return Subscription.builder()
                .user(user)
                .billingKey(null)
                .startAt(startAt)
                .nextBillingAt(expiresAt)
                .canceledAt(startAt)
                .build();
    }

    // PortOne 관리자 콘솔에서 수동 환불된 경우(Transaction.Cancelled 웹훅) 유예 없이 즉시 만료
    public void expireImmediately() {
        this.status = SubscriptionStatus.EXPIRED;
    }

    // F-23: 정기결제(자동갱신) 해지 예약. 이미 승인된 회차는 그대로 두고, 다음 결제만 막음.
    // 1개월 이용권은 애초에 생성 시점부터 canceledAt이 채워져 있어 대상이 아님.
    public boolean isRecurring() {
        return billingKey != null;
    }

    public void cancel() {
        this.canceledAt = LocalDateTime.now();
    }

    public void resume() {
        this.canceledAt = null;
    }
}

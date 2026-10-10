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

// 사용자의 구독 계약 자체를 나타내는 엔티티(정기결제/1개월 이용권 공통).
// 상태는 ACTIVE(정상 이용중) → PAST_DUE(결제 실패, 혜택 즉시 차단, 유예기간) → EXPIRED(종료) 순서.
// canceledAt은 해지 의사를 언제 밝혔는지만 기록, 실제 종료(EXPIRED)는 배치나 즉시 만료 메서드가 처리.
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

    // 정기구독 시작 요청이 1회차 결제를 하는 동안 자리를 선점하는 행. 유저 락 안에서 저장해,
    // 결제 중에 들어온 같은 사용자의 다른 시작 요청이 이 행을 보고 거절되게 한다.
    public static Subscription pending(User user, BillingKey billingKey, LocalDateTime now) {
        Subscription subscription = activate(user, billingKey, now, now);
        subscription.status = SubscriptionStatus.PENDING;
        return subscription;
    }

    // 1회차 결제가 확정되면 선점 행을 정상 구독으로 전환
    public void activatePending(LocalDateTime startAt, LocalDateTime nextBillingAt) {
        this.status = SubscriptionStatus.ACTIVE;
        this.startAt = startAt;
        this.nextBillingAt = nextBillingAt;
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

    // 정상(ACTIVE) 구독의 해지 예약: 이미 승인된 회차는 그대로 두고 다음 결제만 막음.
    // PAST_DUE(유예기간) 중의 해지는 다른 의미라 여기서 다루지 않고 cancelDuringGracePeriod()로 분리.
    public void cancel() {
        this.canceledAt = LocalDateTime.now();
    }

    // 결제 실패 배너에서 [구독 취소]를 누른 경우: 유예기간 중엔 이미 새 회차 결제가 안 된 상태라
    // 더 기다릴 이유가 없으므로, 해지 요청 즉시 만료시킴(유예기간 만료를 기다리지 않음).
    public void cancelDuringGracePeriod() {
        this.status = SubscriptionStatus.EXPIRED;
        this.canceledAt = LocalDateTime.now();
        this.paymentFailedAt = null;
    }

    public void resume() {
        this.canceledAt = null;
    }

    // F-22: 정기결제 회차 성공. 다음 결제일을 한 달 뒤로 미루고(1회차와 동일한 규칙),
    // 유예기간(PAST_DUE) 중이었다면 정상(ACTIVE)으로 복귀시키고 실패 기록을 지움.
    public void chargeSucceeded(LocalDateTime paidAt) {
        this.nextBillingAt = paidAt.plusMonths(1).minusDays(1);
        this.status = SubscriptionStatus.ACTIVE;
        this.paymentFailedAt = null;
    }

    // F-22: 정기결제 회차 실패. 혜택을 즉시 차단하기 위해 PAST_DUE로 전환하고,
    // 재시도 때마다 갱신하지 않고 최초 실패 시각만 고정(유예기간 3일 계산 기준).
    public void markPaymentFailed() {
        if (this.paymentFailedAt == null) {
            this.paymentFailedAt = LocalDateTime.now();
        }
        this.status = SubscriptionStatus.PAST_DUE;
    }
}

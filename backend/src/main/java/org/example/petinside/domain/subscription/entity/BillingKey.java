package org.example.petinside.domain.subscription.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "billing_key")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BillingKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", nullable = false)
    private Subscription subscription;

    // PortOne 빌링키 원문은 절대 저장하지 않고 BillingKeyEncryptor로 암호화한 값만 저장
    @Column(name = "billing_key_encrypted", nullable = false)
    private String billingKeyEncrypted;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @Column(name = "issued_at", nullable = false)
    private LocalDateTime issuedAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Builder
    private BillingKey(Subscription subscription, String billingKeyEncrypted, LocalDateTime issuedAt) {
        this.subscription = subscription;
        this.billingKeyEncrypted = billingKeyEncrypted;
        this.isActive = true;
        this.issuedAt = issuedAt;
    }

    public static BillingKey issue(Subscription subscription, String billingKeyEncrypted, LocalDateTime issuedAt) {
        return BillingKey.builder()
                .subscription(subscription)
                .billingKeyEncrypted(billingKeyEncrypted)
                .issuedAt(issuedAt)
                .build();
    }

    public void revoke(LocalDateTime revokedAt) {
        this.isActive = false;
        this.revokedAt = revokedAt;
    }
}

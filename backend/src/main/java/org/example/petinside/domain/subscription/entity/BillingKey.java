package org.example.petinside.domain.subscription.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.petinside.domain.user.entity.User;

import java.time.LocalDateTime;

// 빌링키는 특정 구독 건이 아니라 사용자에게 귀속된다. 구독을 해지했다가 재구독해도
// 카드가 그대로면 재등록 없이 같은 빌링키를 재사용할 수 있어야 함.
@Entity
@Table(name = "billing_key")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BillingKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // PortOne 빌링키 원문은 절대 저장하지 않고 BillingKeyEncryptor로 암호화한 값만 저장.
    @Column(name = "billing_key_encrypted", nullable = false)
    private String billingKeyEncrypted;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @Column(name = "issued_at", nullable = false)
    private LocalDateTime issuedAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Builder
    private BillingKey(User user, String billingKeyEncrypted, LocalDateTime issuedAt) {
        this.user = user;
        this.billingKeyEncrypted = billingKeyEncrypted;
        this.isActive = true;
        this.issuedAt = issuedAt;
    }

    public static BillingKey issue(User user, String billingKeyEncrypted, LocalDateTime issuedAt) {
        return BillingKey.builder()
                .user(user)
                .billingKeyEncrypted(billingKeyEncrypted)
                .issuedAt(issuedAt)
                .build();
    }

    public void revoke(LocalDateTime revokedAt) {
        this.isActive = false;
        this.revokedAt = revokedAt;
    }
}

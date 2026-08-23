package org.example.petinside.domain.subscription.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.petinside.domain.user.entity.User;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

// 프론트가 빌링키 발급 SDK 호출 후 이탈해도 BillingKey.Issued 웹훅과
// issueId로 어떤 사용자가 발급을 시도했는지 복구할 수 있도록, SDK 호출 전에 미리 서버에 남겨두는 기록.
@Entity
@Table(name = "billing_key_issuance_intent")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class BillingKeyIssuanceIntent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "issue_id", nullable = false, unique = true)
    private String issueId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private BillingKeyIssuanceIntent(String issueId, User user) {
        this.issueId = issueId;
        this.user = user;
    }

    public static BillingKeyIssuanceIntent create(String issueId, User user) {
        return BillingKeyIssuanceIntent.builder()
                .issueId(issueId)
                .user(user)
                .build();
    }

    public boolean isCompleted() {
        return completedAt != null;
    }

    public void markCompleted(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }
}

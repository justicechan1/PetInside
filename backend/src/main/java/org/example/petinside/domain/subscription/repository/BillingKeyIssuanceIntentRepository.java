package org.example.petinside.domain.subscription.repository;

import org.example.petinside.domain.subscription.entity.BillingKeyIssuanceIntent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BillingKeyIssuanceIntentRepository extends JpaRepository<BillingKeyIssuanceIntent, Long> {
    // issueId(SDK 호출 시 발급한 고유값)로 발급 의도를 찾음
    Optional<BillingKeyIssuanceIntent> findByIssueId(String issueId);
}

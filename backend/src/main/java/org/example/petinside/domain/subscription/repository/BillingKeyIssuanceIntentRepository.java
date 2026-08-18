package org.example.petinside.domain.subscription.repository;

import org.example.petinside.domain.subscription.entity.BillingKeyIssuanceIntent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BillingKeyIssuanceIntentRepository extends JpaRepository<BillingKeyIssuanceIntent, Long> {
    Optional<BillingKeyIssuanceIntent> findByIssueId(String issueId);
}

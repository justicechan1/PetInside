package org.example.petinside.domain.subscription.repository;

import org.example.petinside.domain.subscription.entity.BillingKey;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BillingKeyRepository extends JpaRepository<BillingKey, Long> {
}

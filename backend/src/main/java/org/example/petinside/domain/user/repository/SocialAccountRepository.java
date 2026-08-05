package org.example.petinside.domain.user.repository;

import org.example.petinside.domain.user.entity.SocialAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SocialAccountRepository extends JpaRepository<SocialAccount, Long> {
    Optional<SocialAccount> findByProviderAndProviderId(String provider, String providerId);
    boolean existsByProviderAndProviderId(String provider, String providerId);
    Optional<SocialAccount> findByUserId(Long userId);
}
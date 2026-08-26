package org.example.petinside.domain.payment.repository;

import org.example.petinside.domain.payment.entity.Payment;
import org.example.petinside.domain.payment.entity.PaymentStatus;
import org.example.petinside.domain.subscription.entity.Subscription;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    // paymentId는 unique 제약이 걸려있어 멱등성의 핵심 키 역할을 함(같은 결제 재조회/재검증 시 사용)
    Optional<Payment> findByPaymentId(String paymentId);

    // subscription_id 컬럼만 직접 갱신하는 벌크 업데이트.
    // 엔티티를 로드해서 저장하면, REPEATABLE READ에서 이 트랜잭션이 verifyAndMarkPaid(REQUIRES_NEW)의
    // PAID 커밋 이전 스냅샷을 그대로 갖고 있다가 그 오래된 필드값 전체로 덮어써서
    // 방금 커밋된 PAID/paid_at을 되돌려버리는 문제가 있어(subscription_id만 콕 집어 갱신).
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Payment p SET p.subscription = :subscription WHERE p.paymentId = :paymentId")
    int linkSubscription(@Param("paymentId") String paymentId, @Param("subscription") Subscription subscription);

    Page<Payment> findByStatus(PaymentStatus status, Pageable pageable);
    Page<Payment> findByUserId(Long userId, Pageable pageable);

    List<Payment> findByUserIdOrderByCreatedAtDesc(Long userId);

    // 결제창을 열어놓고 이탈해서 영영 READY로 남는 시도를 자동 만료 처리할 때 사용
    List<Payment> findByStatusAndCreatedAtBefore(PaymentStatus status, LocalDateTime threshold);

    // 정기결제 다음 회차 번호 계산용
    Optional<Payment> findFirstBySubscriptionIdOrderByRoundDesc(Long subscriptionId);
}

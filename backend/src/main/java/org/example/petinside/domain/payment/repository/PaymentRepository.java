package org.example.petinside.domain.payment.repository;

import org.example.petinside.domain.payment.entity.Payment;
import org.example.petinside.domain.payment.entity.PaymentStatus;
import org.example.petinside.domain.subscription.entity.Subscription;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    // paymentId는 unique 제약이 걸려있어 멱등성의 핵심 키 역할을 함(같은 결제 재조회/재검증 시 사용)
    Optional<Payment> findByPaymentId(String paymentId);

    // 결제 완료 확정 시 사용 - 완료 API 경로(verifyAndMarkPaid)와 웹훅 경로가 같은 paymentId를
    // 동시에 확정하려는 경우(정기결제는 noticeUrls로 웹훅도 같이 받음)가 있어, 비관적 락으로
    // 한쪽이 끝날 때까지 다른 쪽을 대기시켜 동시 INSERT 경합(락 대기시간 초과)을 막는다(2026-08-26).
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Payment p WHERE p.paymentId = :paymentId")
    Optional<Payment> findByPaymentIdForUpdate(@Param("paymentId") String paymentId);

    // subscription_id 컬럼만 직접 갱신하는 벌크 업데이트.
    // 엔티티를 로드해서 저장하면, REPEATABLE READ에서 이 트랜잭션이 verifyAndMarkPaid(REQUIRES_NEW)의
    // PAID 커밋 이전 스냅샷을 그대로 갖고 있다가 그 오래된 필드값 전체로 덮어써서
    // 방금 커밋된 PAID/paid_at을 되돌려버리는 문제가 있어(subscription_id만 콕 집어 갱신).
    // clearAutomatically는 일부러 안 씀 - 영속성 컨텍스트 전체를 비우면 호출부가 들고 있던
    // subscription(의 billingKey 등 아직 초기화 안 된 연관 엔티티)까지 detach돼서
    // LazyInitializationException이 났었음(2026-08-26, chargeNextRound에서 재현).
    @Modifying
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

package org.example.petinside.domain.subscription.repository;

import org.example.petinside.domain.subscription.entity.Subscription;
import org.example.petinside.domain.subscription.entity.SubscriptionStatus;
import org.example.petinside.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    boolean existsByUserAndStatus(User user, SubscriptionStatus status);

    // 신규 구독 시작 차단용: ACTIVE든 PAST_DUE(유예기간)든 이미 유효한 구독을 갖고 있으면 중복 가입 불가.
    boolean existsByUserAndStatusIn(User user, Collection<SubscriptionStatus> statuses);

    // F-25: 구독 상태 조회용. 정기/단건 구분 없이 가장 최근 구독 1건.
    Optional<Subscription> findFirstByUserIdOrderByIdDesc(Long userId);

    // 해지 예약(canceledAt 존재)됐고 만료 시점(nextBillingAt)이 지난 구독 - 정기구독 해지분/단건 이용권 만료 공통 대상
    List<Subscription> findByStatusAndCanceledAtIsNotNullAndNextBillingAtBefore(SubscriptionStatus status, LocalDateTime now);

    // 해지되지 않았고(canceledAt null) 다음 결제일이 지난 ACTIVE 구독 - 자동 재청구(F-22) 대상.
    // 실패하면 status가 PAST_DUE로 바뀌므로 이 쿼리(status=ACTIVE)에서 자연히 빠짐 - 그 뒤로는
    // 사용자가 [다시 결제]를 눌러야만(retry-payment API) 재시도되고, 그래도 안 되면 유예기간 만료로 EXPIRED됨.
    // 1개월 이용권은 생성 시점부터 canceledAt이 채워져 있어 자동으로 제외됨.
    // 스케줄러에서 트랜잭션 밖으로 나간 뒤에도 user/billingKey를 안전하게 쓸 수 있도록 함께 즉시 로딩.
    @Query("SELECT s FROM Subscription s JOIN FETCH s.user JOIN FETCH s.billingKey " +
            "WHERE s.status = :status AND s.canceledAt IS NULL AND s.nextBillingAt < :now")
    List<Subscription> findDueForRecurringCharge(@Param("status") SubscriptionStatus status, @Param("now") LocalDateTime now);

    // 유예기간(payment_failed_at 이후 N일)이 지나도 결제 실패가 계속된(PAST_DUE) 정기구독 - 자동 만료 대상
    List<Subscription> findByStatusAndPaymentFailedAtBefore(SubscriptionStatus status, LocalDateTime threshold);

    // 해지/재개/재시도(F-23, F-22) 처리용. 트랜잭션 밖에서도 user를 안전하게 쓸 수 있도록 즉시 로딩.
    @Query("SELECT s FROM Subscription s JOIN FETCH s.user LEFT JOIN FETCH s.billingKey WHERE s.id = :id")
    Optional<Subscription> findByIdWithUser(@Param("id") Long id);

    // 구독 상태/가입일 필터링 용
    Page<Subscription> findByStatus(SubscriptionStatus status, Pageable pageable);
    Page<Subscription> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);
}

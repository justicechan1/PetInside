package org.example.petinside.domain.subscription.repository;

import org.example.petinside.domain.subscription.entity.Subscription;
import org.example.petinside.domain.subscription.entity.SubscriptionStatus;
import org.example.petinside.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    // 특정 유저가 특정 상태의 구독을 가지고 있는지 여부 확인
    boolean existsByUserAndStatus(User user, SubscriptionStatus status);

    // 인증 뱃지(F-XX) 단건 판정용: EmojiService와 동일하게 별도 플래그 없이 구독 상태를 실시간 조회.
    boolean existsByUser_IdAndStatus(Long userId, SubscriptionStatus status);

    // 인증 뱃지 배치 판정용(게시글/댓글 목록): 대상 유저 중 현재 ACTIVE 구독을 가진 유저 id만 반환.
    // 구독이 해지되어 EXPIRED로 넘어가거나 결제 실패로 PAST_DUE가 되면 이 집합에서 자연히 빠져
    // 뱃지가 즉시 회수됨(EmojiService의 F-28과 동일한 방식 - 별도 회수 로직 불필요).
    @Query("SELECT DISTINCT s.user.id FROM Subscription s WHERE s.user.id IN :userIds AND s.status = :status")
    Set<Long> findUserIdsByUserIdInAndStatus(@Param("userIds") Collection<Long> userIds, @Param("status") SubscriptionStatus status);

    // ACTIVE 상태 뿐만 아니라 결제 실패 유예기간(PAST_DUE) 상태인 경우에도 중복 구독 가입을 차단
    boolean existsByUserAndStatusIn(User user, Collection<SubscriptionStatus> statuses);

    // 정기/단건 구분 없이 해당 회원의 가장 최근 구독 내역 1건을 최신순으로 조회
    Optional<Subscription> findFirstByUserIdOrderByIdDesc(Long userId);

    // 해지 예약이 등록되어 있고, 다음 결제 예정일이 지난 구독을 조회
    List<Subscription> findByStatusAndCanceledAtIsNotNullAndNextBillingAtBefore(SubscriptionStatus status, LocalDateTime now);

    // 해지 신청이 없었고, 결제 주기가 도래한 ACTIVE 구독 목록을 조회
    @Query("SELECT s FROM Subscription s JOIN FETCH s.user JOIN FETCH s.billingKey " +
            "WHERE s.status = :status AND s.canceledAt IS NULL AND s.nextBillingAt < :now")
    List<Subscription> findDueForRecurringCharge(@Param("status") SubscriptionStatus status, @Param("now") LocalDateTime now);

    // 유예기간이 지나도 결제 실패가 계속된 정기구독 조회
    List<Subscription> findByStatusAndPaymentFailedAtBefore(SubscriptionStatus status, LocalDateTime threshold);

    // 구독 ID로 조회하며, 연관된 User 엔티티 및 BillingKey 엔티티(LEFT JOIN)를 Fetch Join
    @Query("SELECT s FROM Subscription s JOIN FETCH s.user LEFT JOIN FETCH s.billingKey WHERE s.id = :id")
    Optional<Subscription> findByIdWithUser(@Param("id") Long id);

    // 비관적 쓰기 락을 적용하여 조회 (이중결제 방지).
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Subscription s WHERE s.id = :id")
    Optional<Subscription> findByIdForUpdate(@Param("id") Long id);

    // 특정 상태의 구독 페이징 조회
    Page<Subscription> findByStatus(SubscriptionStatus status, Pageable pageable);
    // 특정 기간 내 가입된 구독 페이징 조회
    Page<Subscription> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);
}

package org.example.petinside.domain.subscription.service;

import lombok.RequiredArgsConstructor;
import org.example.petinside.domain.payment.service.PaymentService;
import org.example.petinside.domain.subscription.dto.SubscriptionCompleteResponse;
import org.example.petinside.domain.subscription.entity.BillingKey;
import org.example.petinside.domain.subscription.entity.Subscription;
import org.example.petinside.domain.subscription.entity.SubscriptionStatus;
import org.example.petinside.domain.subscription.repository.BillingKeyRepository;
import org.example.petinside.domain.subscription.repository.SubscriptionRepository;
import org.example.petinside.domain.user.entity.User;
import org.example.petinside.domain.user.repository.UserRepository;
import org.example.petinside.global.exception.CustomException;
import org.example.petinside.global.exception.UserNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;

// SubscriptionService.create()의 락/검증과 구독 생성을 별도 빈으로 분리.
@Component
@RequiredArgsConstructor
public class SubscriptionCreationSteps {

    static final Set<SubscriptionStatus> VALID_SUBSCRIPTION_STATUSES = EnumSet.of(SubscriptionStatus.ACTIVE, SubscriptionStatus.PAST_DUE);

    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final BillingKeyRepository billingKeyRepository;
    private final PaymentService paymentService;

    // 결제 진행 중인 선점 행도 "이미 구독 있음"으로 본다
    static final Set<SubscriptionStatus> BLOCKING_STATUSES =
            EnumSet.of(SubscriptionStatus.ACTIVE, SubscriptionStatus.PAST_DUE, SubscriptionStatus.PENDING);

    public record Reservation(Long subscriptionId, String billingKeyEncrypted) {
    }

    // 이미 활성 구독이 있는지 확인 + 빌링키 검증 후 PENDING 구독으로 자리를 선점.
    // 유저 row 락은 이 트랜잭션이 끝나면 풀리지만, 커밋된 PENDING 행이 남아 결제 중에 들어온 동시 요청을 막는다.
    @Transactional
    public Reservation reserveForNewSubscription(Long userId, Long billingKeyId) {
        // 유저 DB Row 비관적 락 조회 - 동시 구독 실행 방지
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        // 이미 이용 중이거나 결제 유예 중인 활성 구독 존재 여부 검증
        if (subscriptionRepository.existsByUserAndStatusIn(user, BLOCKING_STATUSES)) {
            throw new CustomException(HttpStatus.CONFLICT.value(), "이미 활성 구독이 존재합니다.");
        }

        // 빌링키 존재 및 본인 소유 여부 검증
        BillingKey billingKey = billingKeyRepository.findById(billingKeyId)
                .filter(bk -> bk.getUser().getId().equals(userId))
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND.value(), "빌링키를 찾을 수 없습니다."));

        // 빌링키 만료/삭제 여부 검증
        if (!billingKey.isActive()) {
            throw new CustomException(HttpStatus.CONFLICT.value(), "폐기된 빌링키입니다.");
        }
        Subscription pending = subscriptionRepository.save(Subscription.pending(user, billingKey, LocalDateTime.now()));
        return new Reservation(pending.getId(), billingKey.getBillingKeyEncrypted());
    }

    // 1회차 결제가 실패하면 선점 행을 지워 다시 시작할 수 있게 함
    @Transactional
    public void releasePending(Long subscriptionId) {
        subscriptionRepository.deleteById(subscriptionId);
    }

    // 결제가 확정된 뒤 구독 row를 생성하고 결제 기록에 연결.
    @Transactional
    public SubscriptionCompleteResponse activateSubscription(Long subscriptionId, String paymentId) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND.value(), "구독을 찾을 수 없습니다."));

        LocalDateTime now = LocalDateTime.now();
        // 구독 기간 설정: 결제일시부터 1개월 후 하루 전까지(예: 8/19 결제 → 다음 결제일 9/18)
        subscription.activatePending(now, now.plusMonths(1).minusDays(1));
        // 생성된 Payment 엔티티에 새로 생성된 Subscription 연관관계 연결
        paymentService.linkSubscription(paymentId, subscription);

        return new SubscriptionCompleteResponse(subscription.getId(), subscription.getStatus(), subscription.getNextBillingAt());
    }
}

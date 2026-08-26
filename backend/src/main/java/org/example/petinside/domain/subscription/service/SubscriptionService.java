package org.example.petinside.domain.subscription.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.petinside.domain.payment.dto.PaymentPrepareResponse;
import org.example.petinside.domain.payment.entity.Payment;
import org.example.petinside.domain.payment.service.PaymentService;
import org.example.petinside.domain.subscription.dto.SubscriptionCompleteResponse;
import org.example.petinside.domain.subscription.dto.SubscriptionCreateRequest;
import org.example.petinside.domain.subscription.dto.SubscriptionMeResponse;
import org.example.petinside.domain.subscription.entity.BillingKey;
import org.example.petinside.domain.subscription.entity.Subscription;
import org.example.petinside.domain.subscription.entity.SubscriptionStatus;
import org.example.petinside.domain.subscription.repository.SubscriptionRepository;
import org.example.petinside.domain.user.entity.User;
import org.example.petinside.domain.user.repository.UserRepository;
import org.example.petinside.global.exception.CustomException;
import org.example.petinside.global.exception.UserNotFoundException;
import org.example.petinside.global.portone.BillingKeyEncryptor;
import org.example.petinside.global.portone.PortOneClient;
import org.example.petinside.global.portone.PortOneProperties;
import org.example.petinside.global.portone.dto.PortOneBillingKeyPaymentRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

// 구독 생성/해지/재개/재시도 등 구독 도메인의 핵심 로직을 담당하는 서비스.
@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private static final String ORDER_NAME = "PetInside 구독 결제";
    // 이미 유효한 구독(정상이든 유예기간 중이든)이 있으면 새 구독을 또 시작할 수 없음.
    private static final Set<SubscriptionStatus> VALID_SUBSCRIPTION_STATUSES = EnumSet.of(SubscriptionStatus.ACTIVE, SubscriptionStatus.PAST_DUE);

    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PaymentService paymentService;
    private final PortOneClient portOneClient;
    private final PortOneProperties portOneProperties;
    private final BillingKeyEncryptor billingKeyEncryptor;
    private final SubscriptionCreationSteps creationSteps;

    // 빌링키 기반 1회차 결제 실행 및 정기구독 신규 생성
    public SubscriptionCompleteResponse create(Long userId, SubscriptionCreateRequest request) {
        // 유저 DB 락을 획득하여 동시 중복 신청 차단 및 빌링키 상태 검증
        BillingKey billingKey = creationSteps.reserveForNewSubscription(userId, request.billingKeyId());

        // READY 상태의 Payment/Order 엔티티 생성
        Payment payment = paymentService.createReadyPayment(userId);
        String rawBillingKey = billingKeyEncryptor.decrypt(billingKey.getBillingKeyEncrypted());

        // PortOne REST API를 통해 빌링키 승인 요청(HTTP 통신)
        portOneClient.payWithBillingKey(payment.getPaymentId(), new PortOneBillingKeyPaymentRequest(
                rawBillingKey,
                portOneProperties.storeId(),
                portOneProperties.channelKeySubscription(),
                ORDER_NAME,
                new PortOneBillingKeyPaymentRequest.Customer(userId.toString()),
                new PortOneBillingKeyPaymentRequest.Amount(payment.getAmount()),
                payment.getCurrency(),
                noticeUrls()
        ));

        // PortOne API 승인 상태 쟂회 및 PAID 확정 검증
        paymentService.verifyAndMarkPaid(userId, payment.getPaymentId());

        // 구독 엔티티 생성 및 결제 이력 연결
        return creationSteps.activateSubscription(userId, billingKey, payment.getPaymentId());
    }

    // PortOne, 요청 건별로 noticeUrls를 실어 보내 웹훅을 받음.
    private List<String> noticeUrls() {
        String webhookNoticeUrl = portOneProperties.webhookNoticeUrl();
        return webhookNoticeUrl == null || webhookNoticeUrl.isBlank() ? null : List.of(webhookNoticeUrl);
    }

    // 정기 구독 자동 재청구
    public void chargeNextRound(Subscription subscription) {
        try {
            // 결제 준비 엔티티 생성
            Payment payment = paymentService.createNextRoundPayment(subscription);
            // 구독 연결은 이 트랜잭션(이미 subscription row 락을 쥔 트랜잭션)에서 처리 -
            // createNextRoundPayment(REQUIRES_NEW)에서 바로 연결하면 자기 자신의 락과 충돌한다.
            paymentService.linkSubscription(payment.getPaymentId(), subscription);
            BillingKey billingKey = subscription.getBillingKey();
            Long userId = subscription.getUser().getId();
            String rawBillingKey = billingKeyEncryptor.decrypt(billingKey.getBillingKeyEncrypted());

            // PortOne  서버-to-서버 자동 결제 요청
            portOneClient.payWithBillingKey(payment.getPaymentId(), new PortOneBillingKeyPaymentRequest(
                    rawBillingKey,
                    portOneProperties.storeId(),
                    portOneProperties.channelKeySubscription(),
                    ORDER_NAME,
                    new PortOneBillingKeyPaymentRequest.Customer(userId.toString()),
                    new PortOneBillingKeyPaymentRequest.Amount(payment.getAmount()),
                    payment.getCurrency(),
                    noticeUrls()
            ));

            // 결제 승인 검증 및 성공 상태 업데이트
            Payment paid = paymentService.verifyAndMarkPaid(userId, payment.getPaymentId());
            // 구독 연장 처리
            subscription.chargeSucceeded(paid.getPaidAt());
            log.info("정기결제 재청구 성공(저장 전): subscriptionId={}, status={}, nextBillingAt={}, paymentFailedAt={}",
                    subscription.getId(), subscription.getStatus(), subscription.getNextBillingAt(), subscription.getPaymentFailedAt());
        } catch (RuntimeException e) {
            // 결제 실패(카드사 거절뿐 아니라 코드 내부 예외도 여기서 전부 삼켜지므로, 원인 진단을 위해 반드시 기록)
            log.error("정기결제 재청구 실패: subscriptionId={}", subscription.getId(), e);
            subscription.markPaymentFailed();
        } finally {
            // 구독 변경 상태 DB 반영
            Subscription saved = subscriptionRepository.save(subscription);
            log.info("정기결제 재청구 저장 완료: subscriptionId={}, status={}, nextBillingAt={}, paymentFailedAt={}",
                    saved.getId(), saved.getStatus(), saved.getNextBillingAt(), saved.getPaymentFailedAt());
        }
    }

    // 1개월 이용권 단건 구매 준비. (빌링키 X)
    @Transactional
    public PaymentPrepareResponse prepareOneTime(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        // 이미 활성구독이 있는 경우 구매 불가
        if (subscriptionRepository.existsByUserAndStatusIn(user, VALID_SUBSCRIPTION_STATUSES)) {
            throw new CustomException(HttpStatus.CONFLICT.value(), "이미 활성 구독이 존재합니다.");
        }

        return paymentService.prepare(userId);
    }

    // 단건결제 완료검증 후 1개월짜리(자동갱신 없는) 구독 생성
    @Transactional
    public SubscriptionCompleteResponse completeOneTime(Long userId, String paymentId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        // verifyAndMarkPaid는 REQUIRES_NEW라 반환된 payment는 이 트랜잭션 기준으로 detached 상태.
        paymentService.verifyAndMarkPaid(userId, paymentId);

        LocalDateTime now = LocalDateTime.now();
        // 이용기간: 오늘부터 1개월 후 하루 전까지(예: 8/19 결제 → 9/18 만료)
        Subscription subscription = Subscription.purchaseOneTime(user, now, now.plusMonths(1).minusDays(1));
        subscriptionRepository.save(subscription);
        // Detached 상태 문제를 방지하기 위해 paymentId로 조회후 이 트랜잭션 내 영속성 컨테스트에서 연관관계 연결
        paymentService.linkSubscription(paymentId, subscription);

        return new SubscriptionCompleteResponse(subscription.getId(), subscription.getStatus(), subscription.getNextBillingAt());
    }

    // 내 구독 상태 조회
    public SubscriptionMeResponse getMySubscription(Long userId) {
        return subscriptionRepository.findFirstByUserIdOrderByIdDesc(userId)
                .map(SubscriptionMeResponse::from)
                .orElseGet(SubscriptionMeResponse::none);
    }

    // 정기구독 해지 예약. 이미 승인된 회차는 그대로 두고 다음 결제만 막음.
    @Transactional
    public SubscriptionMeResponse cancel(Long userId, Long subscriptionId) {
        Subscription subscription = findMyRecurring(userId, subscriptionId, VALID_SUBSCRIPTION_STATUSES);

        // 결제 실패 유예 상태에서의 해지는 즉시 구독 종료
        if (subscription.getStatus() == SubscriptionStatus.PAST_DUE) {
            subscription.cancelDuringGracePeriod();
            return SubscriptionMeResponse.from(subscription);
        }

        if (subscription.getCanceledAt() != null) {
            throw new CustomException(HttpStatus.CONFLICT.value(), "이미 해지 예약된 구독입니다.");
        }

        subscription.cancel();
        return SubscriptionMeResponse.from(subscription);
    }

    // 해지 예약 취소(구독 재개)
    @Transactional
    public SubscriptionMeResponse resume(Long userId, Long subscriptionId) {
        Subscription subscription = findMyRecurring(userId, subscriptionId, EnumSet.of(SubscriptionStatus.ACTIVE));

        if (subscription.getCanceledAt() == null) {
            throw new CustomException(HttpStatus.CONFLICT.value(), "해지 예약 상태가 아닙니다.");
        }

        subscription.resume();
        return SubscriptionMeResponse.from(subscription);
    }

    // 결제 실패(PAST_DUE) 상태 구독의 수동 재결제 시도
    @Transactional
    public SubscriptionMeResponse retryPayment(Long userId, Long subscriptionId) {
        // 동시 재시도 차단
        Subscription subscription = findMyRecurringForUpdate(userId, subscriptionId, EnumSet.of(SubscriptionStatus.PAST_DUE));

        // 결제 재시도 실행
        chargeNextRound(subscription);
        return SubscriptionMeResponse.from(subscription);
    }

    // 내 정기구독 조회 (락 없음 - 해지/재개 등 단순 조회용)
    private Subscription findMyRecurring(Long userId, Long subscriptionId, Set<SubscriptionStatus> allowedStatuses) {
        Subscription subscription = subscriptionRepository.findByIdWithUser(subscriptionId)
                .filter(s -> s.getUser().getId().equals(userId))
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND.value(), "구독을 찾을 수 없습니다."));

        return validateRecurring(subscription, allowedStatuses);
    }

    // 내 정기 구독 조회(Pessimistic Lock 적용).
    // subscription row만 잠그므로, 이후 REQUIRES_NEW로 넘어가기 전에 user를 여기서 미리
    // (위 filter의 getUser() 호출로) 초기화해둔다 - 그래야 REQUIRES_NEW 트랜잭션에서
    // LazyInitializationException 없이 이미 로드된 값을 그대로 쓸 수 있다.
    private Subscription findMyRecurringForUpdate(Long userId, Long subscriptionId, Set<SubscriptionStatus> allowedStatuses) {
        Subscription subscription = subscriptionRepository.findByIdForUpdate(subscriptionId)
                .filter(s -> s.getUser().getId().equals(userId))
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND.value(), "구독을 찾을 수 없습니다."));

        return validateRecurring(subscription, allowedStatuses);
    }

    // 정기 구독 여부 및 상태 유효성 검증
    private Subscription validateRecurring(Subscription subscription, Set<SubscriptionStatus> allowedStatuses) {
        if (!allowedStatuses.contains(subscription.getStatus())) {
            throw new CustomException(HttpStatus.CONFLICT.value(), "지금 상태에서는 처리할 수 없습니다.");
        }

        if (!subscription.isRecurring()) {
            throw new CustomException(HttpStatus.CONFLICT.value(), "1개월 이용권은 해지/재개/재시도 대상이 아닙니다.");
        }

        return subscription;
    }
}

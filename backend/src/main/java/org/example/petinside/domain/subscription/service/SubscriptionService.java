package org.example.petinside.domain.subscription.service;

import lombok.RequiredArgsConstructor;
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

    // 이미 검증·저장된 빌링키(billingKeyId)로 1회차 결제를 실행하고 구독을 시작.
    // 빌링키 발급(카드 등록) 자체는 별도 API(BillingKeyController)에서 이미 끝난 상태.
    // 같은 유저가 거의 동시에 두 번 호출해도("이미 구독 있는지 확인 → 결제" 사이에 이중 결제가 끼어들지
    // 못하도록) 유저 row에 락을 걸어 트랜잭션이 끝날 때까지 뒤 요청이 대기하게 만든다.
    //
    // 이 메서드 자체는 일부러 @Transactional을 걸지 않는다. 카드 결제(payWithBillingKey) 자체는
    // 성공했는데 그 뒤 검증(verifyAndMarkPaid)이 실패하는 경우, 메서드 전체가 하나의 트랜잭션이면
    // 그 실패로 인해 방금 만든 Payment/Order 기록까지 통째로 롤백되어 사라진다 — 실제로는 카드가
    // 결제됐는데 DB엔 아무 흔적도 안 남는 셈. reserveForNewSubscription/activateSubscription과
    // paymentService의 각 단계(REQUIRES_NEW)가 각자 독립적으로 커밋되게 해서 이를 방지한다
    // (chargeNextRound()와 동일한 이유로 동일한 패턴을 적용).
    public SubscriptionCompleteResponse create(Long userId, SubscriptionCreateRequest request) {
        BillingKey billingKey = creationSteps.reserveForNewSubscription(userId, request.billingKeyId());

        Payment payment = paymentService.createReadyPayment(userId);
        String rawBillingKey = billingKeyEncryptor.decrypt(billingKey.getBillingKeyEncrypted());

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

        // 빌링키 결제 요청의 즉시 응답은 카드사 승인이 최종 확정(PAID)되기 전 중간 상태일 수 있어 신뢰하지 않음.
        // 검증에 실패하면 여기서 예외가 던져지지만, 그 실패 기록(FAILED)은 이미 별도 트랜잭션으로 커밋된 뒤다.
        paymentService.verifyAndMarkPaid(userId, payment.getPaymentId());

        return creationSteps.activateSubscription(userId, billingKey, payment.getPaymentId());
    }

    // PortOne, 요청 건별로 noticeUrls를 실어 보내 웹훅을 받음.
    private List<String> noticeUrls() {
        String webhookNoticeUrl = portOneProperties.webhookNoticeUrl();
        return webhookNoticeUrl == null || webhookNoticeUrl.isBlank() ? null : List.of(webhookNoticeUrl);
    }

    // F-22: 정기결제 자동 재청구 한 건. 스케줄러(SubscriptionBillingScheduler)가 매일 대상 구독마다 호출.
    // 실패해도 예외를 던지지 않고 payment_failed_at만 기록 — 한 건의 실패가 배치 전체를 막지 않게 함.
    // 일부러 이 메서드 자체는 @Transactional을 걸지 않음: createNextRoundPayment/verifyAndMarkPaid가
    // 각자 독립된 트랜잭션으로 커밋되게 해서, 결제 실패로 인한 롤백이 이 메서드의 성공/실패 기록까지
    // 함께 롤백시키지 않도록 함(같은 트랜잭션에 묶이면 verifyAndMarkPaid의 실패가 전체를 롤백시킴).
    // createNextRoundPayment/decrypt도 try 안으로 옮김: 여기서 예외(예: 빌링키 복호화 키 설정 오류)가
    // 나도 markPaymentFailed()가 호출되게 해서, 스케줄러가 매번 같은 이유로 무한 재시도만 하고
    // PAST_DUE로도 못 넘어가는 상황을 막는다.
    public void chargeNextRound(Subscription subscription) {
        try {
            Payment payment = paymentService.createNextRoundPayment(subscription);
            BillingKey billingKey = subscription.getBillingKey();
            Long userId = subscription.getUser().getId();
            String rawBillingKey = billingKeyEncryptor.decrypt(billingKey.getBillingKeyEncrypted());

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

            Payment paid = paymentService.verifyAndMarkPaid(userId, payment.getPaymentId());
            subscription.chargeSucceeded(paid.getPaidAt());
        } catch (RuntimeException e) {
            subscription.markPaymentFailed();
        } finally {
            subscriptionRepository.save(subscription);
        }
    }

    // 1개월 이용권 단건 구매 준비. 빌링키 없이 일반결제 채널로 카드결제창을 바로 염.
    @Transactional
    public PaymentPrepareResponse prepareOneTime(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

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

        // verifyAndMarkPaid는 REQUIRES_NEW라 반환된 payment는 이 트랜잭션 기준으로 detached 상태다.
        // linkSubscription은 paymentId로 다시 조회해서 이 트랜잭션의 영속성 컨텍스트 안에서 연결한다.
        paymentService.verifyAndMarkPaid(userId, paymentId);

        LocalDateTime now = LocalDateTime.now();
        // 결제일 기준 한 달 뒤가 아니라 그 하루 전까지가 이용 기간(예: 8/19 결제 → 9/18 만료)
        Subscription subscription = Subscription.purchaseOneTime(user, now, now.plusMonths(1).minusDays(1));
        subscriptionRepository.save(subscription);
        paymentService.linkSubscription(paymentId, subscription);

        return new SubscriptionCompleteResponse(subscription.getId(), subscription.getStatus(), subscription.getNextBillingAt());
    }

    // F-25: 내 구독 상태 조회. 정기/단건 구분 없이 가장 최근 구독 1건 기준.
    public SubscriptionMeResponse getMySubscription(Long userId) {
        return subscriptionRepository.findFirstByUserIdOrderByIdDesc(userId)
                .map(SubscriptionMeResponse::from)
                .orElseGet(SubscriptionMeResponse::none);
    }

    // F-23: 해지 예약. 이미 승인된 회차는 그대로 두고 다음 결제만 막음.
    // 유예기간(PAST_DUE) 중이면 더 기다릴 이유가 없으므로 즉시 만료시킴(cancelDuringGracePeriod).
    @Transactional
    public SubscriptionMeResponse cancel(Long userId, Long subscriptionId) {
        Subscription subscription = findMyRecurring(userId, subscriptionId, VALID_SUBSCRIPTION_STATUSES);

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

    // F-23: 해지 예약 취소(재개). 아직 만료 전(ACTIVE)인 동안만 가능.
    @Transactional
    public SubscriptionMeResponse resume(Long userId, Long subscriptionId) {
        Subscription subscription = findMyRecurring(userId, subscriptionId, EnumSet.of(SubscriptionStatus.ACTIVE));

        if (subscription.getCanceledAt() == null) {
            throw new CustomException(HttpStatus.CONFLICT.value(), "해지 예약 상태가 아닙니다.");
        }

        subscription.resume();
        return SubscriptionMeResponse.from(subscription);
    }

    // F-22: 결제 실패 배너의 [다시 결제]. 유예기간(PAST_DUE) 중인 구독만 대상 - 실패한 구독은
    // 자동 배치가 더 이상 건드리지 않으므로, 이게 재시도할 수 있는 유일한 경로.
    //
    // 구독 row에 락을 걸어 조회한다(findByIdForUpdateWithUser) - create()가 유저 row를 잠그는 것과
    // 같은 이유. 락 없이는 사용자가 [다시 결제]를 더블클릭하거나 요청이 재시도됐을 때 두 요청 모두
    // status==PAST_DUE 체크를 통과해서 같은 카드로 두 번 청구될 수 있다. 이 메서드가 트랜잭션을 쥔 채
    // chargeNextRound(PortOne 호출 포함)까지 끝나야 락이 풀리므로, 뒤 요청은 그동안 대기했다가
    // 갱신된 상태(PAST_DUE가 아님)를 보고 CONFLICT로 걸러진다.
    @Transactional
    public SubscriptionMeResponse retryPayment(Long userId, Long subscriptionId) {
        Subscription subscription = findMyRecurringForUpdate(userId, subscriptionId, EnumSet.of(SubscriptionStatus.PAST_DUE));

        chargeNextRound(subscription);
        return SubscriptionMeResponse.from(subscription);
    }

    private Subscription findMyRecurring(Long userId, Long subscriptionId, Set<SubscriptionStatus> allowedStatuses) {
        Subscription subscription = subscriptionRepository.findByIdWithUser(subscriptionId)
                .filter(s -> s.getUser().getId().equals(userId))
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND.value(), "구독을 찾을 수 없습니다."));

        return validateRecurring(subscription, allowedStatuses);
    }

    private Subscription findMyRecurringForUpdate(Long userId, Long subscriptionId, Set<SubscriptionStatus> allowedStatuses) {
        Subscription subscription = subscriptionRepository.findByIdForUpdateWithUser(subscriptionId)
                .filter(s -> s.getUser().getId().equals(userId))
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND.value(), "구독을 찾을 수 없습니다."));

        return validateRecurring(subscription, allowedStatuses);
    }

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

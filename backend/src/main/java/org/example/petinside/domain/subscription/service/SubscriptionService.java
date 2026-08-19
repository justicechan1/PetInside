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
import org.example.petinside.domain.subscription.repository.BillingKeyRepository;
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

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private static final String ORDER_NAME = "PetInside 구독 결제";

    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final BillingKeyRepository billingKeyRepository;
    private final PaymentService paymentService;
    private final PortOneClient portOneClient;
    private final PortOneProperties portOneProperties;
    private final BillingKeyEncryptor billingKeyEncryptor;

    // 이미 검증·저장된 빌링키(billingKeyId)로 1회차 결제를 실행하고 구독을 시작.
    // 빌링키 발급(카드 등록) 자체는 별도 API(BillingKeyController)에서 이미 끝난 상태.
    @Transactional
    public SubscriptionCompleteResponse create(Long userId, SubscriptionCreateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        if (subscriptionRepository.existsByUserAndStatus(user, SubscriptionStatus.ACTIVE)) {
            throw new CustomException(HttpStatus.CONFLICT.value(), "이미 활성 구독이 존재합니다.");
        }

        BillingKey billingKey = billingKeyRepository.findById(request.billingKeyId())
                .filter(bk -> bk.getUser().getId().equals(userId))
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND.value(), "빌링키를 찾을 수 없습니다."));

        if (!billingKey.isActive()) {
            throw new CustomException(HttpStatus.CONFLICT.value(), "폐기된 빌링키입니다.");
        }

        Payment payment = paymentService.createReadyPayment(userId);
        String rawBillingKey = billingKeyEncryptor.decrypt(billingKey.getBillingKeyEncrypted());

        portOneClient.payWithBillingKey(payment.getPaymentId(), new PortOneBillingKeyPaymentRequest(
                rawBillingKey,
                portOneProperties.storeId(),
                portOneProperties.channelKeySubscription(),
                ORDER_NAME,
                new PortOneBillingKeyPaymentRequest.Customer(userId.toString()),
                new PortOneBillingKeyPaymentRequest.Amount(payment.getAmount()),
                payment.getCurrency()
        ));

        // 빌링키 결제 요청의 즉시 응답은 카드사 승인이 최종 확정(PAID)되기 전 중간 상태일 수 있어 신뢰하지 않음.
        paymentService.verifyAndMarkPaid(userId, payment.getPaymentId());

        LocalDateTime now = LocalDateTime.now();
        // 결제일 기준 한 달 뒤가 아니라 그 하루 전까지가 이용 기간(예: 8/19 결제 → 다음 결제일 9/18)
        Subscription subscription = Subscription.activate(user, billingKey, now, now.plusMonths(1).minusDays(1));
        subscriptionRepository.save(subscription);
        payment.linkSubscription(subscription);

        return new SubscriptionCompleteResponse(subscription.getId(), subscription.getStatus(), subscription.getNextBillingAt());
    }

    // 1개월 이용권 단건 구매 준비. 빌링키 없이 일반결제 채널로 카드결제창을 바로 염.
    @Transactional
    public PaymentPrepareResponse prepareOneTime(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        if (subscriptionRepository.existsByUserAndStatus(user, SubscriptionStatus.ACTIVE)) {
            throw new CustomException(HttpStatus.CONFLICT.value(), "이미 활성 구독이 존재합니다.");
        }

        return paymentService.prepare(userId);
    }

    // 단건결제 완료검증 후 1개월짜리(자동갱신 없는) 구독 생성
    @Transactional
    public SubscriptionCompleteResponse completeOneTime(Long userId, String paymentId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        Payment payment = paymentService.verifyAndMarkPaid(userId, paymentId);

        LocalDateTime now = LocalDateTime.now();
        // 결제일 기준 한 달 뒤가 아니라 그 하루 전까지가 이용 기간(예: 8/19 결제 → 9/18 만료)
        Subscription subscription = Subscription.purchaseOneTime(user, now, now.plusMonths(1).minusDays(1));
        subscriptionRepository.save(subscription);
        payment.linkSubscription(subscription);

        return new SubscriptionCompleteResponse(subscription.getId(), subscription.getStatus(), subscription.getNextBillingAt());
    }

    // F-25: 내 구독 상태 조회. 정기/단건 구분 없이 가장 최근 구독 1건 기준.
    public SubscriptionMeResponse getMySubscription(Long userId) {
        return subscriptionRepository.findFirstByUserIdOrderByIdDesc(userId)
                .map(SubscriptionMeResponse::from)
                .orElseGet(SubscriptionMeResponse::none);
    }

    // F-23: 정기결제 해지 예약. 이미 승인된 회차는 그대로 두고 다음 결제만 막음.
    @Transactional
    public SubscriptionMeResponse cancel(Long userId, Long subscriptionId) {
        Subscription subscription = findMyActiveRecurring(userId, subscriptionId);

        if (subscription.getCanceledAt() != null) {
            throw new CustomException(HttpStatus.CONFLICT.value(), "이미 해지 예약된 구독입니다.");
        }

        subscription.cancel();
        return SubscriptionMeResponse.from(subscription);
    }

    // F-23: 해지 예약 취소(재개). 아직 만료 전(ACTIVE)인 동안만 가능.
    @Transactional
    public SubscriptionMeResponse resume(Long userId, Long subscriptionId) {
        Subscription subscription = findMyActiveRecurring(userId, subscriptionId);

        if (subscription.getCanceledAt() == null) {
            throw new CustomException(HttpStatus.CONFLICT.value(), "해지 예약 상태가 아닙니다.");
        }

        subscription.resume();
        return SubscriptionMeResponse.from(subscription);
    }

    private Subscription findMyActiveRecurring(Long userId, Long subscriptionId) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .filter(s -> s.getUser().getId().equals(userId))
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND.value(), "구독을 찾을 수 없습니다."));

        if (subscription.getStatus() != SubscriptionStatus.ACTIVE) {
            throw new CustomException(HttpStatus.CONFLICT.value(), "이미 종료된 구독입니다.");
        }

        if (!subscription.isRecurring()) {
            throw new CustomException(HttpStatus.CONFLICT.value(), "1개월 이용권은 해지/재개 대상이 아닙니다.");
        }

        return subscription;
    }
}

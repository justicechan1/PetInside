package org.example.petinside.domain.subscription.service;

import lombok.RequiredArgsConstructor;
import org.example.petinside.domain.payment.entity.Payment;
import org.example.petinside.domain.payment.service.PaymentService;
import org.example.petinside.domain.subscription.dto.SubscriptionCompleteRequest;
import org.example.petinside.domain.subscription.dto.SubscriptionCompleteResponse;
import org.example.petinside.domain.subscription.dto.SubscriptionPrepareResponse;
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
import org.example.petinside.global.portone.dto.PortOneBillingKeyDetail;
import org.example.petinside.global.portone.dto.PortOneBillingKeyPaymentRequest;
import org.example.petinside.global.portone.dto.PortOnePaymentDetail;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private static final String ISSUED_STATUS = "ISSUED";
    private static final String ORDER_NAME = "PetInside 구독 결제";

    private final PaymentService paymentService;
    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final BillingKeyRepository billingKeyRepository;
    private final PortOneClient portOneClient;
    private final PortOneProperties portOneProperties;
    private final BillingKeyEncryptor billingKeyEncryptor;

    @Transactional
    public SubscriptionPrepareResponse prepare(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        if (subscriptionRepository.existsByUserAndStatus(user, SubscriptionStatus.ACTIVE)) {
            throw new CustomException(HttpStatus.CONFLICT.value(), "이미 활성 구독이 존재합니다.");
        }

        Payment payment = paymentService.createReadyPayment(userId);
        return new SubscriptionPrepareResponse(payment.getPaymentId(), portOneProperties.storeId(),
                portOneProperties.channelKeySubscription(), payment.getAmount(), payment.getCurrency());
    }

    // 프론트는 requestIssueBillingKey로 빌링키만 발급받아 전달한다.
    // 1회차 결제는 프론트 결제창이 아니라, 이 서버가 발급된 빌링키로 PortOne에 직접 결제를 요청해서 실행한다.
    @Transactional
    public SubscriptionCompleteResponse complete(Long userId, SubscriptionCompleteRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        Payment payment = paymentService.findReadyPayment(userId, request.paymentId());

        PortOneBillingKeyDetail billingKeyDetail = portOneClient.getBillingKeyDetail(request.billingKey());
        boolean billingKeyVerified = ISSUED_STATUS.equalsIgnoreCase(billingKeyDetail.status())
                && portOneProperties.storeId().equals(billingKeyDetail.storeId());

        if (!billingKeyVerified) {
            throw new CustomException(HttpStatus.UNPROCESSABLE_ENTITY.value(), "PortOne 빌링키 정보가 유효하지 않습니다.");
        }

        // requestIssueBillingKeyAndPay(휴대폰 인증)처럼 프론트 SDK 단계에서 이미 1회차 결제까지 끝났을 수 있다.
        // 그 경우 PortOne이 이 paymentId를 이미 알고 있으므로, 없을 때만 서버가 직접 빌링키 결제를 요청한다.
        PortOnePaymentDetail chargeResult = portOneClient.findPaymentDetail(payment.getPaymentId())
                .orElseGet(() -> portOneClient.payWithBillingKey(payment.getPaymentId(), new PortOneBillingKeyPaymentRequest(
                        request.billingKey(),
                        portOneProperties.storeId(),
                        portOneProperties.channelKeySubscription(),
                        ORDER_NAME,
                        new PortOneBillingKeyPaymentRequest.Customer(userId.toString()),
                        new PortOneBillingKeyPaymentRequest.Amount(payment.getAmount()),
                        payment.getCurrency()
                )));

        // 어느 경로든 완료 API/웹훅과 같은 검증 로직(PaymentService.finalizeByDetail)을 그대로 탄다.
        paymentService.finalizeByDetail(payment, chargeResult);

        LocalDateTime now = LocalDateTime.now();
        Subscription subscription = Subscription.activate(user, now, now.plusMonths(1));
        subscriptionRepository.save(subscription);

        String encryptedBillingKey = billingKeyEncryptor.encrypt(request.billingKey());
        billingKeyRepository.save(BillingKey.issue(subscription, encryptedBillingKey, now));

        payment.linkSubscription(subscription);

        return new SubscriptionCompleteResponse(subscription.getId(), subscription.getStatus(), subscription.getNextBillingAt());
    }
}

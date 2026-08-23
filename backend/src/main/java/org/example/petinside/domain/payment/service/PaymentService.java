package org.example.petinside.domain.payment.service;

import lombok.RequiredArgsConstructor;
import org.example.petinside.domain.payment.dto.PaymentCompleteResponse;
import org.example.petinside.domain.payment.dto.PaymentHistoryResponse;
import org.example.petinside.domain.payment.dto.PaymentPrepareResponse;
import org.example.petinside.domain.payment.entity.Order;
import org.example.petinside.domain.payment.entity.Payment;
import org.example.petinside.domain.payment.entity.PaymentStatus;
import org.example.petinside.domain.payment.entity.PaymentTransaction;
import org.example.petinside.domain.payment.repository.OrderRepository;
import org.example.petinside.domain.payment.repository.PaymentRepository;
import org.example.petinside.domain.payment.repository.PaymentTransactionRepository;
import org.example.petinside.domain.subscription.entity.Subscription;
import org.example.petinside.domain.user.entity.User;
import org.example.petinside.domain.user.repository.UserRepository;
import org.example.petinside.global.exception.CustomException;
import org.example.petinside.global.exception.UserNotFoundException;
import org.example.petinside.global.portone.PortOneClient;
import org.example.petinside.global.portone.PortOneProperties;
import org.example.petinside.global.portone.dto.PortOnePaymentDetail;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    // 요금제는 아직 고정 요금 하나뿐 (구독 요금제 다양화는 범위 밖). 정기구독/1개월 이용권 공통.
    static final int PLAN_AMOUNT = 1900;
    static final String CURRENCY = "KRW";
    private static final String PAID_STATUS = "PAID";
    private static final String TEST_CHANNEL_TYPE = "TEST";

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final UserRepository userRepository;
    private final PortOneClient portOneClient;
    private final PortOneProperties portOneProperties;
    private final PaymentFailureRecorder paymentFailureRecorder;

    @Transactional
    public PaymentPrepareResponse prepare(Long userId) {
        Payment payment = createReadyPayment(userId);
        return new PaymentPrepareResponse(payment.getPaymentId(), portOneProperties.storeId(), portOneProperties.channelKey(), PLAN_AMOUNT, CURRENCY);
    }

    @Transactional
    public PaymentCompleteResponse complete(Long userId, String paymentId) {
        Payment payment = verifyAndMarkPaid(userId, paymentId);
        return new PaymentCompleteResponse(payment.getPaymentId(), payment.getStatus(), payment.getAmount(), payment.getCurrency(), payment.getPaidAt());
    }

    // F-24: 결제 내역 조회. 최신순. 이탈/미완료로 영영 READY로 남은 시도는 노출하지 않음.
    public List<PaymentHistoryResponse> getHistory(Long userId) {
        return paymentRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .filter(payment -> !payment.isReady())
                .map(PaymentHistoryResponse::from)
                .toList();
    }

    // 구독 준비(F-21)에서도 재사용: 결제 준비 자체는 빌링키 유무와 무관하게 동일.
    // 주문(Order)과 결제 시도(Payment)를 분리해서, 같은 주문에 결제 재시도가 여러 번 있었던 이력을 남길 수 있게 함.
    // REQUIRES_NEW: 호출부(SubscriptionService.create() 등)가 이후 PortOne 검증 단계에서 실패해도
    // "결제를 시도했다"는 기록 자체는 남아있어야 하므로, 호출부의 트랜잭션과 별개로 즉시 커밋한다.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Payment createReadyPayment(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        Order order = orderRepository.save(Order.ready(user, PLAN_AMOUNT, CURRENCY));

        //파라미터 제한(1~40자)에 맞춰 UUID는 하이픈 없이 사용
        String paymentId = portOneProperties.paymentIdPrefix() + "-SUB-" + UUID.randomUUID().toString().replace("-", "");
        Payment payment = Payment.createReady(user, order, paymentId, 1, PLAN_AMOUNT, CURRENCY);
        return paymentRepository.save(payment);
    }

    // F-22: 정기결제 다음 회차 결제 준비. 이미 존재하는 구독에 대한 재청구라 생성 시점에 바로 연결.
    // REQUIRES_NEW: SubscriptionService.retryPayment()가 구독 row에 락을 건 트랜잭션 안에서 이 메서드를
    // 호출해도, 결제 시도 기록은 그 트랜잭션의 성패와 무관하게 독립적으로 커밋되게 함.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Payment createNextRoundPayment(Subscription subscription) {
        User user = subscription.getUser();
        int nextRound = paymentRepository.findFirstBySubscriptionIdOrderByRoundDesc(subscription.getId())
                .map(Payment::getRound)
                .orElse(0) + 1;

        Order order = orderRepository.save(Order.ready(user, PLAN_AMOUNT, CURRENCY));
        String paymentId = portOneProperties.paymentIdPrefix() + "-SUB-" + UUID.randomUUID().toString().replace("-", "");
        Payment payment = Payment.createReady(user, order, paymentId, nextRound, PLAN_AMOUNT, CURRENCY);
        payment.linkSubscription(subscription);
        return paymentRepository.save(payment);
    }

    // 구독 완료검증(F-21)에서도 재사용: 이 유저의 READY 결제인지 확인
    @Transactional
    public Payment findReadyPayment(Long userId, String paymentId) {
        Payment payment = paymentRepository.findByPaymentId(paymentId)
                .filter(p -> p.getUser().getId().equals(userId))
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND.value(), "결제 준비 내역을 찾을 수 없습니다."));

        if (!payment.isReady()) {
            throw new CustomException(HttpStatus.CONFLICT.value(), "이미 처리된 결제입니다.");
        }
        return payment;
    }

    // 빌링키 없는 결제(단건조회 검증)용. 프론트가 결제창을 직접 호출했을 때 PortOne 재조회로 최종 확정.
    // REQUIRES_NEW: 호출부(SubscriptionService.create()/chargeNextRound() 등)의 트랜잭션이 이후 단계에서
    // 실패해도, 실제 카드 승인이 끝난 결과(PAID든 FAILED든)는 그와 무관하게 즉시 커밋되어 남아야 한다.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Payment verifyAndMarkPaid(Long userId, String paymentId) {
        Payment payment = findReadyPayment(userId, paymentId);
        PortOnePaymentDetail detail = portOneClient.getPaymentDetail(paymentId);
        finalizeByDetail(payment, detail);
        return payment;
    }

    // 완료 API(F-21) 웹훅이든 같은 동기화 로직을 타야 한다는 원칙에 따라 검증 기준을 한 곳에 둠.
    public void finalizeByDetail(Payment payment, PortOnePaymentDetail detail) {
        boolean verified = PAID_STATUS.equalsIgnoreCase(detail.status())
                && detail.amount() != null && detail.amount().total() == payment.getAmount()
                && CURRENCY.equalsIgnoreCase(detail.currency())
                && portOneProperties.storeId().equals(detail.storeId())
                && isOurChannel(detail.channel())
                && isTestChannel(detail.channel());

        if (!verified) {
            // 이 메서드를 호출한 쪽(예: PaymentWebhookService.handle())의 트랜잭션이 뒤이어 던지는
            // 예외로 롤백되더라도, "검증에 실패했다"는 기록만은 별도 트랜잭션으로 즉시 커밋해 남긴다.
            // 그렇지 않으면 FAILED 마킹이 통째로 사라지고 결제/주문이 영영 READY로 남는다.
            paymentFailureRecorder.recordVerificationFailure(payment.getId(), detail.transactionId());
            throw new CustomException(HttpStatus.UNPROCESSABLE_ENTITY.value(), "PortOne 결제 정보가 주문과 일치하지 않습니다.");
        }

        paymentTransactionRepository.save(PaymentTransaction.record(payment, detail.transactionId(), PaymentStatus.PAID));
        payment.markPaid(LocalDateTime.now());
        payment.getOrder().markCompleted();
    }

    // create()처럼 구독이 결제 시점엔 아직 없어 나중에 연결해야 하는 경우용.
    // payment가 (REQUIRES_NEW로) 이미 다른 트랜잭션에서 커밋된 detached 상태일 수 있어 id로 다시 조회한다.
    @Transactional
    public void linkSubscription(String paymentId, Subscription subscription) {
        Payment payment = paymentRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND.value(), "결제 내역을 찾을 수 없습니다."));
        payment.linkSubscription(subscription);
    }

    // 테스트 상점은 여러 팀이 공용으로 써서, storeId만으로는 다른 팀 채널로 발생한 결제를 걸러내지 못함.
    // 단건결제 채널(channelKey)과 정기결제 채널(channelKeySubscription) 둘 중 하나와 일치해야 우리 결제로 인정.
    private boolean isOurChannel(PortOnePaymentDetail.Channel channel) {
        if (channel == null || channel.key() == null) {
            return false;
        }
        return channel.key().equals(portOneProperties.channelKey())
                || channel.key().equals(portOneProperties.channelKeySubscription());
    }

    // 실수로 실결제가 발생하지 않도록, 테스트 채널로 발생한 결제만 인정.
    private boolean isTestChannel(PortOnePaymentDetail.Channel channel) {
        return channel != null && TEST_CHANNEL_TYPE.equalsIgnoreCase(channel.type());
    }
}

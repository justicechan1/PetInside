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

// 결제(단건/구독 공통) 준비·검증·조회를 담당하는 서비스.
@Service
@RequiredArgsConstructor
public class PaymentService {

    // 요금제는 아직 고정 요금 하나. 정기구독/1개월 이용권 공통.
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

    // 빌링키 없는 단건결제
    // 1단계: 결제 시도를 READY로 만들고 프론트가 결제창을 띄우는 데 필요한 정보(paymentId, storeId, channelKey, 금액)를 줌.
    @Transactional
    public PaymentPrepareResponse prepare(Long userId) {
        Payment payment = createReadyPayment(userId);
        return new PaymentPrepareResponse(payment.getPaymentId(), portOneProperties.storeId(), portOneProperties.channelKey(), PLAN_AMOUNT, CURRENCY);
    }

    // 단건결제 2단계: 프론트가 결제창에서 결제를 마쳤다고 알려오면, PortOne을 재조회해서 실제로 확정됐는지 검증하고 결과를 응답으로 내려줌.
    @Transactional
    public PaymentCompleteResponse complete(Long userId, String paymentId) {
        Payment payment = verifyAndMarkPaid(userId, paymentId);
        return new PaymentCompleteResponse(payment.getPaymentId(), payment.getStatus(), payment.getAmount(), payment.getCurrency(), payment.getPaidAt());
    }

    // 사용자의 결제 내역 목록 조회 (무효 결제 시도는 제외)
    public List<PaymentHistoryResponse> getHistory(Long userId) {
        return paymentRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .filter(payment -> !payment.isReady())
                .map(PaymentHistoryResponse::from)
                .toList();
    }

    // 최초 결제 시도(Order,Payment) 준비 레코드 생성
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Payment createReadyPayment(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        // 주문 객체 생성
        Order order = orderRepository.save(Order.ready(user, PLAN_AMOUNT, CURRENCY));

        // PortOne 결제 ID 규격에 맞춘 고유 paymentId 생성 (파라미터 40자 제한 준수)
        String paymentId = portOneProperties.paymentIdPrefix() + "-SUB-" + UUID.randomUUID().toString().replace("-", "");
        // 회차 명시 및 payment 엔티티 저장
        Payment payment = Payment.createReady(user, order, paymentId, 1, PLAN_AMOUNT, CURRENCY);
        return paymentRepository.save(payment);
    }

    // 정기 결제(구독) 다음 회차 결제 준비 레코드 생성
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Payment createNextRoundPayment(Subscription subscription) {
        User user = subscription.getUser();
        // 이전 최고 회차를 조회하여 다음 회차 번호 계산
        int nextRound = paymentRepository.findFirstBySubscriptionIdOrderByRoundDesc(subscription.getId())
                .map(Payment::getRound)
                .orElse(0) + 1;

        Order order = orderRepository.save(Order.ready(user, PLAN_AMOUNT, CURRENCY));
        String paymentId = portOneProperties.paymentIdPrefix() + "-SUB-" + UUID.randomUUID().toString().replace("-", "");
        Payment payment = Payment.createReady(user, order, paymentId, nextRound, PLAN_AMOUNT, CURRENCY);
        payment.linkSubscription(subscription); // 생성 시점에 이미 존재하는 구독 객체 연동
        return paymentRepository.save(payment);
    }

    // 검증 대상이 될 READY 상태의 결제건 조회 및 본인 확인
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

    // PortOne 결제 결과 조회 및 최종 확정 처리
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Payment verifyAndMarkPaid(Long userId, String paymentId) {
        Payment payment = findReadyPayment(userId, paymentId);
        // PortOne API 호출하여 실제 PG 결제 내역 조회
        PortOnePaymentDetail detail = portOneClient.getPaymentDetail(paymentId);
        // 대조 및 상태 마킹
        finalizeByDetail(payment, detail);
        return payment;
    }

    // PortOne 결제 데이터와 DB 주문 데이터 교차 대조 (무결성)
    public void finalizeByDetail(Payment payment, PortOnePaymentDetail detail) {
        // 위변조 검증
        boolean verified = PAID_STATUS.equalsIgnoreCase(detail.status())  // 상태
                && detail.amount() != null && detail.amount().total() == payment.getAmount()  // 결제금액
                && CURRENCY.equalsIgnoreCase(detail.currency())  // 통화
                && portOneProperties.storeId().equals(detail.storeId())  // 상점ID
                && isOurChannel(detail.channel())  // 채널 일치 여부
                && (!portOneProperties.requireTestChannel() || isTestChannel(detail.channel()));  // 로컬/개발 전용 테스트 채널 강제

        // 검증 실패 시 예외 처리 및 실패 기록 독립 기록
        if (!verified) {
            paymentFailureRecorder.recordVerificationFailure(payment.getId(), detail.transactionId());
            throw new CustomException(HttpStatus.UNPROCESSABLE_ENTITY.value(), "PortOne 결제 정보가 주문과 일치하지 않습니다.");
        }

        // 검증 성공 시 결제 트랜잭션 승인 기록 생성 및 Payment/Order 상태 완료(PAID) 처리
        paymentTransactionRepository.save(PaymentTransaction.record(payment, detail.transactionId(), PaymentStatus.PAID));
        payment.markPaid(LocalDateTime.now());
        payment.getOrder().markCompleted();
    }

    // Detached 상태의 Payment 엔티티에 구독 정보 연결
    @Transactional
    public void linkSubscription(String paymentId, Subscription subscription) {
        Payment payment = paymentRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND.value(), "결제 내역을 찾을 수 없습니다."));
        payment.linkSubscription(subscription);
    }

    // 발급된 결제 채널 키가 당사의 단건/정기결제 채널 키와 일치하는지 검증
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

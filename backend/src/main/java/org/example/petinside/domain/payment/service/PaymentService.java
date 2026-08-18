package org.example.petinside.domain.payment.service;

import lombok.RequiredArgsConstructor;
import org.example.petinside.domain.payment.dto.PaymentCompleteResponse;
import org.example.petinside.domain.payment.dto.PaymentPrepareResponse;
import org.example.petinside.domain.payment.entity.Payment;
import org.example.petinside.domain.payment.repository.PaymentRepository;
import org.example.petinside.domain.user.entity.User;
import org.example.petinside.domain.user.repository.UserRepository;
import org.example.petinside.global.exception.CustomException;
import org.example.petinside.global.exception.UserNotFoundException;
import org.example.petinside.global.portone.PortOneClient;
import org.example.petinside.global.portone.PortOneProperties;
import org.example.petinside.global.portone.dto.PortOnePaymentDetail;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    // 요금제는 아직 고정 요금 하나뿐 (구독 요금제 다양화는 범위 밖)
    static final int PLAN_AMOUNT = 1000;
    static final String CURRENCY = "KRW";
    private static final String PAID_STATUS = "PAID";

    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final PortOneClient portOneClient;
    private final PortOneProperties portOneProperties;

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

    // 구독 준비(F-21)에서도 재사용: 결제 준비 자체는 빌링키 유무와 무관하게 동일
    @Transactional
    public Payment createReadyPayment(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        //파라미터 제한(1~40자)에 맞춰 UUID는 하이픈 없이 사용
        String paymentId = portOneProperties.paymentIdPrefix() + "-SUB-" + UUID.randomUUID().toString().replace("-", "");
        Payment payment = Payment.createReady(user, paymentId, 1, PLAN_AMOUNT, CURRENCY);
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

    // 빌링키 없는 결제(단건조회 검증)용. 프론트가 결제창을 직접 호출했을 때 PortOne 재조회로 최종 확정한다.
    @Transactional
    public Payment verifyAndMarkPaid(Long userId, String paymentId) {
        Payment payment = findReadyPayment(userId, paymentId);
        PortOnePaymentDetail detail = portOneClient.getPaymentDetail(paymentId);
        finalizeByDetail(payment, detail);
        return payment;
    }

    // 완료 API(F-21) 웹훅이든 같은 동기화 로직을 타야 한다는 원칙에 따라 검증 기준을 한 곳에 둠
    public void finalizeByDetail(Payment payment, PortOnePaymentDetail detail) {
        boolean verified = PAID_STATUS.equalsIgnoreCase(detail.status())
                && detail.amount() != null && detail.amount().total() == payment.getAmount()
                && CURRENCY.equalsIgnoreCase(detail.currency())
                && portOneProperties.storeId().equals(detail.storeId());

        if (!verified) {
            payment.markFailed();
            throw new CustomException(HttpStatus.UNPROCESSABLE_ENTITY.value(), "PortOne 결제 정보가 주문과 일치하지 않습니다.");
        }

        payment.markPaid(LocalDateTime.now());
    }
}

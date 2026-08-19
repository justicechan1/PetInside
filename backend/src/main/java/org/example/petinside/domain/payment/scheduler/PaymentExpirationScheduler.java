package org.example.petinside.domain.payment.scheduler;

import lombok.RequiredArgsConstructor;
import org.example.petinside.domain.payment.entity.Payment;
import org.example.petinside.domain.payment.entity.PaymentStatus;
import org.example.petinside.domain.payment.repository.PaymentRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

// 5분마다: 결제창을 열어놓고(prepare) 10분 넘게 완료(/complete)도 웹훅도 안 온 READY 결제를 FAILED로 전환.
// 그렇지 않으면 이탈한 시도가 영영 READY로 DB에 남는다.
@Component
@RequiredArgsConstructor
public class PaymentExpirationScheduler {

    private static final long READY_TIMEOUT_MINUTES = 10;

    private final PaymentRepository paymentRepository;

    @Scheduled(cron = "0 */5 * * * *")
    @Transactional
    public void expireStaleReadyPayments() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(READY_TIMEOUT_MINUTES);
        for (Payment payment : paymentRepository.findByStatusAndCreatedAtBefore(PaymentStatus.READY, threshold)) {
            payment.markFailed();
            payment.getOrder().markFailed();
        }
    }
}

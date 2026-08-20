package org.example.petinside.domain.payment.scheduler;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.petinside.domain.payment.entity.Payment;
import org.example.petinside.domain.payment.entity.PaymentStatus;
import org.example.petinside.domain.payment.repository.PaymentRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

// 5분마다: 결제창을 열어놓고(prepare) 10분 넘게 완료(/complete)도 웹훅도 안 온 READY 결제를 FAILED로 전환.
// 그렇지 않으면 이탈한 시도가 영영 READY로 DB에 남는다.
@Slf4j
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
            try {
                payment.getOrder().markFailed();
            } catch (EntityNotFoundException e) {
                // 초기 데이터 중 order_id가 실제 존재하지 않는 주문을 가리키는 이상 데이터가 있었음(2026-08-20 발견).
                // 이거 하나 때문에 전체 배치가 매번 죽어서 다른 정상 건들까지 영영 READY로 안 남게 됐던 적이 있어서,
                // Payment 자체는 실패 처리하고 Order 쪽만 건너뛰도록 방어.
                log.warn("결제 만료 처리 중 연결된 주문을 찾을 수 없음: paymentId={}", payment.getPaymentId());
            }
        }
    }
}

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

// 결제창 유실 또는 사용자 이탈로 인해 고립된 READY 상태의 결제건을 자동으로 만료 처리하는 스케줄러.
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentExpirationScheduler {

    // 결제 준비 완료 후 최종 완료까지 기다리는 시간
    private static final long READY_TIMEOUT_MINUTES = 10;

    private final PaymentRepository paymentRepository;

    // 생성된 지 10 분이 지난 READY 상태의 결제 및 연결된 주문을 실패 상태로 전환 (5분마다 실행)
    @Scheduled(cron = "0 */5 * * * *")
    @Transactional
    public void expireStaleReadyPayments() {
        // 기준 시간 설정 (현재시각-10분)
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(READY_TIMEOUT_MINUTES);
        // 10분 이상 지난 READY 상태의 결제 목록 조회
        for (Payment payment : paymentRepository.findByStatusAndCreatedAtBefore(PaymentStatus.READY, threshold)) {
            // 결제 상태를 FAILED로 변경
            payment.markFailed();
            // 연결된 주문 상태 역시 FAILED로 변경
            try {
                payment.getOrder().markFailed();
            } catch (EntityNotFoundException e) {
                // Payment 자체는 실패 처리하되, Order 참조 실패 시 방어적으로 catch하여 개별 로그만 남기고 다른 결제건의 만료 처리를 계속 진행함.
                log.warn("결제 만료 처리 중 연결된 주문을 찾을 수 없음: paymentId={}", payment.getPaymentId());
            }
        }
    }
}

package org.example.petinside.domain.subscription;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.example.petinside.domain.subscription.dto.SubscriptionCreateRequest;
import org.example.petinside.domain.subscription.entity.BillingKey;
import org.example.petinside.domain.subscription.repository.BillingKeyRepository;
import org.example.petinside.domain.subscription.repository.SubscriptionRepository;
import org.example.petinside.domain.subscription.service.SubscriptionService;
import org.example.petinside.domain.user.entity.User;
import org.example.petinside.domain.user.repository.UserRepository;
import org.example.petinside.global.portone.BillingKeyEncryptor;
import org.example.petinside.global.portone.PortOneClient;
import org.example.petinside.global.portone.dto.PortOnePaymentDetail;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

// 같은 사용자가 정기구독 시작을 동시에 여러 번 요청했을 때(결제 버튼 연타·재시도) 실제 청구가 몇 번 나가는지 측정.
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:mysql://localhost:${TEST_DB_PORT:3318}/petinside_test?serverTimezone=Asia/Seoul&characterEncoding=UTF-8",
        "spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver",
        "spring.datasource.username=app",
        "spring.datasource.password=app_local_only",
        "spring.jpa.hibernate.ddl-auto=create",
        "portone.require-test-channel=false",
        "jwt.secret=d0463ea8b8be735d3384ec13ddd9b64fb5ccfe562c827b4d5ab25b6118ade6db"
})
class SubscriptionConcurrencyTest {

    private static final int REQUESTS = 10;

    @Autowired SubscriptionService subscriptionService;
    @Autowired UserRepository userRepository;
    @Autowired BillingKeyRepository billingKeyRepository;
    @Autowired SubscriptionRepository subscriptionRepository;

    @MockBean PortOneClient portOneClient;
    @MockBean BillingKeyEncryptor billingKeyEncryptor;
    @MockBean org.example.petinside.domain.notification.service.WebPushSender webPushSender;

    @Test
    void 동시_구독_시작_10건() throws Exception {
        User user = userRepository.save(User.createLocalUser("concurrency@test.com", "pw", "동시성"));
        BillingKey billingKey = billingKeyRepository.save(BillingKey.issue(user, "encrypted", LocalDateTime.now()));

        AtomicInteger charges = new AtomicInteger();
        when(billingKeyEncryptor.decrypt(anyString())).thenReturn("billing-key");
        when(portOneClient.payWithBillingKey(anyString(), any())).thenAnswer(inv -> {
            charges.incrementAndGet();
            Thread.sleep(200); // PG 승인 왕복 시간
            return null;
        });
        when(portOneClient.getPaymentDetail(anyString())).thenAnswer(inv -> new PortOnePaymentDetail(
                inv.getArgument(0), "PAID", "test-store-id", "KRW", "tx-" + inv.getArgument(0),
                new PortOnePaymentDetail.Amount(1900),
                new PortOnePaymentDetail.Channel("TEST", "test-channel-key-sub")));

        ExecutorService pool = Executors.newFixedThreadPool(REQUESTS);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger failed = new AtomicInteger();
        for (int i = 0; i < REQUESTS; i++) {
            pool.submit(() -> {
                try {
                    start.await();
                    subscriptionService.create(user.getId(), new SubscriptionCreateRequest(billingKey.getId()));
                    success.incrementAndGet();
                } catch (Exception e) {
                    failed.incrementAndGet();
                }
            });
        }
        start.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(60, TimeUnit.SECONDS)).isTrue();

        long subscriptions = subscriptionRepository.count();
        System.out.println("[동시 구독 측정] 요청 " + REQUESTS + "건 → PG 청구 " + charges.get()
                + "회 / 구독 생성 " + subscriptions + "건 / 성공 " + success.get() + " / 실패 " + failed.get());
        assertThat(charges.get()).isEqualTo(1);
        assertThat(subscriptions).isEqualTo(1);
    }
}

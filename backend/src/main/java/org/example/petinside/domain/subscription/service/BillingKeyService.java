package org.example.petinside.domain.subscription.service;

import lombok.RequiredArgsConstructor;
import org.example.petinside.domain.subscription.dto.BillingKeyCreateRequest;
import org.example.petinside.domain.subscription.dto.BillingKeyCreateResponse;
import org.example.petinside.domain.subscription.dto.BillingKeyPrepareResponse;
import org.example.petinside.domain.subscription.entity.BillingKey;
import org.example.petinside.domain.subscription.entity.BillingKeyIssuanceIntent;
import org.example.petinside.domain.subscription.repository.BillingKeyIssuanceIntentRepository;
import org.example.petinside.domain.subscription.repository.BillingKeyRepository;
import org.example.petinside.domain.user.entity.User;
import org.example.petinside.domain.user.repository.UserRepository;
import org.example.petinside.global.exception.CustomException;
import org.example.petinside.global.exception.UserNotFoundException;
import org.example.petinside.global.portone.BillingKeyEncryptor;
import org.example.petinside.global.portone.PortOneClient;
import org.example.petinside.global.portone.PortOneProperties;
import org.example.petinside.global.portone.dto.PortOneBillingKeyDetail;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

// 카드 등록(빌링키 발급)을 처리하는 서비스.
// PortOne SDK가 카드정보를 직접 받아 처리한 뒤 빌링키만 넘겨줌.
@Service
@RequiredArgsConstructor
public class BillingKeyService {

    private static final String ISSUED_STATUS = "ISSUED"; // 정상발급 상숫값

    private final UserRepository userRepository;
    private final BillingKeyIssuanceIntentRepository intentRepository;
    private final BillingKeyRepository billingKeyRepository;
    private final PortOneClient portOneClient;
    private final PortOneProperties portOneProperties;
    private final BillingKeyEncryptor billingKeyEncryptor;

    // 프론트가 SDK를 호출하기 전에 실행, 누가 발급을 시도했는지를 먼저 서버에 남겨둠.
    // 발급 성공 뒤 프론트가 이탈해도 복구할 수 있게 하기 위함.
    @Transactional
    public BillingKeyPrepareResponse prepare(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId)); // 사용자 존재 여부 확인

        String issueId = "issue-" + UUID.randomUUID().toString().replace("-", ""); // 식별자 생성
        intentRepository.save(BillingKeyIssuanceIntent.create(issueId, user));

        return new BillingKeyPrepareResponse(issueId, portOneProperties.storeId(), portOneProperties.channelKeySubscription());
    }

    // 발급 완료 API: 프론트가 SDK로 카드 등록을 마친 뒤, 발급받은 빌링키를 알려주면 verifyAndStore로 검증·암호화 저장까지 진행.
    @Transactional
    public BillingKeyCreateResponse create(Long userId, BillingKeyCreateRequest request) {
        // 본인 여부 검증
        BillingKeyIssuanceIntent intent = intentRepository.findByIssueId(request.issueId())
                .filter(i -> i.getUser().getId().equals(userId))
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND.value(), "빌링키 발급 의도를 찾을 수 없습니다."));

        // 중복 검사(멱등성)
        if (intent.isCompleted()) {
            throw new CustomException(HttpStatus.CONFLICT.value(), "이미 처리된 발급 요청입니다.");
        }

        // 검증 + 암호화
        BillingKey billingKey = verifyAndStore(intent, request.billingKey());
        return new BillingKeyCreateResponse(billingKey.getId());
    }

    // PortOne 데이터 위변조 검증 및 암호화 저장
    @Transactional
    public BillingKey verifyAndStore(BillingKeyIssuanceIntent intent, String rawBillingKey) {
        // 빌링키 상세 정보 조회
        PortOneBillingKeyDetail detail = portOneClient.getBillingKeyDetail(rawBillingKey);
        // 유효성 조건 교차 검증
        boolean verified = ISSUED_STATUS.equalsIgnoreCase(detail.status()) // 상태
                && portOneProperties.storeId().equals(detail.storeId())    // 상점ID
                && isOurChannel(detail.channels())                         // 채널
                && isSameCustomer(detail.customer(), intent);              // 고객ID

        if (!verified) {
            throw new CustomException(HttpStatus.UNPROCESSABLE_ENTITY.value(), "PortOne 빌링키 정보가 유효하지 않습니다.");
        }

        // 빌링키 원문을 AES 알고리즘으로 암호화
        String encrypted = billingKeyEncryptor.encrypt(rawBillingKey);
        // 엔티티 생성 및 저장
        BillingKey billingKey = billingKeyRepository.save(BillingKey.issue(intent.getUser(), encrypted, LocalDateTime.now()));
        // 완료처리하여 중복 방지
        intent.markCompleted(LocalDateTime.now());
        return billingKey;
    }

    // 빌링키가 우리 서비스의 정기결제 전용 채널을 통해 정상 발급되었는지 확인.
    private boolean isOurChannel(List<PortOneBillingKeyDetail.Channel> channels) {
        if (channels == null) {
            return false;
        }
        return channels.stream().anyMatch(channel ->
                portOneProperties.channelKeySubscription().equals(channel.key())
                        && "TEST".equalsIgnoreCase(channel.type()));
    }

    // SDK 결제 요청 시 넘겨준 customerId와 실제 접속 중인 사용자가 일치하는지 검증.
    private boolean isSameCustomer(PortOneBillingKeyDetail.Customer customer, BillingKeyIssuanceIntent intent) {
        return customer != null && String.valueOf(intent.getUser().getId()).equals(customer.id());
    }
}

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

@Service
@RequiredArgsConstructor
public class BillingKeyService {

    private static final String ISSUED_STATUS = "ISSUED";

    private final UserRepository userRepository;
    private final BillingKeyIssuanceIntentRepository intentRepository;
    private final BillingKeyRepository billingKeyRepository;
    private final PortOneClient portOneClient;
    private final PortOneProperties portOneProperties;
    private final BillingKeyEncryptor billingKeyEncryptor;

    // 프론트가 requestIssueBillingKey를 호출하기 전에, 누가 발급을 시도했는지를 먼저 서버에 남겨둔다.
    // 발급 성공 뒤 프론트가 이탈해도 복구할 수 있게 하기 위함.
    @Transactional
    public BillingKeyPrepareResponse prepare(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        String issueId = "issue-" + UUID.randomUUID().toString().replace("-", "");
        intentRepository.save(BillingKeyIssuanceIntent.create(issueId, user));

        return new BillingKeyPrepareResponse(issueId, portOneProperties.storeId(), portOneProperties.channelKeySubscription());
    }

    @Transactional
    public BillingKeyCreateResponse create(Long userId, BillingKeyCreateRequest request) {
        BillingKeyIssuanceIntent intent = intentRepository.findByIssueId(request.issueId())
                .filter(i -> i.getUser().getId().equals(userId))
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND.value(), "빌링키 발급 의도를 찾을 수 없습니다."));

        if (intent.isCompleted()) {
            throw new CustomException(HttpStatus.CONFLICT.value(), "이미 처리된 발급 요청입니다.");
        }

        BillingKey billingKey = verifyAndStore(intent, request.billingKey());
        return new BillingKeyCreateResponse(billingKey.getId());
    }

    // 완료 API든 BillingKey.Issued 웹훅 복구 경로든 같은 검증·저장 로직을 공유.
    @Transactional
    public BillingKey verifyAndStore(BillingKeyIssuanceIntent intent, String rawBillingKey) {
        PortOneBillingKeyDetail detail = portOneClient.getBillingKeyDetail(rawBillingKey);
        boolean verified = ISSUED_STATUS.equalsIgnoreCase(detail.status())
                && portOneProperties.storeId().equals(detail.storeId())
                && isOurChannel(detail.channels())
                && isSameCustomer(detail.customer(), intent);

        if (!verified) {
            throw new CustomException(HttpStatus.UNPROCESSABLE_ENTITY.value(), "PortOne 빌링키 정보가 유효하지 않습니다.");
        }

        String encrypted = billingKeyEncryptor.encrypt(rawBillingKey);
        BillingKey billingKey = billingKeyRepository.save(BillingKey.issue(intent.getUser(), encrypted, LocalDateTime.now()));
        intent.markCompleted(LocalDateTime.now());
        return billingKey;
    }

    // 테스트 상점은 여러 팀이 공용으로 써서, storeId만으로는 다른 팀 채널로 발급된 빌링키를 걸러내지 못함.
    // 정기결제용 채널(channelKeySubscription)로, 그것도 테스트 채널로 발급된 것만 인정.
    private boolean isOurChannel(List<PortOneBillingKeyDetail.Channel> channels) {
        if (channels == null) {
            return false;
        }
        return channels.stream().anyMatch(channel ->
                portOneProperties.channelKeySubscription().equals(channel.key())
                        && "TEST".equalsIgnoreCase(channel.type()));
    }

    // 프론트에서 requestIssueBillingKey 호출 시 customerId로 우리 userId를 그대로 넘기므로,
    // 발급 의도(intent)를 남긴 사용자와 실제로 발급받은 사용자가 같은지 대조 가능.
    private boolean isSameCustomer(PortOneBillingKeyDetail.Customer customer, BillingKeyIssuanceIntent intent) {
        return customer != null && String.valueOf(intent.getUser().getId()).equals(customer.id());
    }
}

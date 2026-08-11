package org.example.petinside.global.security.oauth2;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// OAuth2 로그인 성공 시 콜백 URL에 토큰을 직접 노출하지 않기 위한 1회용 코드 저장소.
// 프론트는 코드만 받아 /api/v1/auth/oauth2/exchange로 즉시 교환해 토큰을 받는다.
@Component
public class OAuth2AuthCodeStore {

    private static final long CODE_TTL_MILLIS = 60_000; // 1분

    private final Map<String, Entry> codes = new ConcurrentHashMap<>();

    public String issue(Long userId) {
        String code = UUID.randomUUID().toString();
        codes.put(code, new Entry(userId, Instant.now().plusMillis(CODE_TTL_MILLIS)));
        return code;
    }

    // 코드를 1회성으로 소비: 조회 즉시 제거하고, 만료된 경우 empty 반환
    public Optional<Long> consume(String code) {
        Entry entry = codes.remove(code);
        if (entry == null || Instant.now().isAfter(entry.expiresAt())) {
            return Optional.empty();
        }
        return Optional.of(entry.userId());
    }

    private record Entry(Long userId, Instant expiresAt) {
    }
}
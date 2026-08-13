package org.example.petinside.domain.auth.dto;

// accessToken/refreshToken 발급 결과를 서비스 → 컨트롤러로 넘기는 내부 전달용 DTO.
// refreshToken은 API 응답 바디에 노출하지 않고 컨트롤러가 HttpOnly 쿠키로만 내려보낸다.
public record AuthTokens(
        String accessToken,
        String refreshToken,
        String tokenType
) {
}
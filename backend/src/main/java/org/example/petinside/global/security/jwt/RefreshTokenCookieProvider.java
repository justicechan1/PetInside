package org.example.petinside.global.security.jwt;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

// refreshToken을 HttpOnly 쿠키로 발급/만료시키는 로직을 한 곳에 모아
// login/reissue/oauth2 exchange/logout이 동일한 쿠키 속성(path, SameSite 등)을 쓰도록 보장한다.
@Component
@RequiredArgsConstructor
public class RefreshTokenCookieProvider {

    private static final String COOKIE_NAME = "refreshToken";
    private static final String COOKIE_PATH = "/api/v1/auth";

    private final JwtProperties jwtProperties;

    public ResponseCookie create(String refreshToken) {
        return ResponseCookie.from(COOKIE_NAME, refreshToken)
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path(COOKIE_PATH)
                .maxAge(Duration.ofMillis(jwtProperties.refreshTokenExpiration()))
                .build();
    }

    public ResponseCookie expire() {
        return ResponseCookie.from(COOKIE_NAME, "")
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path(COOKIE_PATH)
                .maxAge(0)
                .build();
    }
}
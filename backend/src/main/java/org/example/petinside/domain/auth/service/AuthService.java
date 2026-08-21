package org.example.petinside.domain.auth.service;

import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.example.petinside.domain.auth.dto.AuthTokens;
import org.example.petinside.domain.auth.dto.LoginRequest;
import org.example.petinside.domain.auth.dto.SignupRequest;
import org.example.petinside.domain.auth.entity.RefreshToken;
import org.example.petinside.domain.auth.repository.RefreshTokenRepository;
import org.example.petinside.domain.user.entity.User;
import org.example.petinside.domain.user.repository.UserRepository;
import org.example.petinside.global.exception.CustomException;
import org.example.petinside.global.exception.DuplicateFieldException;
import org.example.petinside.global.exception.InvalidCredentialsException;
import org.example.petinside.global.exception.UserNotFoundException;
import org.example.petinside.global.security.jwt.JwtProperties;
import org.example.petinside.global.security.jwt.JwtProvider;
import org.example.petinside.global.security.jwt.TokenHasher;
import org.example.petinside.global.security.oauth2.OAuth2AuthCodeStore;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String INVALID_CREDENTIALS_MESSAGE = "아이디 또는 비밀번호가 일치하지 않습니다.";
    private static final String INVALID_REFRESH_TOKEN_MESSAGE = "유효하지 않거나 만료된 refreshToken입니다.";
    private static final String ALREADY_REVOKED_REFRESH_TOKEN_MESSAGE = "유효하지 않거나 이미 폐기된 refreshToken입니다.";
    private static final String TOKEN_TYPE = "Bearer";

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final TokenHasher tokenHasher;
    private final JwtProperties jwtProperties;
    private final OAuth2AuthCodeStore oAuth2AuthCodeStore;

    @Transactional
    public void signup(SignupRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new DuplicateFieldException("이미 사용 중인 아이디입니다.");
        }
        if (userRepository.existsByNickname(request.nickname())) {
            throw new DuplicateFieldException("이미 사용 중인 닉네임입니다.");
        }

        // 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(request.password());
        User user = User.createLocalUser(request.username(), encodedPassword, request.nickname());

        userRepository.save(user);
    }

    // 로그인 service
    @Transactional
    public AuthTokens login(LoginRequest request) {
        // 사용자 유무 확인
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new InvalidCredentialsException(INVALID_CREDENTIALS_MESSAGE));

        // 삭제된 계정 로그인 차단
        if (user.isDeleted()) {
            throw new InvalidCredentialsException(INVALID_CREDENTIALS_MESSAGE);
        }

        // 비밀번호 확인 (소셜 로그인 계정은 일반 로그인 차단)
        if (user.getPassword() == null || !passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new InvalidCredentialsException(INVALID_CREDENTIALS_MESSAGE);
        }

        return issueTokens(user);
    }

    // 토큰 재발급: refreshToken 서명/만료 검증 + DB에 살아있고(is_revoked=false) 대조되는 토큰인지 확인 후 로테이션 발급
    @Transactional
    public AuthTokens reissue(String refreshToken) {
        if (refreshToken == null) {
            throw new CustomException(HttpStatus.UNAUTHORIZED.value(), INVALID_REFRESH_TOKEN_MESSAGE);
        }

        JwtProvider.JwtPayload payload;
        try {
            payload = jwtProvider.parseRefreshToken(refreshToken);
        } catch (JwtException | IllegalArgumentException e) {
            throw new CustomException(HttpStatus.UNAUTHORIZED.value(), INVALID_REFRESH_TOKEN_MESSAGE);
        }

        RefreshToken saved = refreshTokenRepository.findByTokenValue(tokenHasher.sha256Hex(refreshToken))
                .filter(rt -> rt.getUser().getId().equals(payload.userId()))
                .filter(rt -> !rt.isRevoked())
                .orElseThrow(() -> new CustomException(HttpStatus.UNAUTHORIZED.value(), INVALID_REFRESH_TOKEN_MESSAGE));

        return issueTokens(saved.getUser());
    }

    // 로그아웃: accessToken 인증 + refreshToken 쿠키 대조 후 DB에서 폐기(is_revoked=true) 처리 → 재발급 차단
    @Transactional
    public void logout(Long userId, String refreshToken) {
        if (refreshToken == null) {
            throw new CustomException(HttpStatus.UNAUTHORIZED.value(), ALREADY_REVOKED_REFRESH_TOKEN_MESSAGE);
        }

        RefreshToken saved = refreshTokenRepository.findByTokenValue(tokenHasher.sha256Hex(refreshToken))
                .filter(rt -> rt.getUser().getId().equals(userId))
                .filter(rt -> !rt.isRevoked())
                .orElseThrow(() -> new CustomException(HttpStatus.UNAUTHORIZED.value(), ALREADY_REVOKED_REFRESH_TOKEN_MESSAGE));

        saved.revoke();
    }

    // 소셜 로그인 콜백에서 발급된 1회용 code를 실제 accessToken/refreshToken으로 교환
    @Transactional
    public AuthTokens exchangeOAuth2Code(String code) {
        Long userId = oAuth2AuthCodeStore.consume(code)
                .orElseThrow(() -> new CustomException(HttpStatus.UNAUTHORIZED.value(), "유효하지 않거나 만료된 code입니다."));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        return issueTokens(user);
    }

    // accessToken/refreshToken 발급 + 유저당 refreshToken 1개만 유지(기존 토큰 있으면 제거 후 재발급)
    private AuthTokens issueTokens(User user) {
        String accessToken = jwtProvider.createAccessToken(user.getId(), user.getRole());
        String refreshToken = jwtProvider.createRefreshToken(user.getId());

        refreshTokenRepository.deleteByUser(user);
        refreshTokenRepository.save(RefreshToken.builder()
                .id(UUID.randomUUID().toString())
                .user(user)
                .tokenValue(tokenHasher.sha256Hex(refreshToken))
                .expiresAt(LocalDateTime.now().plusNanos(jwtProperties.refreshTokenExpiration() * 1_000_000))
                .build());

        return new AuthTokens(accessToken, refreshToken, TOKEN_TYPE);
    }
}

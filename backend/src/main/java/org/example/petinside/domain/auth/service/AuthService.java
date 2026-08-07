package org.example.petinside.domain.auth.service;

import lombok.RequiredArgsConstructor;
import org.example.petinside.domain.auth.dto.LoginRequest;
import org.example.petinside.domain.auth.dto.LoginResponse;
import org.example.petinside.domain.auth.dto.SignupRequest;
import org.example.petinside.domain.auth.entity.RefreshToken;
import org.example.petinside.domain.auth.repository.RefreshTokenRepository;
import org.example.petinside.domain.user.entity.User;
import org.example.petinside.domain.user.repository.UserRepository;
import org.example.petinside.global.exception.DuplicateFieldException;
import org.example.petinside.global.exception.InvalidCredentialsException;
import org.example.petinside.global.security.jwt.JwtProperties;
import org.example.petinside.global.security.jwt.JwtProvider;
import org.example.petinside.global.security.jwt.TokenHasher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String INVALID_CREDENTIALS_MESSAGE = "아이디 또는 비밀번호가 일치하지 않습니다.";
    private static final String TOKEN_TYPE = "Bearer";

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final TokenHasher tokenHasher;
    private final JwtProperties jwtProperties;

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
    public LoginResponse login(LoginRequest request) {
        // 사용자 유무 확인
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new InvalidCredentialsException(INVALID_CREDENTIALS_MESSAGE));

        // 비밀번호 확인 (소셜 로그인 계정은 일반 로그인 차단)
        if (user.getPassword() == null || !passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new InvalidCredentialsException(INVALID_CREDENTIALS_MESSAGE);
        }

        String accessToken = jwtProvider.createAccessToken(user.getId(), user.getRole());
        String refreshToken = jwtProvider.createRefreshToken(user.getId());

        // 유저당 refreshToken 1개만 유저 (기존 토큰 있으면 제거 후 재발급)
        refreshTokenRepository.deleteByUser(user);
        refreshTokenRepository.save(RefreshToken.builder()
                .id(UUID.randomUUID().toString())
                .user(user)
                .tokenValue(tokenHasher.sha256Hex(refreshToken))
                .expiresAt(LocalDateTime.now().plusNanos(jwtProperties.refreshTokenExpiration() * 1_000_000))
                .build());

        return new LoginResponse(accessToken, refreshToken, TOKEN_TYPE);
    }
}

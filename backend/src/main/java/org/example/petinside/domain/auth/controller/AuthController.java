package org.example.petinside.domain.auth.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.petinside.domain.auth.dto.LoginRequest;
import org.example.petinside.domain.auth.dto.LoginResponse;
import org.example.petinside.domain.auth.dto.RefreshRequest;
import org.example.petinside.domain.auth.dto.SignupRequest;
import org.example.petinside.domain.auth.service.AuthService;
import org.example.petinside.global.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // 회원가입
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<Void>> signup(@Valid @RequestBody SignupRequest request) {
        authService.signup(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.<Void>success(HttpStatus.CREATED.value(), "회원가입 성공"));
    }

    // 로그인
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);

        return ResponseEntity
                .ok(ApiResponse.success(HttpStatus.OK.value(), "로그인 성공", response));
    }

    // 토큰 재발급: Refresh Token 필요, Access Token 불필요 (permitAll)
    @PostMapping("/reissue")
    public ResponseEntity<ApiResponse<LoginResponse>> reissue(@Valid @RequestBody RefreshRequest request) {
        LoginResponse response = authService.reissue(request);

        return ResponseEntity
                .ok(ApiResponse.success(HttpStatus.OK.value(), "토큰 재발급 성공", response));
    }

    // 로그아웃: Access Token 필요 (permitAll 아님, JwtAuthenticationFilter가 인증 처리)
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@AuthenticationPrincipal Long userId) {
        authService.logout(userId);

        return ResponseEntity
                .ok(ApiResponse.success(HttpStatus.OK.value(), "로그아웃 성공"));
    }
}

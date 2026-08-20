package org.example.petinside.domain.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.petinside.domain.auth.dto.AuthTokens;
import org.example.petinside.domain.auth.dto.LoginRequest;
import org.example.petinside.domain.auth.dto.LoginResponse;
import org.example.petinside.domain.auth.dto.OAuth2ExchangeRequest;
import org.example.petinside.domain.auth.dto.SignupRequest;
import org.example.petinside.domain.auth.service.AuthService;
import org.example.petinside.global.response.ApiResponse;
import org.example.petinside.global.security.jwt.RefreshTokenCookieProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "인증", description = "회원가입, 로그인, 토큰 재발급/로그아웃 API")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenCookieProvider refreshTokenCookieProvider;

    @Operation(
            summary = "회원가입",
            description = """
                    로컬 계정으로 회원가입합니다. 인증 불필요.
                    - username: 영문 소문자 + 숫자, 4~20자
                    - password: REDACTED 이상, 영문 + 숫자 + 특수문자 포함
                    - nickname: 공백 없이 2~10자
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201", description = "회원가입 성공",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, examples = @ExampleObject(
                            value = "{\"status\":201,\"message\":\"회원가입 성공\",\"data\":null}"))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "형식 위반 / 필수값 누락 / 아이디 또는 닉네임 중복",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, examples = @ExampleObject(
                            value = "{\"status\":400,\"message\":\"이미 사용 중인 아이디입니다.\",\"data\":null}"))
            )
    })
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<Void>> signup(@Valid @RequestBody SignupRequest request) {
        authService.signup(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.<Void>success(HttpStatus.CREATED.value(), "회원가입 성공"));
    }

    @Operation(summary = "로그인", description = "아이디/비밀번호로 로그인합니다. accessToken(1시간)은 응답 바디로, refreshToken(1주일)은 HttpOnly 쿠키(Set-Cookie)로 발급됩니다. 인증 불필요.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "로그인 성공",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = LoginResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "아이디 또는 비밀번호 불일치",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, examples = @ExampleObject(
                            value = "{\"status\":401,\"message\":\"아이디 또는 비밀번호가 일치하지 않습니다.\",\"data\":null}"))
            )
    })
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthTokens tokens = authService.login(request);

        return ResponseEntity
                .ok()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookieProvider.create(tokens.refreshToken()).toString())
                .body(ApiResponse.success(HttpStatus.OK.value(), "로그인 성공",
                        new LoginResponse(tokens.accessToken(), tokens.tokenType())));
    }

    @Operation(summary = "토큰 재발급", description = "refreshToken 쿠키(브라우저가 자동 전송)로 accessToken/refreshToken을 재발급합니다. 재발급 시 기존 refreshToken은 폐기되고 새 refreshToken 쿠키가 발급됩니다(로테이션). Access Token 불필요.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "토큰 재발급 성공",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = LoginResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "refreshToken 쿠키 없음 / 만료 / 위조 / 이미 폐기됨",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, examples = @ExampleObject(
                            value = "{\"status\":401,\"message\":\"유효하지 않거나 만료된 refreshToken입니다.\",\"data\":null}"))
            )
    })
    @PostMapping("/reissue")
    public ResponseEntity<ApiResponse<LoginResponse>> reissue(
            @Parameter(hidden = true) @CookieValue(value = "refreshToken", required = false) String refreshToken) {
        AuthTokens tokens = authService.reissue(refreshToken);

        return ResponseEntity
                .ok()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookieProvider.create(tokens.refreshToken()).toString())
                .body(ApiResponse.success(HttpStatus.OK.value(), "토큰 재발급 성공",
                        new LoginResponse(tokens.accessToken(), tokens.tokenType())));
    }

    @Operation(summary = "소셜 로그인 code 교환", description = "소셜 로그인 콜백에서 받은 1회용 code를 실제 토큰으로 교환합니다. accessToken은 응답 바디로, refreshToken은 HttpOnly 쿠키로 발급됩니다(로그인과 동일). 인증 불필요.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "로그인 성공",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = LoginResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "유효하지 않거나 만료된 code",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, examples = @ExampleObject(
                            value = "{\"status\":401,\"message\":\"유효하지 않거나 만료된 code입니다.\",\"data\":null}"))
            )
    })
    @PostMapping("/oauth2/exchange")
    public ResponseEntity<ApiResponse<LoginResponse>> exchangeOAuth2Code(@Valid @RequestBody OAuth2ExchangeRequest request) {
        AuthTokens tokens = authService.exchangeOAuth2Code(request.code());

        return ResponseEntity
                .ok()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookieProvider.create(tokens.refreshToken()).toString())
                .body(ApiResponse.success(HttpStatus.OK.value(), "로그인 성공",
                        new LoginResponse(tokens.accessToken(), tokens.tokenType())));
    }

    @Operation(summary = "로그아웃", description = "accessToken 사용자의 refreshToken(쿠키로 전송됨)을 DB에서 폐기(is_revoked=true)하고, 응답으로 refreshToken 쿠키를 즉시 만료시킵니다. Access Token 필요.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "로그아웃 성공",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, examples = @ExampleObject(
                            value = "{\"status\":200,\"message\":\"로그아웃 성공\",\"data\":null}"))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "Access Token 없음/만료, 또는 refreshToken 쿠키 없음/이미 폐기됨",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
            )
    })
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId,
            @Parameter(hidden = true) @CookieValue(value = "refreshToken", required = false) String refreshToken) {
        authService.logout(userId, refreshToken);

        return ResponseEntity
                .ok()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookieProvider.expire().toString())
                .body(ApiResponse.success(HttpStatus.OK.value(), "로그아웃 성공"));
    }
}

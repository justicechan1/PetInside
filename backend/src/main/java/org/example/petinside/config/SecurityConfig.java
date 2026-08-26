package org.example.petinside.config;

import lombok.RequiredArgsConstructor;
import org.example.petinside.global.security.RestAuthenticationEntryPoint;
import org.example.petinside.global.security.jwt.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.example.petinside.global.security.oauth2.CustomAuthorizationRequestResolver;
import org.example.petinside.global.security.oauth2.CustomOidcUserService;
import org.example.petinside.global.security.oauth2.OAuth2SuccessHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.RegexRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    @Value("${cors.allowed-origins}")
    private String[] allowedOrigins;

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;
    private final CustomOidcUserService customOidcUserService;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final CustomAuthorizationRequestResolver customAuthorizationRequestResolver;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(request -> {
                    var config = new CorsConfiguration();
                    config.setAllowedOrigins(Arrays.asList(allowedOrigins));
                    config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
                    config.setAllowedHeaders(List.of("*"));
                    config.setAllowCredentials(true);
                    return config;
                }))
                .csrf(csrf -> csrf.disable())
                // OAuth2Login의 인가 요청(state)은 세션에 저장되므로 완전 STATELESS 대신 IF_REQUIRED 사용
                // (API 인증 자체는 세션이 아니라 JwtAuthenticationFilter가 담당하므로 영향 없음)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .httpBasic(httpBasic -> httpBasic.disable())
                .formLogin(formLogin -> formLogin.disable())
                // 회원가입/로그인/토큰재발급/구글 OAuth2 흐름/Swagger/에러페이지 권한 허용 (로그아웃은 Access Token 필요하므로 제외)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/v1/auth/signup",
                                "/api/v1/auth/login",
                                "/api/v1/auth/reissue", // accessToken을 새로 재발급
                                "/api/v1/auth/oauth2/exchange", // 소셜 로그인 콜백 code를 토큰으로 교환
                                "/api/v1/payments/webhook", // PortOne 서버가 호출 - JWT 대신 웹훅 서명으로 검증
                                "/oauth2/**",
                                "/login/oauth2/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/error",
                                "/images/**" // 로컬 디스크에 업로드된 이미지 정적 서빙 - 비회원도 조회 가능해야 함
                        ).permitAll()
                        // 게시글 목록/상세, 댓글 목록, 좋아요 상태(개수) 조회는 비회원도 가능
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/posts",
                                "/api/v1/posts/*",
                                "/api/v1/posts/*/comments",
                                "/api/v1/posts/*/likes/me",
                                "/api/v1/comments/*/likes/me",
                                "/api/v1/pets/photos/popular"
                        ).permitAll()
                        // 공개 프로필 — me/* 는 인증 필요, 나머지 숫자 userId 경로는 비회원 공개
                        // me/posts 를 먼저 선언해야 */posts 보다 우선 매칭됨
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/users/me",
                                "/api/v1/users/me/posts",
                                "/api/v1/users/me/pets"
                        ).authenticated()
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/users/*",
                                "/api/v1/users/*/posts",
                                "/api/v1/users/*/pets",
                                "/api/v1/pets/*/photos"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                // 인증되지 않은 사용자 401
                .exceptionHandling(e -> e.authenticationEntryPoint(restAuthenticationEntryPoint))
                // 구글 OAuth2 로그인: 유저 정보 조회/자동가입은 CustomOidcUserService, 성공 후 JWT 발급은 OAuth2SuccessHandler
                .oauth2Login(oauth2 -> oauth2
                        // 로그아웃 후 재로그인 시 구글 세션으로 자동 로그인되지 않도록 매번 계정 선택 화면을 강제
                        .authorizationEndpoint(endpoint -> endpoint.authorizationRequestResolver(customAuthorizationRequestResolver))
                        .userInfoEndpoint(userInfo -> userInfo.oidcUserService(customOidcUserService))
                        .successHandler(oAuth2SuccessHandler)
                )
                // JWT 인증 필터를 UsernamePasswordAuthenticationFilter 앞에 등록
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}

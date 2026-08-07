package org.example.petinside.config;

import lombok.RequiredArgsConstructor;
import org.example.petinside.global.security.RestAuthenticationEntryPoint;
import org.example.petinside.global.security.jwt.JwtAuthenticationFilter;
import org.example.petinside.global.security.oauth2.CustomOidcUserService;
import org.example.petinside.global.security.oauth2.OAuth2SuccessHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;
    private final CustomOidcUserService customOidcUserService;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
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
                                "/oauth2/**",
                                "/login/oauth2/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/error"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                // 인증되지 않은 사용자 401
                .exceptionHandling(e -> e.authenticationEntryPoint(restAuthenticationEntryPoint))
                // 구글 OAuth2 로그인: 유저 정보 조회/자동가입은 CustomOidcUserService, 성공 후 JWT 발급은 OAuth2SuccessHandler
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo.oidcUserService(customOidcUserService))
                        .successHandler(oAuth2SuccessHandler)
                )
                // JWT 인증 필터를 UsernamePasswordAuthenticationFilter 앞에 등록
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}

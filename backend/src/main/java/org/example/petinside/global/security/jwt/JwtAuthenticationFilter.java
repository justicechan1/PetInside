package org.example.petinside.global.security.jwt;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";

    private final JwtProvider jwtProvider;

    // 요청이 들어오면 Authorization 헤더의 JWT를 꺼내서 검증
    // 성공 시 SecurityContext에 인증 정보 주입
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // SecurityConfig가 OAuth2 state 저장 때문에 세션(IF_REQUIRED)을 쓰는데, Spring Security가
        // 이전 요청에서 세션에 저장해둔 인증 정보를 요청마다 자동으로 복원해준다. 그래서 토큰이 없을 때
        // 아무것도 안 하면(=여기서 지우지 않으면) 로그아웃 후에도 세션에 남은 예전 인증이 그대로
        // 살아남아 계속 인증된 것처럼 처리되는 문제가 있었음(2026-08-20 발견). 매 요청 이 시점에
        // 컨텍스트를 먼저 비워서, 인증 여부는 오직 "이번 요청에 실린 유효한 JWT"로만 판단하게 함.
        SecurityContextHolder.clearContext();

        String token = resolveToken(request);

        // 유효한 토큰 형식이 헤더에 존재할 경우에만 인증
        if (token != null) {
            // 토큰이 만료 / 위조 시 미인증 상태로 진행
            try {
                JwtProvider.JwtPayload payload = jwtProvider.parseAccessToken(token);

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        payload.userId(),
                        null, // 패스워드는 null 처리
                        List.of(new SimpleGrantedAuthority("ROLE_" + payload.role()))
                );
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (JwtException | IllegalArgumentException e) {
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(HEADER);
        if (header != null && header.startsWith(PREFIX)) {
            return header.substring(PREFIX.length());  // "Bearer " 접두사 제거 후 토큰 값만 추출
        }
        return null;  // 헤더 없거나 "Bearer "로 안 시작하면 미인증 요청으로 처리
    }
}

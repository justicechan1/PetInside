package org.example.petinside.global.security.oauth2;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final OAuth2AuthCodeStore oAuth2AuthCodeStore;

    // 프론트엔드의 OAuth2 콜백 수신용 리다이렉트 URI
    @Value("${app.oauth2.redirect-uri}")
    private String redirectUri;

    // 구글 인증 성공 후 토큰을 URL에 직접 노출하지 않기 위해 1회용 code만 발급해 전달
    // (토큰이 브라우저 히스토리/서버 접근 로그/Referer에 남는 것을 방지)
    // 프론트는 이 code를 /api/v1/auth/oauth2/exchange로 즉시 교환해 실제 토큰을 받는다.
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                         Authentication authentication) throws IOException {
        CustomOidcUser principal = (CustomOidcUser) authentication.getPrincipal();
        String code = oAuth2AuthCodeStore.issue(principal.getUserId());

        // API 인증은 JWT 헤더로만 해야 하므로, OAuth2Login 과정에서 생긴 세션은 즉시 폐기한다.
        // (세션을 살려두면 같은 브라우저의 이후 요청이 Authorization 헤더 없이도 세션 쿠키만으로 인증돼버린다)
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();

        String targetUrl = UriComponentsBuilder.fromUriString(redirectUri)
                .queryParam("code", code)
                .build()
                .toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
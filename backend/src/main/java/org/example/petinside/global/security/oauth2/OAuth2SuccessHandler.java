package org.example.petinside.global.security.oauth2;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.example.petinside.domain.auth.dto.LoginResponse;
import org.example.petinside.domain.auth.service.AuthService;
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

    private final AuthService authService;

    // 프론트엔드의 OAuth2 콜백 수신용 리다이렉트 URI
    @Value("${app.oauth2.redirect-uri}")
    private String redirectUri;

    // 구글 인증 성공 후 accessToken/refreshToken을 프론트 콜백 URL 쿼리파라미터로 전달
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                         Authentication authentication) throws IOException {
        CustomOidcUser principal = (CustomOidcUser) authentication.getPrincipal();
        LoginResponse tokens = authService.socialLogin(principal.getUserId());

        // API 인증은 JWT 헤더로만 해야 하므로, OAuth2Login 과정에서 생긴 세션은 즉시 폐기한다.
        // (세션을 살려두면 같은 브라우저의 이후 요청이 Authorization 헤더 없이도 세션 쿠키만으로 인증돼버림)
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();

        String targetUrl = UriComponentsBuilder.fromUriString(redirectUri)
                .queryParam("accessToken", tokens.accessToken())
                .queryParam("refreshToken", tokens.refreshToken())
                .build()
                .toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
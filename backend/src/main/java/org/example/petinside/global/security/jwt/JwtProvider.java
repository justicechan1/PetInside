package org.example.petinside.global.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
@RequiredArgsConstructor
public class JwtProvider {

    private static final String CLAIM_TYPE = "type";
    private static final String CLAIM_ROLE = "role";
    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";

    private final JwtProperties jwtProperties;

    private SecretKey secretKey() {
        return Keys.hmacShaKeyFor(jwtProperties.secret().getBytes());
    }

    public String createAccessToken(Long userId, String role) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim(CLAIM_TYPE, TYPE_ACCESS)
                .claim(CLAIM_ROLE, role)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + jwtProperties.accessTokenExpiration()))
                .signWith(secretKey())
                .compact();
    }

    public String createRefreshToken(Long userId) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim(CLAIM_TYPE, TYPE_REFRESH)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + jwtProperties.refreshTokenExpiration()))
                .signWith(secretKey())
                .compact();
    }

    public JwtPayload parseAccessToken(String token) {
        return parse(token, TYPE_ACCESS);
    }

    public JwtPayload parseRefreshToken(String token) {
        return parse(token, TYPE_REFRESH);
    }

    private JwtPayload parse(String token, String expectedType) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        String type = claims.get(CLAIM_TYPE, String.class);
        if (!expectedType.equals(type)) {
            throw new io.jsonwebtoken.JwtException("잘못된 토큰 타입입니다.");
        }

        Long userId = Long.valueOf(claims.getSubject());
        String role = claims.get(CLAIM_ROLE, String.class);
        return new JwtPayload(userId, role);
    }

    public record JwtPayload(Long userId, String role) {
    }
}

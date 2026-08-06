package org.example.petinside.domain.auth.dto;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        String tokenType
) {
}

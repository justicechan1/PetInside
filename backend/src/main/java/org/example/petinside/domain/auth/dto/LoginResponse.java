package org.example.petinside.domain.auth.dto;

public record LoginResponse(
        Long userId,
        String username,
        String nickname
) {
}

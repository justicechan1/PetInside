package org.example.petinside.domain.admin.dto;

import org.example.petinside.domain.user.entity.User;

import java.time.LocalDateTime;

public record UserSummaryResponse(
        Long id,
        String username,
        String nickname,
        String role,
        LocalDateTime createdAt
) {
    public static UserSummaryResponse from(User user) {
        return new UserSummaryResponse(
                user.getId(), user.getUsername(), user.getNickname(),
                user.getRole(), user.getCreatedAt()
        );
    }
}

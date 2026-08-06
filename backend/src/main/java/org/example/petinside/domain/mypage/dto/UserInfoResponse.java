package org.example.petinside.domain.mypage.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

// 내 정보 조회 응답 - 민감 정보(password) 제외하고 필요한 필드만 담아 반환
@Getter
@AllArgsConstructor
public class UserInfoResponse {
    private Long id;
    private String username;
    private String nickname;
    private String profileImageUrl;
    private String role;
    private LocalDateTime createdAt;
    private String provider; // 소셜 로그인이면 "GOOGLE" 등, 일반 로그인이면 null
}

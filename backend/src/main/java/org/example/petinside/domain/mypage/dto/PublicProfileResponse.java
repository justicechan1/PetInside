package org.example.petinside.domain.mypage.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

// 타 사용자 공개 프로필 조회 응답(F-33) - username/password/role 등 민감 정보는 제외
@Getter
@AllArgsConstructor
public class PublicProfileResponse {
    private Long id;
    private String nickname;
    private String profileImageUrl;
    private long postCount;
    private List<String> badges; // 인증뱃지 도메인 미구현이라 항상 빈 배열
    private String membershipTier; // 활성 구독이 있으면 "SUBSCRIBER", 없으면 null
}

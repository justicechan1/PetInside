package org.example.petinside.domain.mypage.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

// 내 게시글 목록 응답 - 목록 카드에 필요한 정보만 담아 반환 (본문 내용 제외)
@Getter
@AllArgsConstructor
public class MyPostResponse {
    private Long id;
    private String category;
    private String title;
    private String thumbnailImageUrl; // sortOrder가 0인 첫 번째 이미지
    private LocalDateTime createdAt;
}

package org.example.petinside.domain.post.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Schema(description = "게시글 목록 항목 응답")
@Getter
@NoArgsConstructor
@AllArgsConstructor // 👈 public 생성자 자동 생성
public class PostListResponse {
    @Schema(description = "게시글 ID")
    private Long id;
    @Schema(description = "게시글 제목")
    private String title;
    @Schema(description = "게시글 카테고리")
    private String category;
    @Schema(description = "작성자 ID")
    private Long authorId;
    @Schema(description = "작성자 닉네임")
    private String authorNickname;
    @Schema(description = "작성자 프로필 사진 URL, 없으면 null")
    private String authorProfileImageUrl;
    @Schema(description = "조회수")
    private int viewCount;
    @Schema(description = "댓글 수")
    private int commentCount;
    @Schema(description = "대표(첫 번째) 이미지 URL, 없으면 null")
    private String thumbnailUrl;
    @Schema(description = "작성 일시")
    private LocalDateTime createdAt;
}
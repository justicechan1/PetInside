package org.example.petinside.domain.post.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "게시글 상세 응답")
@Getter
@Builder
public class PostDetailResponse {
    @Schema(description = "게시글 ID")
    private Long id;
    @Schema(description = "게시글 제목")
    private String title;
    @Schema(description = "게시글 내용")
    private String content;
    @Schema(description = "게시글 카테고리")
    private String category;
    @Schema(description = "조회수 (조회 시마다 1 증가)")
    private int viewCount;
    @Schema(description = "작성자 닉네임")
    private String authorNickname;
    @Schema(description = "첨부 이미지 URL 목록")
    private List<String> imageUrls;
    @Schema(description = "작성 일시")
    private LocalDateTime createdAt;
    @Schema(description = "수정 일시")
    private LocalDateTime updatedAt;
}
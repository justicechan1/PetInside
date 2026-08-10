package org.example.petinside.domain.post.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.List;

@Schema(description = "게시글 수정 요청")
@Getter
@NoArgsConstructor
public class PostUpdateRequest {

    @Schema(description = "게시글 카테고리", example = "자유")
    @NotBlank(message = "카테고리는 필수입니다.")
    private String category;

    @Schema(description = "게시글 제목", example = "우리 강아지 자랑 좀 할게요")
    @NotBlank(message = "제목은 필수입니다.")
    private String title;

    @Schema(description = "게시글 내용", example = "오늘 산책 다녀왔어요")
    @NotBlank(message = "내용은 필수입니다.")
    private String content;

    @Schema(description = "첨부 이미지 URL 목록 (선택, 전달된 목록으로 기존 이미지를 전부 대체)")
    private List<String> imageUrls;
}
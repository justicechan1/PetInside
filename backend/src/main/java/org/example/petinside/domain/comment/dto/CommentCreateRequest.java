package org.example.petinside.domain.comment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "댓글/대댓글 작성 요청")
@Getter
@NoArgsConstructor
public class CommentCreateRequest {

    @Schema(description = "댓글 내용", example = "귀엽네요!")
    @NotBlank(message = "댓글 내용은 필수입니다.")
    private String content;

    @Schema(description = "부모 댓글 ID (대댓글일 때만 지정, 최상위 댓글이면 null). "
            + "이미 대댓글인 댓글을 부모로 지정하면 400 에러", example = "null")
    private Long parentId;
}
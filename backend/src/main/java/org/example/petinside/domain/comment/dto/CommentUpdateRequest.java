package org.example.petinside.domain.comment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "댓글/대댓글 수정 요청")
@Getter
@NoArgsConstructor
public class CommentUpdateRequest {

    @Schema(description = "수정할 댓글 내용", example = "수정된 댓글입니다.")
    @NotBlank(message = "댓글 내용은 필수입니다.")
    private String content;
}

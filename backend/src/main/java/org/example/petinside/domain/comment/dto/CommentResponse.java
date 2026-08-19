package org.example.petinside.domain.comment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "댓글 응답 (최상위 댓글은 children에 대댓글 목록을 포함)")
@Getter
@Builder
@AllArgsConstructor
public class CommentResponse {
    @Schema(description = "댓글 ID")
    private Long id;
    @Schema(description = "댓글 내용")
    private String content;
    @Schema(description = "작성자 ID")
    private Long authorId;
    @Schema(description = "작성자 닉네임")
    private String authorNickname;
    @Schema(description = "작성자 프로필 사진 URL, 없으면 null")
    private String authorProfileImageUrl;
    @Schema(description = "작성 일시")
    private LocalDateTime createdAt;
    @Schema(description = "대댓글 목록 (대댓글 자신의 children은 항상 null, 2단계까지만 지원)")
    private List<CommentResponse> children;
}
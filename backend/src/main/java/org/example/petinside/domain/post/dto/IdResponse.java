package org.example.petinside.domain.post.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Schema(description = "생성/수정/삭제된 리소스의 ID만 담는 공용 응답")
@Getter
@AllArgsConstructor
public class IdResponse {
    @Schema(description = "대상 리소스(게시글/댓글) ID")
    private Long id;

    public static IdResponse from(Long id) {
        return new IdResponse(id);
    }
}
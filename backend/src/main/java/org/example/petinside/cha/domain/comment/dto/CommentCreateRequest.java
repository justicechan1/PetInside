package org.example.petinside.cha.domain.comment.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CommentCreateRequest {
    private String content;
    private Long parentId; // 일반 댓글은 null, 대댓글은 부모 댓글 ID
}
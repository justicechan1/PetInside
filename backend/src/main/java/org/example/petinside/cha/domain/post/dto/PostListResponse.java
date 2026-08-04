package org.example.petinside.cha.domain.post.dto;

import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
@Builder
public class PostListResponse {
    private Long id;
    private String category;
    private String title;
    private String authorNickname;
    private LocalDateTime createdAt;
}
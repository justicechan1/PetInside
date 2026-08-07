package org.example.petinside.domain.post.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor // 👈 public 생성자 자동 생성
public class PostListResponse {
    private Long id;
    private String title;
    private String category;
    private String authorNickname;
    private int viewCount;
    private int commentCount;
    private String thumbnailUrl;
    private LocalDateTime createdAt;
}
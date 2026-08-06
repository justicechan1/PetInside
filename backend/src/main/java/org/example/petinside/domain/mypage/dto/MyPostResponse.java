package org.example.petinside.domain.mypage.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class MyPostResponse {
    private Long id;
    private String category;
    private String title;
    private String thumbnailImageUrl;
    private LocalDateTime createdAt;
}
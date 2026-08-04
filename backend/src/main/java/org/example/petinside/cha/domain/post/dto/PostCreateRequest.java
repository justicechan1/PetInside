package org.example.petinside.cha.domain.post.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.List;

@Getter
@NoArgsConstructor
public class PostCreateRequest {
    private String category;
    private String title;
    private String content;
    private List<String> imageUrls;
}
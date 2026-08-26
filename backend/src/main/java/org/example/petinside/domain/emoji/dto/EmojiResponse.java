package org.example.petinside.domain.emoji.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Schema(description = "이모지 응답")
@Getter
@AllArgsConstructor
public class EmojiResponse {
    @Schema(description = "이모지 ID")
    private Long id;
    @Schema(description = "이모지 이미지 URL")
    private String imageUrl;
    @Schema(description = "이모지 이름")
    private String name;
}

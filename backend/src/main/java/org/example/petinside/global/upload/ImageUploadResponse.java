package org.example.petinside.global.upload;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Schema(description = "로컬 디스크 이미지 업로드 응답")
@Getter
@AllArgsConstructor
public class ImageUploadResponse {

    @Schema(description = "업로드된 이미지의 절대 URL. 게시글 등록/수정 시 imageUrls에 그대로 담아 전달",
            example = "http://localhost:8080/images/3f2c1e2e-....jpg")
    private String url;
}

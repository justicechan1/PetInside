package org.example.petinside.global.upload;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Schema(description = "S3 이미지 업로드용 presigned URL 발급 응답")
@Getter
@AllArgsConstructor
public class PresignedUrlResponse {

    @Schema(description = "여기로 파일을 PUT 요청으로 직접 업로드 (Body에 파일 바이너리만 담기, 유효시간 지나면 403)",
            example = "https://petinside-images.s3.ap-northeast-2.amazonaws.com/images/3f2c1e2e-....jpg?X-Amz-Algorithm=...")
    private String uploadUrl;

    @Schema(description = "업로드 완료 후 접근 가능한 최종 URL. 게시글 등록/수정 요청의 imageUrls에 그대로 담아 전달",
            example = "https://petinside-images.s3.ap-northeast-2.amazonaws.com/images/3f2c1e2e-....jpg")
    private String imageUrl;
}

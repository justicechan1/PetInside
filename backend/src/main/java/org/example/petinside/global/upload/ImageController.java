package org.example.petinside.global.upload;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.petinside.global.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// 게시글/프로필 등에서 공통으로 쓰는 S3 presigned URL 발급.
// 실제 파일은 이 서버를 거치지 않고 프론트가 uploadUrl로 S3에 직접 PUT 업로드한다.
@Tag(name = "이미지 업로드", description = "S3 presigned URL 발급 - 도메인 전반에서 재사용")
@RestController
@RequestMapping("/api/v1/images")
@RequiredArgsConstructor
public class ImageController {

    private final S3UploadService s3UploadService;

    @Operation(
            summary = "이미지 업로드용 presigned URL 발급",
            description = "S3에 파일을 직접 업로드할 수 있는 presigned URL을 발급합니다. 로그인이 필요합니다. "
                    + "응답의 uploadUrl로 파일을 PUT 요청 본문에 담아 직접 S3에 업로드하고, "
                    + "완료 후 imageUrl을 게시글 등록/수정 요청의 imageUrls에 그대로 담아 전달하면 됩니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "확장자 누락 또는 지원하지 않는 확장자(jpg/jpeg/png/gif/webp 외)")
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/presigned-url")
    public ResponseEntity<ApiResponse<PresignedUrlResponse>> getPresignedUrl(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "업로드할 파일의 확장자 (예: jpg, png)", example = "jpg")
            @RequestParam String extension
    ) {
        PresignedUrlResponse response = s3UploadService.createPresignedUrl(extension);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(HttpStatus.OK.value(), "presigned URL이 발급되었습니다.", response));
    }
}

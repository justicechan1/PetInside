package org.example.petinside.global.upload;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.petinside.global.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

// 게시글/프로필 등에서 공통으로 쓰는 이미지 업로드.
// 파일을 이 서버로 보내면 EC2 로컬 디스크(app.upload.dir)에 저장하고 /images/** 로 서빙되는 URL을 돌려준다.
@Tag(name = "이미지 업로드", description = "이미지 업로드 - 도메인 전반에서 재사용")
@RestController
@RequestMapping("/api/v1/images")
@RequiredArgsConstructor
public class ImageController {

    private final ImageUploadService imageUploadService;

    @Operation(
            summary = "이미지 업로드",
            description = "이미지 파일을 이 서버로 직접 업로드합니다. EC2 로컬 디스크에 저장하고 접근 가능한 절대 URL을 반환합니다. "
                    + "로그인이 필요하며, 반환된 url을 게시글 등록/수정 요청의 imageUrls에 그대로 담아 전달하면 됩니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "파일이 없거나 지원하지 않는 확장자(jpg/jpeg/png/gif/webp 외)")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<ImageUploadResponse>> uploadImage(
            @AuthenticationPrincipal Long userId,
            @RequestParam("file") MultipartFile file
    ) {
        String filename = imageUploadService.store(file);
        String url = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/images/")
                .path(filename)
                .toUriString();

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED.value(), "이미지가 업로드되었습니다.", new ImageUploadResponse(url)));
    }
}

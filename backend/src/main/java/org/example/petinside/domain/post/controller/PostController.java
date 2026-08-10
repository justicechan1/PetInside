package org.example.petinside.domain.post.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.petinside.domain.post.dto.IdResponse;
import org.example.petinside.domain.post.dto.PostCreateRequest;
import org.example.petinside.domain.post.dto.PostDetailResponse;
import org.example.petinside.domain.post.dto.PostListResponse;
import org.example.petinside.domain.post.dto.PostUpdateRequest;
import org.example.petinside.domain.post.service.PostService;
import org.example.petinside.global.response.ApiResponse;
import org.example.petinside.global.response.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// 게시판 컨트롤러  F-10/F-11~F-14
// 인증은 JWT 필터, 본인/관리자 권한 검증은 PostService가 403/404 예외로 처리
@Tag(name = "게시글", description = "게시판 게시글 등록/조회/수정/삭제 API")
@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;


    // F-11/13 목록 조회 (검색/페이징) — 비회원 가능. category/keyword는 선택값.
    @Operation(
            summary = "게시글 목록 조회",
            description = "게시글 목록을 페이징하여 조회합니다. 비회원도 호출 가능합니다. "
                    + "category/keyword는 선택값이며, 기본 정렬은 createdAt 내림차순입니다."
    )
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<PostListResponse>>> getPostList(
            @Parameter(description = "게시글 카테고리 (선택, 미지정 시 전체)") @RequestParam(required = false) String category,
            @Parameter(description = "제목/내용 검색 키워드 (선택)") @RequestParam(required = false) String keyword,
            @Parameter(description = "페이지/정렬 정보 (기본: size=10, createdAt DESC)")
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        PageResponse<PostListResponse> response = postService.getPostList(category, keyword, pageable);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "성공", response));
    }

    // 상세 조회 — 비회원 가능. 조회할 때마다 viewCount 1 증가. 404: 없거나 삭제된 게시글.
    @Operation(
            summary = "게시글 상세 조회",
            description = "게시글 상세 정보를 조회합니다. 비회원도 호출 가능하며, 호출할 때마다 조회수(viewCount)가 1 증가합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "게시글이 없거나 삭제된 경우")
    })
    @GetMapping("/{postId}")
    public ResponseEntity<ApiResponse<PostDetailResponse>> getPostDetail(
            @Parameter(description = "조회할 게시글 ID") @PathVariable Long postId
    ) {
        PostDetailResponse response = postService.getPostDetail(postId);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "성공", response));
    }

    // F-10 게시글 등록 — 로그인 필요. userId는 토큰(@AuthenticationPrincipal)에서 추출. 400: 필수값 누락.
    @Operation(
            summary = "게시글 등록",
            description = "로그인한 사용자가 새 게시글을 등록합니다. 작성자는 인증 토큰에서 추출되며 요청 본문에 포함하지 않습니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "카테고리/제목/내용 등 필수값 누락")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping
    public ResponseEntity<ApiResponse<IdResponse>> createPost(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody PostCreateRequest request
    ) {
        IdResponse response = postService.createPost(userId, request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED.value(), "게시글이 등록되었습니다.", response));
    }

    // F-14 게시글 수정 — 작성자 본인만(관리자도 타인 글 수정 불가). 403: 작성자 아님, 404: 없거나 삭제된 게시글.
    @Operation(
            summary = "게시글 수정",
            description = "게시글을 수정합니다. 작성자 본인만 가능하며, 관리자도 타인의 게시글은 수정할 수 없습니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "작성자 본인이 아닌 경우"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "게시글이 없거나 삭제된 경우")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/{postId}")
    public ResponseEntity<ApiResponse<IdResponse>> updatePost(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "수정할 게시글 ID") @PathVariable Long postId,
            @Valid @RequestBody PostUpdateRequest request
    ) {
        IdResponse response = postService.updatePost(userId, postId, request);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "게시글이 수정되었습니다.", response));
    }

    // F-14 게시글 삭제(soft delete) — 작성자 또는 관리자(제재). 403: 둘 다 아님, 404: 없거나 이미 삭제된 게시글.
    @Operation(
            summary = "게시글 삭제",
            description = "게시글을 소프트 삭제합니다. 작성자 본인 또는 관리자(제재 목적)만 가능합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "작성자 본인도 관리자도 아닌 경우"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "게시글이 없거나 이미 삭제된 경우")
    })
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/{postId}")
    public ResponseEntity<ApiResponse<IdResponse>> deletePost(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "삭제할 게시글 ID") @PathVariable Long postId
    ) {
        IdResponse response = postService.deletePost(userId, postId);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "게시글이 삭제되었습니다.", response));
    }
}

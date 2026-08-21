package org.example.petinside.domain.like.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.petinside.domain.like.dto.LikeResponse;
import org.example.petinside.domain.like.service.LikeService;
import org.example.petinside.global.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

// 게시글/댓글 좋아요 컨트롤러
// 토글(POST)은 로그인 필요, 상태 조회(GET)는 게시글/댓글 상세와 마찬가지로 비회원도 개수는 볼 수 있어야 하므로 인증 선택
@Tag(name = "좋아요", description = "게시글/댓글 좋아요 토글 및 상태 조회 API")
@RestController
@RequiredArgsConstructor
public class LikeController {

    private final LikeService likeService;

    @Operation(
            summary = "게시글 좋아요 토글",
            description = "이미 좋아요를 눌렀으면 취소(-1), 안 눌렀으면 등록(+1)합니다. 작성자 본인도 자신의 게시글에 좋아요를 누를 수 있습니다."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/api/v1/posts/{postId}/likes")
    public ResponseEntity<ApiResponse<LikeResponse>> togglePostLike(
            @Parameter(description = "좋아요를 토글할 게시글 ID") @PathVariable Long postId,
            @AuthenticationPrincipal Long userId) {
        LikeResponse response = likeService.togglePostLike(postId, userId);
        String message = response.isLiked() ? "좋아요를 눌렀습니다." : "좋아요를 취소했습니다.";
        return ResponseEntity.ok(ApiResponse.of(200, message, response));
    }

    @Operation(
            summary = "게시글 좋아요 상태 조회",
            description = "현재 사용자가 좋아요를 눌렀는지 여부와 총 좋아요 수를 조회합니다. 비회원은 liked=false로 개수만 조회됩니다."
    )
    @GetMapping("/api/v1/posts/{postId}/likes/me")
    public ResponseEntity<ApiResponse<LikeResponse>> getPostLikeStatus(
            @Parameter(description = "조회할 게시글 ID") @PathVariable Long postId,
            @AuthenticationPrincipal(errorOnInvalidType = false) Long userId) {
        return ResponseEntity.ok(ApiResponse.success(200, "조회 성공", likeService.getPostLikeStatus(postId, userId)));
    }

    @Operation(
            summary = "댓글 좋아요 토글",
            description = "이미 좋아요를 눌렀으면 취소(-1), 안 눌렀으면 등록(+1)합니다. 작성자 본인도 자신의 댓글에 좋아요를 누를 수 있습니다."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/api/v1/comments/{commentId}/likes")
    public ResponseEntity<ApiResponse<LikeResponse>> toggleCommentLike(
            @Parameter(description = "좋아요를 토글할 댓글 ID") @PathVariable Long commentId,
            @AuthenticationPrincipal Long userId) {
        LikeResponse response = likeService.toggleCommentLike(commentId, userId);
        String message = response.isLiked() ? "좋아요를 눌렀습니다." : "좋아요를 취소했습니다.";
        return ResponseEntity.ok(ApiResponse.of(200, message, response));
    }

    @Operation(
            summary = "댓글 좋아요 상태 조회",
            description = "현재 사용자가 좋아요를 눌렀는지 여부와 총 좋아요 수를 조회합니다. 비회원은 liked=false로 개수만 조회됩니다."
    )
    @GetMapping("/api/v1/comments/{commentId}/likes/me")
    public ResponseEntity<ApiResponse<LikeResponse>> getCommentLikeStatus(
            @Parameter(description = "조회할 댓글 ID") @PathVariable Long commentId,
            @AuthenticationPrincipal(errorOnInvalidType = false) Long userId) {
        return ResponseEntity.ok(ApiResponse.success(200, "조회 성공", likeService.getCommentLikeStatus(commentId, userId)));
    }
}

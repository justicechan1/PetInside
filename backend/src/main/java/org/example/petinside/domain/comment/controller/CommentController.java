package org.example.petinside.domain.comment.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.petinside.domain.comment.dto.CommentCreateRequest;
import org.example.petinside.domain.comment.dto.CommentResponse;
import org.example.petinside.domain.comment.dto.CommentUpdateRequest;
import org.example.petinside.domain.comment.service.CommentService;
import org.example.petinside.domain.post.dto.IdResponse;
import org.example.petinside.global.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// 댓글/대댓글 컨트롤러  F-12
// 목록 조회는 /posts/{postId}/comments, 삭제는 /comments/{commentId}라 URI를 클래스 레벨로 묶지 않음
@Tag(name = "댓글", description = "게시글 댓글/대댓글(최대 2단계) 등록/조회/수정/삭제 API")
@RestController
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    // F-12 목록 조회 — 비회원 가능. 최상위 댓글 아래 children으로 대댓글 트리 구성. 404: 없는 게시글.
    @Operation(
            summary = "댓글 목록 조회",
            description = "게시글의 댓글을 조회합니다. 비회원도 호출 가능하며, 최상위 댓글 아래 children으로 대댓글이 함께 반환됩니다(2단계까지)."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "게시글이 없는 경우")
    })
    @GetMapping("/api/v1/posts/{postId}/comments")
    public ResponseEntity<ApiResponse<List<CommentResponse>>> getComments(
            @Parameter(description = "댓글을 조회할 게시글 ID") @PathVariable Long postId
    ) {
        List<CommentResponse> response = commentService.getCommentsByPostId(postId);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "성공", response));
    }

    // F-12 댓글/대댓글 작성 — 로그인 필요. parentId 있으면 대댓글. 404: 없는 게시글/ 댓글.
    @Operation(
            summary = "댓글/대댓글 작성",
            description = "게시글에 댓글을 작성합니다. parentId를 지정하면 대댓글이 되며, "
                    + "대댓글에는 다시 답글을 달 수 없어 대댓글 depth는 2단계로 제한됩니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "내용 누락 또는 대댓글에 답글을 시도한 경우"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "게시글 또는 부모 댓글이 없는 경우")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/api/v1/posts/{postId}/comments")
    public ResponseEntity<ApiResponse<IdResponse>> createComment(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "댓글을 작성할 게시글 ID") @PathVariable Long postId,
            @Valid @RequestBody CommentCreateRequest request
    ) {
        IdResponse response = commentService.createComment(userId, postId, request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED.value(), "댓글이 등록되었습니다.", response));
    }

    // F-12 댓글/대댓글 수정 — 작성자 본인만. 403: 작성자 아님, 404: 없거나 삭제된 댓글.
    @Operation(
            summary = "댓글/대댓글 수정",
            description = "댓글 또는 대댓글의 내용을 수정합니다. 작성자 본인만 가능합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "작성자 본인이 아닌 경우"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "댓글이 없거나 삭제된 경우")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/api/v1/comments/{commentId}")
    public ResponseEntity<ApiResponse<IdResponse>> updateComment(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "수정할 댓글 ID") @PathVariable Long commentId,
            @Valid @RequestBody CommentUpdateRequest request
    ) {
        IdResponse response = commentService.updateComment(userId, commentId, request);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "댓글이 수정되었습니다.", response));
    }

    // F-12 댓글 삭제 — 작성자 본인만. 관리자 강제삭제는
    @Operation(
            summary = "댓글/대댓글 삭제",
            description = "댓글 또는 대댓글을 삭제합니다. 작성자 본인만 가능합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "작성자 본인이 아닌 경우"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "댓글이 없거나 이미 삭제된 경우")
    })
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/api/v1/comments/{commentId}")
    public ResponseEntity<ApiResponse<IdResponse>> deleteComment(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "삭제할 댓글 ID") @PathVariable Long commentId
    ) {
        IdResponse response = commentService.deleteComment(userId, commentId);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "댓글이 삭제되었습니다.", response));
    }
}

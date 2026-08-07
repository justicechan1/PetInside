package org.example.petinside.domain.comment.controller;

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
@RestController
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    // F-12 목록 조회 — 비회원 가능. 최상위 댓글 아래 children으로 대댓글 트리 구성. 404: 없는 게시글.
    @GetMapping("/api/v1/posts/{postId}/comments")
    public ResponseEntity<ApiResponse<List<CommentResponse>>> getComments(@PathVariable Long postId) {
        List<CommentResponse> response = commentService.getCommentsByPostId(postId);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "성공", response));
    }

    // F-12 댓글/대댓글 작성 — 로그인 필요. parentId 있으면 대댓글. 404: 없는 게시글/ 댓글.
    @PostMapping("/api/v1/posts/{postId}/comments")
    public ResponseEntity<ApiResponse<IdResponse>> createComment(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long postId,
            @Valid @RequestBody CommentCreateRequest request
    ) {
        IdResponse response = commentService.createComment(userId, postId, request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED.value(), "댓글이 등록되었습니다.", response));
    }

    // F-12 댓글/대댓글 수정 — 작성자 본인만. 403: 작성자 아님, 404: 없거나 삭제된 댓글.
    @PutMapping("/api/v1/comments/{commentId}")
    public ResponseEntity<ApiResponse<IdResponse>> updateComment(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long commentId,
            @Valid @RequestBody CommentUpdateRequest request
    ) {
        IdResponse response = commentService.updateComment(userId, commentId, request);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "댓글이 수정되었습니다.", response));
    }

    // F-12 댓글 삭제 — 작성자 본인만. 관리자 강제삭제는
    @DeleteMapping("/api/v1/comments/{commentId}")
    public ResponseEntity<ApiResponse<IdResponse>> deleteComment(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long commentId
    ) {
        IdResponse response = commentService.deleteComment(userId, commentId);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "댓글이 삭제되었습니다.", response));
    }
}

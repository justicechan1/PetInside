package org.example.petinside.domain.admin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.petinside.global.response.ApiResponse;
import org.example.petinside.domain.admin.dto.*;
import org.example.petinside.domain.admin.service.AdminService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;


@Tag(name = "관리자", description = "관리자 전용 API")
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    // F-15
    @Operation(summary = "권한 부여", description = "특정 회원을 ADMIN으로 변경")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "권한 변경 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "ADMIN 권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "대상 회원이 존재하지 않음")
    })
    @PutMapping("/users/{userId}/role")
    public ResponseEntity<ApiResponse<RoleUpdateResponse>> updateRole(
            @Parameter(description = "권한을 변경할 회원의 ID", example = "1")
            @PathVariable Long userId,
            @Valid @RequestBody RoleUpdateRequest request
    ) {
        RoleUpdateResponse response = adminService.updateRole(userId, request.role());
        return ResponseEntity.ok(ApiResponse.of(200, "권한이 변경되었습니다.", response));
    }

    // F-16
    @Operation(summary = "게시글 삭제", description = "관리자가 게시글을 삭제 처리")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "게시글 삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증이 필요합니다"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "ADMIN 권한이 없습니다"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않거나 이미 삭제된 게시글입니다")
    })
    @DeleteMapping("/posts/{postId}")
    public ResponseEntity<ApiResponse<DeleteResponse>> deletePost(
            @Parameter(description = "삭제할 게시글의 ID", example = "1")
            @PathVariable Long postId) {
        DeleteResponse response = adminService.deletePost(postId);
        return ResponseEntity.ok(ApiResponse.of(200, "게시글이 삭제되었습니다.", response));
    }

    // F-17
    @Operation(summary = "회원 목록 조회", description = "전체 회원 목록을 페이징하여 조회")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증이 필요합니다"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "ADMIN 권한이 없습니다")
    })
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<Page<UserSummaryResponse>>> getUsers(
            @Parameter(description = "페이지 번호(0부터 시작) 및 페이지 크기")
            @PageableDefault(size = 10) Pageable pageable
    ) {
        Page<UserSummaryResponse> response = adminService.getUsers(pageable);
        return ResponseEntity.ok(ApiResponse.of(200, "조회 성공", response));
    }

    // F-18
    @Operation(summary = "서비스 통계 조회", description = "일일 가입자·게시글 수·접속자 수 통계 조회")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증이 필요합니다"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "ADMIN 권한이 없습니다")
    })
    @GetMapping("/statistics/daily")
    public ResponseEntity<ApiResponse<DailyStatisticsResponse>> getDailyStatistics() {
        DailyStatisticsResponse response = adminService.getDailyStatistics();
        return ResponseEntity.ok(ApiResponse.of(200, "조회 성공", response));
    }

    // F-19
    @Operation(summary = "댓글 삭제", description = "관리자가 댓글을 삭제 처리")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "댓글 삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증이 필요합니다"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "ADMIN 권한이 없습니다"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않거나 이미 삭제된 댓글입니다")
    })
    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<ApiResponse<DeleteResponse>> deleteComment(
            @io.swagger.v3.oas.annotations.Parameter(description = "삭제할 댓글의 ID", example = "1")
            @PathVariable Long commentId) {
        DeleteResponse response = adminService.deleteComment(commentId);
        return ResponseEntity.ok(ApiResponse.of(200, "댓글이 삭제되었습니다.", response));
    }
}

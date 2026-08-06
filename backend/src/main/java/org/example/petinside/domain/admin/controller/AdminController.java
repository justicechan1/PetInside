package org.example.petinside.domain.admin.controller;

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

// admin/AdminController.java
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")   // 클래스 전체에 ADMIN 권한 강제 — 권한 매트릭스 F-15~F-19 전부 ADMIN 전용
public class AdminController {

    private final AdminService adminService;

    // F-15
    @PutMapping("/users/{userId}/role")
    public ResponseEntity<ApiResponse<RoleUpdateResponse>> updateRole(
            @PathVariable Long userId,
            @RequestBody RoleUpdateRequest request
    ) {
        RoleUpdateResponse response = adminService.updateRole(userId, request.role());
        return ResponseEntity.ok(ApiResponse.of(200, "권한이 변경되었습니다.", response));
    }

    // F-16
    @DeleteMapping("/posts/{postId}")
    public ResponseEntity<ApiResponse<DeleteResponse>> deletePost(@PathVariable Long postId) {
        DeleteResponse response = adminService.deletePost(postId);
        return ResponseEntity.ok(ApiResponse.of(200, "게시글이 삭제되었습니다.", response));
    }

    // F-17
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<Page<UserSummaryResponse>>> getUsers(
            @PageableDefault(size = 10) Pageable pageable
    ) {
        Page<UserSummaryResponse> response = adminService.getUsers(pageable);
        return ResponseEntity.ok(ApiResponse.of(200, "조회 성공", response));
    }

    // F-18
    @GetMapping("/statistics/daily")
    public ResponseEntity<ApiResponse<DailyStatisticsResponse>> getDailyStatistics() {
        DailyStatisticsResponse response = adminService.getDailyStatistics();
        return ResponseEntity.ok(ApiResponse.of(200, "조회 성공", response));
    }

    // F-19
    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<ApiResponse<DeleteResponse>> deleteComment(@PathVariable Long commentId) {
        DeleteResponse response = adminService.deleteComment(commentId);
        return ResponseEntity.ok(ApiResponse.of(200, "댓글이 삭제되었습니다.", response));
    }
}

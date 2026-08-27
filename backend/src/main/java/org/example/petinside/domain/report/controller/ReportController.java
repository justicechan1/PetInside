package org.example.petinside.domain.report.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.petinside.domain.post.dto.IdResponse;
import org.example.petinside.domain.report.dto.ReportCreateRequest;
import org.example.petinside.domain.report.service.ReportService;
import org.example.petinside.global.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "신고", description = "게시글/댓글/대댓글 신고 API")
@RestController
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    // F-43 신고 등록 — 로그인 필요. 본인 게시글/댓글은 신고 불가, 동일 대상 중복 신고 불가.
    @Operation(
            summary = "게시글/댓글/대댓글 신고",
            description = "게시글 또는 댓글(대댓글 포함)을 신고합니다. 본인이 작성한 글은 신고할 수 없으며, 처리 대기 중인 동일 대상을 중복 신고할 수 없습니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "본인 작성글 신고 또는 중복 신고인 경우"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "신고 대상이 없거나 이미 삭제된 경우")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/api/v1/reports")
    public ResponseEntity<ApiResponse<IdResponse>> createReport(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody ReportCreateRequest request
    ) {
        IdResponse response = reportService.createReport(userId, request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED.value(), "신고가 접수되었습니다.", response));
    }
}

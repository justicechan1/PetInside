package org.example.petinside.domain.notification.controller;

import io.portone.sdk.server.common.Country;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.petinside.domain.notification.dto.NotificationReadResponse;
import org.example.petinside.domain.notification.dto.NotificationResponse;
import org.example.petinside.domain.notification.service.NotificationService;
import org.example.petinside.global.response.ApiResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "알림", description = "알림 기능 API")
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;

    // F-34 알림 목록 조회
    @Operation(summary = "알림 목록 조회", description = "로그인한 사용자의 알림 목록을 조회")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증이 핊요합니다.")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<Page<NotificationResponse>>> getNotifications(
        @AuthenticationPrincipal Long userId,
        @PageableDefault(size = 10) Pageable pageable
    ) {
        Page<NotificationResponse> response = notificationService.getNotifications(userId, pageable);
        return ResponseEntity.ok(ApiResponse.of(200, "조회 성공", response));
    }

    // F-34 알림 목록 읽음 처리
    @Operation(summary = "알림 목록 읽음 처리", description = "특정 알림을 읽음 상태로 변경")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "읽음 처리 완료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증이 필요합니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "본인의 알림이 아닙니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 알림입니다.")
    })
    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<ApiResponse<NotificationReadResponse>> readNotification(
        @AuthenticationPrincipal Long userId,
        @Parameter(description = "읽음 처리할 알림 ID", example ="1")
        @PathVariable Long notificationId
    ) {
        NotificationReadResponse response = notificationService.markAsRead(userId, notificationId);
        return ResponseEntity.ok(ApiResponse.of(200, "읽음 처리되었습니다.", response));
        }
    }


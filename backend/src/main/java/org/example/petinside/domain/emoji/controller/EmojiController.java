package org.example.petinside.domain.emoji.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.petinside.domain.emoji.dto.EmojiResponse;
import org.example.petinside.domain.emoji.service.EmojiService;
import org.example.petinside.global.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// 이모지 컨트롤러 - F-26
@Tag(name = "이모지", description = "구독자 전용 이모지 카탈로그 조회 API")
@RestController
@RequestMapping("/api/v1/emojis")
@RequiredArgsConstructor
public class EmojiController {

    private final EmojiService emojiService;

    @Operation(
            summary = "보유 이모지 목록 조회",
            description = "로그인 사용자의 구독 상태가 ACTIVE면 이모지 카탈로그 전체를 반환합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "구독 미가입 또는 만료")
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<EmojiResponse>>> getMyEmojis(
            @AuthenticationPrincipal Long userId
    ) {
        List<EmojiResponse> response = emojiService.getMyEmojis(userId);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "성공", response));
    }
}

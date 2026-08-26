package org.example.petinside.domain.mypage.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.petinside.domain.mypage.dto.MyPostResponse;
import org.example.petinside.domain.mypage.dto.PublicProfileResponse;
import org.example.petinside.domain.mypage.service.MypageService;
import org.example.petinside.global.response.ApiResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "공개 프로필", description = "타 사용자의 공개 프로필/작성글 조회 API (F-33, 비회원 접근 가능)")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class PublicProfileController {

    private final MypageService mypageService;

    @Operation(summary = "공개 프로필 조회", description = "닉네임, 프로필 사진, 작성 게시글 수, 뱃지, 구독 상태를 반환합니다. 민감 정보는 포함하지 않습니다.")
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<PublicProfileResponse>> getPublicProfile(@PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.success(200, "조회 성공", mypageService.getPublicProfile(userId)));
    }

    @Operation(summary = "타 사용자 작성 게시글 목록 조회", description = "해당 사용자가 작성한 게시글을 최신순으로 조회합니다.")
    @GetMapping("/{userId}/posts")
    public ResponseEntity<ApiResponse<Page<MyPostResponse>>> getPublicPosts(
            @PathVariable Long userId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(200, "조회 성공", mypageService.getPublicPosts(userId, keyword, category, pageable)));
    }
}

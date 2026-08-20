package org.example.petinside.domain.mypage.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.petinside.domain.mypage.dto.MyPostResponse;
import org.example.petinside.domain.mypage.dto.NicknameUpdateRequest;
import org.example.petinside.domain.mypage.dto.PasswordUpdateRequest;
import org.example.petinside.domain.mypage.dto.ProfileImageUpdateRequest;
import org.example.petinside.domain.mypage.dto.ProfileLayoutUpdateRequest;
import org.example.petinside.domain.mypage.dto.UserInfoResponse;
import org.example.petinside.domain.mypage.service.MypageService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.example.petinside.global.response.ApiResponse;

@Tag(name = "마이페이지", description = "내 정보 조회 및 수정 API (JWT 인증 필요)")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users/me")
public class MypageController {

    private final MypageService mypageService;

    @Operation(summary = "내 정보 조회", description = "로그인한 사용자의 닉네임, 프로필 이미지, 소셜 로그인 여부 등을 반환합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<UserInfoResponse>> getMyInfo(@AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(ApiResponse.success(200, "조회 성공", mypageService.getMyInfo(userId)));
    }

    @Operation(summary = "닉네임 변경", description = "닉네임을 변경합니다. 다른 사용자가 사용 중인 닉네임은 사용할 수 없습니다.")
    @PatchMapping("/nickname")
    public ResponseEntity<ApiResponse<Void>> updateNickname(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody NicknameUpdateRequest request) {
        mypageService.updateNickname(userId, request);
        return ResponseEntity.ok(ApiResponse.success(200, "닉네임이 변경되었습니다."));
    }

    @Operation(summary = "비밀번호 변경", description = "현재 비밀번호를 검증한 후 새 비밀번호로 변경합니다. 소셜 로그인 사용자는 사용할 수 없습니다.")
    @PatchMapping("/password")
    public ResponseEntity<ApiResponse<Void>> updatePassword(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody PasswordUpdateRequest request) {
        mypageService.updatePassword(userId, request);
        return ResponseEntity.ok(ApiResponse.success(200, "비밀번호가 변경되었습니다."));
    }

    @Operation(summary = "프로필 이미지 변경", description = "프로필 이미지 URL을 변경합니다. 이미지는 클라이언트에서 스토리지에 업로드 후 URL을 전달합니다.")
    @PatchMapping("/profile-image")
    public ResponseEntity<ApiResponse<Void>> updateProfileImage(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody ProfileImageUpdateRequest request) {
        mypageService.updateProfileImage(userId, request);
        return ResponseEntity.ok(ApiResponse.success(200, "프로필 이미지가 변경되었습니다."));
    }

    @Operation(summary = "피드 레이아웃 변경", description = "내 게시글 탭의 레이아웃을 GRID 또는 LIST로 저장합니다.")
    @PatchMapping("/profile-layout")
    public ResponseEntity<ApiResponse<Void>> updateProfileLayout(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody ProfileLayoutUpdateRequest request) {
        mypageService.updateProfileLayout(userId, request);
        return ResponseEntity.ok(ApiResponse.success(200, "레이아웃이 변경되었습니다."));
    }

    @Operation(summary = "내 게시글 목록 조회", description = "내가 작성한 게시글을 최신순으로 조회합니다. category(QNA/BOAST)와 keyword(제목 검색)로 필터링할 수 있습니다.")
    @GetMapping("/posts")
    public ResponseEntity<ApiResponse<Page<MyPostResponse>>> getMyPosts(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(200, "조회 성공", mypageService.getMyPosts(userId, keyword, category, pageable)));
    }
}

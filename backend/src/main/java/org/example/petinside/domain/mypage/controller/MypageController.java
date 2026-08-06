package org.example.petinside.domain.mypage.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.petinside.domain.mypage.dto.MyPostResponse;
import org.example.petinside.domain.mypage.dto.NicknameUpdateRequest;
import org.example.petinside.domain.mypage.dto.PasswordUpdateRequest;
import org.example.petinside.domain.mypage.dto.ProfileImageUpdateRequest;
import org.example.petinside.domain.mypage.dto.UserInfoResponse;
import org.example.petinside.domain.mypage.service.MypageService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.example.petinside.global.response.ApiResponse;

// 마이페이지 관련 API 엔드포인트 - /api/v1/users/me 하위 모든 요청 처리
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users/me")
public class MypageController {

    private final MypageService mypageService;

    // [F-05~08] @AuthenticationPrincipal: JWT 필터에서 꺼낸 로그인 사용자 ID를 자동 주입
    @GetMapping
    public ResponseEntity<ApiResponse<UserInfoResponse>> getMyInfo(@AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(ApiResponse.success(200, "조회 성공", mypageService.getMyInfo(userId)));
    }

    // [F-05] 닉네임 변경 - @Valid로 빈 값 자동 검증
    @PutMapping("/nickname")
    public ResponseEntity<ApiResponse<Void>> updateNickname(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody NicknameUpdateRequest request) {
        mypageService.updateNickname(userId, request);
        return ResponseEntity.ok(ApiResponse.success(200, "닉네임이 변경되었습니다."));
    }

    // [F-06] 비밀번호 변경 - 기존 비밀번호 검증 후 새 비밀번호로 업데이트
    @PutMapping("/password")
    public ResponseEntity<ApiResponse<Void>> updatePassword(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody PasswordUpdateRequest request) {
        mypageService.updatePassword(userId, request);
        return ResponseEntity.ok(ApiResponse.success(200, "비밀번호가 변경되었습니다."));
    }

    // [F-07] 프로필 사진 변경 - 클라이언트가 S3에 업로드 후 받은 URL을 전달
    @PutMapping("/profile-image")
    public ResponseEntity<ApiResponse<Void>> updateProfileImage(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody ProfileImageUpdateRequest request) {
        mypageService.updateProfileImage(userId, request);
        return ResponseEntity.ok(ApiResponse.success(200, "프로필 이미지가 변경되었습니다."));
    }

    // [F-08] 내 게시글 목록 조회 - keyword(제목 검색), category(QNA/BOAST) 선택적 필터링
    @GetMapping("/posts")
    public ResponseEntity<ApiResponse<Page<MyPostResponse>>> getMyPosts(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(200, "조회 성공", mypageService.getMyPosts(userId, keyword, category, pageable)));
    }
}

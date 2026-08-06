package org.example.petinside.domain.mypage.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.petinside.domain.mypage.dto.MyPostResponse;
import org.example.petinside.domain.mypage.dto.NicknameUpdateRequest;
import org.example.petinside.domain.mypage.dto.PasswordUpdateRequest;
import org.example.petinside.domain.mypage.dto.UserInfoResponse;
import org.example.petinside.domain.mypage.service.MypageService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.example.petinside.global.response.ApiResponse;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users/me")
public class MypageController {

    private final MypageService mypageService;

    @GetMapping
    public ResponseEntity<ApiResponse<UserInfoResponse>> getMyInfo(@AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(ApiResponse.success(200, "조회 성공", mypageService.getMyInfo(userId)));
    }

    @PutMapping("/nickname")
    public ResponseEntity<ApiResponse<Void>> updateNickname(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody NicknameUpdateRequest request) {
        mypageService.updateNickname(userId, request);
        return ResponseEntity.ok(ApiResponse.success(200, "닉네임이 변경되었습니다."));
    }

    @PutMapping("/password")
    public ResponseEntity<ApiResponse<Void>> updatePassword(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody PasswordUpdateRequest request) {
        mypageService.updatePassword(userId, request);
        return ResponseEntity.ok(ApiResponse.success(200, "비밀번호가 변경되었습니다."));
    }

    @PutMapping("/profile-image")
    public ResponseEntity<ApiResponse<Void>> updateProfileImage(
            @AuthenticationPrincipal Long userId,
            @RequestParam MultipartFile file) {
        // 스토리지 업로드는 Auth 팀이랑 방식 맞춰야 함
        return ResponseEntity.ok(ApiResponse.success(200, "프로필 이미지가 변경되었습니다."));
    }

    @GetMapping("/posts")
    public ResponseEntity<ApiResponse<Page<MyPostResponse>>> getMyPosts(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(200, "조회 성공", mypageService.getMyPosts(userId, keyword, category, pageable)));
    }
}

package org.example.petinside.mypage.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.petinside.mypage.dto.MyPostResponse;
import org.example.petinside.mypage.dto.NicknameUpdateRequest;
import org.example.petinside.mypage.dto.PasswordUpdateRequest;
import org.example.petinside.mypage.dto.UserInfoResponse;
import org.example.petinside.mypage.service.MypageService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.example.petinside.common.response.ApiResponse;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users/me")
public class MypageController {

    private final MypageService mypageService;

    @GetMapping
    public ResponseEntity<ApiResponse<UserInfoResponse>> getMyInfo(@AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(ApiResponse.success(mypageService.getMyInfo(userId)));
    }

    @PutMapping("/nickname")
    public ResponseEntity<ApiResponse<Void>> updateNickname(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody NicknameUpdateRequest request) {
        mypageService.updateNickname(userId, request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PutMapping("/password")
    public ResponseEntity<ApiResponse<Void>> updatePassword(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody PasswordUpdateRequest request) {
        mypageService.updatePassword(userId, request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PutMapping("/profile-image")
    public ResponseEntity<ApiResponse<Void>> updateProfileImage(
            @AuthenticationPrincipal Long userId,
            @RequestParam MultipartFile file) {
        // 스토리지 업로드는 Auth 팀이랑 방식 맞춰야 함
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/posts")
    public ResponseEntity<ApiResponse<Page<MyPostResponse>>> getMyPosts(
            @AuthenticationPrincipal Long userId,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(mypageService.getMyPosts(userId, pageable)));
    }
}

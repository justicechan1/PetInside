package org.example.petinside.domain.pet.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.petinside.domain.pet.dto.PetPhotoLikeResponse;
import org.example.petinside.domain.pet.dto.PetPhotoRequest;
import org.example.petinside.domain.pet.dto.PetPhotoResponse;
import org.example.petinside.domain.pet.dto.PetRequest;
import org.example.petinside.domain.pet.dto.PetResponse;
import org.example.petinside.domain.pet.dto.PopularPetPhotoResponse;
import org.example.petinside.domain.pet.service.PetService;
import org.example.petinside.global.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "펫 프로필", description = "반려동물 프로필 등록/조회/수정 API (구독자 전용)")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class PetController {

    private final PetService petService;

    @Operation(summary = "인기 펫 사진 조회", description = "좋아요 순 인기 펫 사진 목록 (비회원 공개)")
    @GetMapping("/pets/photos/popular")
    public ResponseEntity<ApiResponse<List<PopularPetPhotoResponse>>> getPopularPhotos(
            @RequestParam(defaultValue = "6") int limit) {
        return ResponseEntity.ok(ApiResponse.success(200, "조회 성공", petService.getPopularPhotos(limit)));
    }

    @Operation(summary = "펫 등록", description = "반려동물 프로필을 등록합니다. 구독자(ACTIVE)만 이용 가능합니다.")
    @PostMapping("/pets")
    public ResponseEntity<ApiResponse<PetResponse>> create(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody PetRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(201, "펫이 등록되었습니다.", petService.create(userId, request)));
    }

    @Operation(summary = "내 펫 목록 조회", description = "내 반려동물 목록을 조회합니다.")
    @GetMapping("/users/me/pets")
    public ResponseEntity<ApiResponse<List<PetResponse>>> getMyPets(
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(ApiResponse.success(200, "조회 성공", petService.getMyPets(userId)));
    }

    @Operation(summary = "공개 펫 목록 조회", description = "특정 사용자의 반려동물 목록을 조회합니다.")
    @GetMapping("/users/{userId}/pets")
    public ResponseEntity<ApiResponse<List<PetResponse>>> getUserPets(
            @PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.success(200, "조회 성공", petService.getMyPets(userId)));
    }

    @Operation(summary = "펫 수정", description = "반려동물 프로필을 수정합니다. 구독자(ACTIVE)만 이용 가능합니다.")
    @PutMapping("/pets/{petId}")
    public ResponseEntity<ApiResponse<PetResponse>> update(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long petId,
            @Valid @RequestBody PetRequest request) {
        return ResponseEntity.ok(ApiResponse.success(200, "펫 정보가 수정되었습니다.", petService.update(userId, petId, request)));
    }

    @Operation(summary = "펫 삭제", description = "반려동물 프로필 및 모든 사진을 삭제합니다. 구독자(ACTIVE)만 이용 가능합니다.")
    @DeleteMapping("/pets/{petId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long petId) {
        petService.delete(userId, petId);
        return ResponseEntity.ok(ApiResponse.success(200, "펫이 삭제되었습니다.", null));
    }

    @Operation(summary = "펫 사진 목록 조회")
    @GetMapping("/pets/{petId}/photos")
    public ResponseEntity<ApiResponse<List<PetPhotoResponse>>> getPhotos(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long petId) {
        return ResponseEntity.ok(ApiResponse.success(200, "조회 성공", petService.getPhotos(petId, userId)));
    }

    @Operation(summary = "펫 사진 좋아요 토글")
    @PostMapping("/pets/{petId}/photos/{photoId}/like")
    public ResponseEntity<ApiResponse<PetPhotoLikeResponse>> toggleLike(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long petId,
            @PathVariable Long photoId) {
        return ResponseEntity.ok(ApiResponse.success(200, "처리 완료", petService.toggleLike(userId, photoId)));
    }

    @Operation(summary = "펫 사진 추가", description = "구독자(ACTIVE)만 이용 가능합니다.")
    @PostMapping("/pets/{petId}/photos")
    public ResponseEntity<ApiResponse<PetPhotoResponse>> addPhoto(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long petId,
            @Valid @RequestBody PetPhotoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(201, "사진이 등록되었습니다.", petService.addPhoto(userId, petId, request)));
    }

    @Operation(summary = "펫 사진 삭제", description = "구독자(ACTIVE)만 이용 가능합니다.")
    @DeleteMapping("/pets/{petId}/photos/{photoId}")
    public ResponseEntity<ApiResponse<Void>> deletePhoto(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long petId,
            @PathVariable Long photoId) {
        petService.deletePhoto(userId, petId, photoId);
        return ResponseEntity.ok(ApiResponse.success(200, "사진이 삭제되었습니다.", null));
    }
}

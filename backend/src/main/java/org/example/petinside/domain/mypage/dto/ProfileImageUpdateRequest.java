package org.example.petinside.domain.mypage.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

// [F-07] 프로필 사진 변경 요청 - 클라이언트가 S3에 업로드 후 받은 URL을 전달
@Getter
@NoArgsConstructor
public class ProfileImageUpdateRequest {
    @NotBlank
    private String imageUrl;
}

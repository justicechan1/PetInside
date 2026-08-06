package org.example.petinside.domain.mypage.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

// [F-06] 비밀번호 변경 요청 - 기존 비밀번호 검증 후 새 비밀번호로 변경
@Getter
@NoArgsConstructor
public class PasswordUpdateRequest {
    @NotBlank
    private String currentPassword;
    @NotBlank
    private String newPassword;
}

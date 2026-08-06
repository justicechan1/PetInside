package org.example.petinside.mypage.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PasswordUpdateRequest {
    @NotBlank
    private String currentPassword;
    @NotBlank
    private String newPassword;
}

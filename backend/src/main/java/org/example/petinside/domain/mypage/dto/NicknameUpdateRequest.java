package org.example.petinside.domain.mypage.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

// [F-05] 닉네임 변경 요청 - @NotBlank로 빈 값 입력 시 400 자동 반환
@Getter
@NoArgsConstructor
public class NicknameUpdateRequest {
    @NotBlank
    private String nickname;
}

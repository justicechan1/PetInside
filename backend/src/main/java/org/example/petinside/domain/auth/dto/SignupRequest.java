package org.example.petinside.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SignupRequest(

        @NotBlank(message = "아이디는 필수입니다.")
        @Pattern(
                regexp = "^[a-z0-9]{4,20}$",
                message = "아이디는 영문 소문자와 숫자로 4~20자여야 합니다."
        )
        String username,

        @NotBlank(message = "비밀번호는 필수입니다.")
        @Pattern(
                regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}$",
                message = "비밀번호는 8자 이상, 영문+숫자+특수문자를 포함해야 합니다."
        )
        String password,

        @NotBlank(message = "닉네임은 필수입니다.")
        @Pattern(
                regexp = "^\\S{2,10}$",
                message = "닉네임은 공백 없이 2~10자여야 합니다."
        )
        String nickname
) {
}

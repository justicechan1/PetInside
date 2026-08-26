package org.example.petinside.domain.mypage.dto;

import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ProfileLayoutUpdateRequest {

    // GRID 또는 LIST만 허용 - 그 외 값이 오면 400 반환
    @Pattern(regexp = "GRID|LIST", message = "profileLayout은 GRID 또는 LIST만 허용됩니다.")
    private String profileLayout;
}

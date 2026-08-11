package org.example.petinside.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record OAuth2ExchangeRequest(

        @NotBlank(message = "code는 필수입니다.")
        String code
) {
}

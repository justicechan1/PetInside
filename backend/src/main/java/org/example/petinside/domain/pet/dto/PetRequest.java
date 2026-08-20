package org.example.petinside.domain.pet.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
public class PetRequest {

    @NotBlank(message = "펫 이름은 필수입니다.")
    private String petName;

    @NotBlank(message = "펫 종류는 필수입니다.")
    private String petType;

    private LocalDate petBirthday; // nullable

    private String petIntro;       // nullable

    private String petImageUrl;    // nullable
}

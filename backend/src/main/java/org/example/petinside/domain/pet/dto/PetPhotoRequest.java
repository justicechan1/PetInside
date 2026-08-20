package org.example.petinside.domain.pet.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class PetPhotoRequest {
    @NotBlank
    private String imageUrl;
    private String caption; // nullable
}

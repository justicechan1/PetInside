package org.example.petinside.domain.pet.dto;

import org.example.petinside.domain.pet.entity.PetPhoto;

import java.time.LocalDateTime;

public record PetPhotoResponse(Long id, String imageUrl, String caption, LocalDateTime createdAt) {
    public static PetPhotoResponse from(PetPhoto p) {
        return new PetPhotoResponse(p.getId(), p.getImageUrl(), p.getCaption(), p.getCreatedAt());
    }
}

package org.example.petinside.domain.pet.dto;

import org.example.petinside.domain.pet.entity.PetPhoto;

import java.time.LocalDateTime;

public record PetPhotoResponse(Long id, String imageUrl, String caption, LocalDateTime createdAt, long likeCount, boolean liked) {
    public static PetPhotoResponse from(PetPhoto p, long likeCount, boolean liked) {
        return new PetPhotoResponse(p.getId(), p.getImageUrl(), p.getCaption(), p.getCreatedAt(), likeCount, liked);
    }
}

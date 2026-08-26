package org.example.petinside.domain.pet.dto;

import org.example.petinside.domain.pet.entity.PetPhoto;

public record PopularPetPhotoResponse(
        Long photoId,
        String imageUrl,
        String caption,
        long likeCount,
        Long ownerId,
        String ownerNickname,
        String petName,
        Long petId
) {
    public static PopularPetPhotoResponse from(PetPhoto photo, long likeCount) {
        return new PopularPetPhotoResponse(
                photo.getId(),
                photo.getImageUrl(),
                photo.getCaption(),
                likeCount,
                photo.getPet().getUser().getId(),
                photo.getPet().getUser().getNickname(),
                photo.getPet().getPetName(),
                photo.getPet().getId()
        );
    }
}

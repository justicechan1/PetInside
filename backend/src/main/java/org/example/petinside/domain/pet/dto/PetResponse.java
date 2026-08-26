package org.example.petinside.domain.pet.dto;

import org.example.petinside.domain.pet.entity.Pet;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record PetResponse(
        Long id,
        String petName,
        String petType,
        LocalDate petBirthday,
        String petIntro,
        String petImageUrl,
        LocalDateTime createdAt
) {
    // 엔티티 → DTO 변환을 레코드 안에 두면 서비스 코드가 깔끔해짐
    public static PetResponse from(Pet pet) {
        return new PetResponse(
                pet.getId(),
                pet.getPetName(),
                pet.getPetType(),
                pet.getPetBirthday(),
                pet.getPetIntro(),
                pet.getPetImageUrl(),
                pet.getCreatedAt()
        );
    }
}

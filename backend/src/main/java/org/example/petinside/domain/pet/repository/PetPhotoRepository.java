package org.example.petinside.domain.pet.repository;

import org.example.petinside.domain.pet.entity.PetPhoto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PetPhotoRepository extends JpaRepository<PetPhoto, Long> {
    List<PetPhoto> findByPetIdOrderByCreatedAtDesc(Long petId);
}

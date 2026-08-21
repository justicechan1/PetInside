package org.example.petinside.domain.pet.repository;

import org.example.petinside.domain.pet.entity.PetPhotoLike;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PetPhotoLikeRepository extends JpaRepository<PetPhotoLike, Long> {
    Optional<PetPhotoLike> findByPhotoIdAndUserId(Long photoId, Long userId);
    long countByPhotoId(Long photoId);
    boolean existsByPhotoIdAndUserId(Long photoId, Long userId);
}

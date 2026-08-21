package org.example.petinside.domain.pet.repository;

import org.example.petinside.domain.pet.entity.PetPhotoLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PetPhotoLikeRepository extends JpaRepository<PetPhotoLike, Long> {
    Optional<PetPhotoLike> findByPhotoIdAndUserId(Long photoId, Long userId);
    long countByPhotoId(Long photoId);
    boolean existsByPhotoIdAndUserId(Long photoId, Long userId);

    @Query("SELECT l.photoId, COUNT(l) FROM PetPhotoLike l GROUP BY l.photoId ORDER BY COUNT(l) DESC")
    List<Object[]> findTopPhotoIdsByLikeCount(org.springframework.data.domain.Pageable pageable);
}

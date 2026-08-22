package org.example.petinside.domain.pet.repository;

import org.example.petinside.domain.pet.entity.PetPhotoLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PetPhotoLikeRepository extends JpaRepository<PetPhotoLike, Long> {
    Optional<PetPhotoLike> findByPhotoIdAndUserId(Long photoId, Long userId);
    long countByPhotoId(Long photoId);
    boolean existsByPhotoIdAndUserId(Long photoId, Long userId);

    @Query(value = "SELECT photo_id, COUNT(*) AS like_count FROM pet_photo_likes GROUP BY photo_id ORDER BY like_count DESC LIMIT :limit", nativeQuery = true)
    List<Object[]> findTopPhotoIdsByLikeCount(@Param("limit") int limit);

    void deleteAllByPhotoId(Long photoId);
}

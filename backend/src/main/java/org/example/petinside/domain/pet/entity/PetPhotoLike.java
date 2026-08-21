package org.example.petinside.domain.pet.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "pet_photo_likes",
        uniqueConstraints = @UniqueConstraint(columnNames = {"photo_id", "user_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PetPhotoLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "photo_id", nullable = false)
    private Long photoId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    public static PetPhotoLike of(Long photoId, Long userId) {
        PetPhotoLike like = new PetPhotoLike();
        like.photoId = photoId;
        like.userId = userId;
        return like;
    }
}

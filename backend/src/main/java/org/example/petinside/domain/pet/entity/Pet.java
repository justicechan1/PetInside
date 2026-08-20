package org.example.petinside.domain.pet.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.petinside.domain.user.entity.User;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "pets")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Pet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 펫은 반드시 주인이 있어야 하므로 nullable = false
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String petName;

    @Column(nullable = false)
    private String petType; // 강아지, 고양이, 앵무새 등 자유 입력

    private LocalDate petBirthday; // nullable - 모르는 경우 생략 가능

    @Column(columnDefinition = "TEXT")
    private String petIntro; // nullable

    private String petImageUrl; // nullable

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    private Pet(User user, String petName, String petType,
                LocalDate petBirthday, String petIntro, String petImageUrl) {
        this.user = user;
        this.petName = petName;
        this.petType = petType;
        this.petBirthday = petBirthday;
        this.petIntro = petIntro;
        this.petImageUrl = petImageUrl;
    }

    public static Pet create(User user, String petName, String petType,
                             LocalDate petBirthday, String petIntro, String petImageUrl) {
        return Pet.builder()
                .user(user)
                .petName(petName)
                .petType(petType)
                .petBirthday(petBirthday)
                .petIntro(petIntro)
                .petImageUrl(petImageUrl)
                .build();
    }

    // 수정은 한 메서드에서 전체 필드를 업데이트 - 부분 수정보다 단순하고 실수가 없음
    public void update(String petName, String petType,
                       LocalDate petBirthday, String petIntro, String petImageUrl) {
        this.petName = petName;
        this.petType = petType;
        this.petBirthday = petBirthday;
        this.petIntro = petIntro;
        this.petImageUrl = petImageUrl;
    }
}

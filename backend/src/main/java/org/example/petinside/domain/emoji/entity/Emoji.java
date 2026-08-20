package org.example.petinside.domain.emoji.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "emoji")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Emoji {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    // 실제 파일이 아니라 스토리지(또는 프론트 정적 리소스)에 업로드된 이미지의 URL만 저장
    @Column(name = "image_url", nullable = false, length = 255)
    private String imageUrl;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Builder
    public Emoji(String name, String imageUrl) {
        this.name = name;
        this.imageUrl = imageUrl;
        this.isActive = true;
    }
}

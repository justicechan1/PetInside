package org.example.petinside.domain.post.repository;

import org.example.petinside.cha.domain.post.entity.PostImage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostImageRepository extends JpaRepository<PostImage, Long> {
}
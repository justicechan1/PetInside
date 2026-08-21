package org.example.petinside.domain.emoji.repository;

import org.example.petinside.domain.emoji.entity.PostEmoji;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostEmojiRepository extends JpaRepository<PostEmoji, Long> {
}

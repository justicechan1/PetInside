package org.example.petinside.domain.emoji.repository;

import org.example.petinside.domain.emoji.entity.CommentEmoji;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentEmojiRepository extends JpaRepository<CommentEmoji, Long> {
}

package org.example.petinside.domain.emoji.repository;

import org.example.petinside.domain.emoji.entity.Emoji;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmojiRepository extends JpaRepository<Emoji, Long> {
    List<Emoji> findAllByIsActiveTrue();
}

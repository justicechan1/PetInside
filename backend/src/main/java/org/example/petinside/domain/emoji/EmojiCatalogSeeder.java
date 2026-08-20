package org.example.petinside.domain.emoji;

import lombok.RequiredArgsConstructor;
import org.example.petinside.domain.emoji.entity.Emoji;
import org.example.petinside.domain.emoji.repository.EmojiRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

// 이모지 카탈로그는 업로드 API 없이 고정 시드 데이터로만 운영하기로 팀 확정(2026-08-12).
// data.sql 대신 ApplicationRunner를 쓰는 이유: ddl-auto=update인 실 MySQL에서는 매 재기동마다
// data.sql이 재실행되면 중복 INSERT가 쌓이므로, count()==0일 때만 채우는 idempotent 방식이 필요함.
@Component
@RequiredArgsConstructor
public class EmojiCatalogSeeder implements ApplicationRunner {

    private final EmojiRepository emojiRepository;

    @Override
    public void run(ApplicationArguments args) {
        if (emojiRepository.count() > 0) {
            return;
        }

        List<Emoji> catalog = List.of(
                Emoji.builder().name("웃음").imageUrl("/emojis/smile.png").build(),
                Emoji.builder().name("슬픔").imageUrl("/emojis/sad.png").build(),
                Emoji.builder().name("하트").imageUrl("/emojis/heart.png").build(),
                Emoji.builder().name("최고").imageUrl("/emojis/thumbsup.png").build(),
                Emoji.builder().name("놀람").imageUrl("/emojis/surprised.png").build(),
                Emoji.builder().name("화남").imageUrl("/emojis/angry.png").build()
        );

        emojiRepository.saveAll(catalog);
    }
}

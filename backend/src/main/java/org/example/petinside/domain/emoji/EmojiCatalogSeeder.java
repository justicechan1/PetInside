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
                // 강아지 - 캡션 있는 세트
                Emoji.builder().name("신나!").imageUrl("/emojis/excited.png").build(),
                Emoji.builder().name("행복해!").imageUrl("/emojis/happy.png").build(),
                Emoji.builder().name("생각 중...").imageUrl("/emojis/thinking.png").build(),
                Emoji.builder().name("덥다!").imageUrl("/emojis/hot.png").build(),
                Emoji.builder().name("멀미...").imageUrl("/emojis/carsick.png").build(),
                Emoji.builder().name("뭐야?").imageUrl("/emojis/what.png").build(),
                Emoji.builder().name("밥 줘!").imageUrl("/emojis/feedme.png").build(),
                Emoji.builder().name("안녕!").imageUrl("/emojis/hello.png").build(),
                Emoji.builder().name("간식?").imageUrl("/emojis/snack.png").build(),
                // 강아지 - 투명 배경 스티커 세트
                Emoji.builder().name("만세!").imageUrl("/emojis/yay.png").build(),
                Emoji.builder().name("긴장...").imageUrl("/emojis/nervous.png").build(),
                Emoji.builder().name("졸려").imageUrl("/emojis/sleepy.png").build(),
                Emoji.builder().name("슬퍼요").imageUrl("/emojis/sad.png").build(),
                Emoji.builder().name("화났어").imageUrl("/emojis/angry.png").build(),
                Emoji.builder().name("대박!").imageUrl("/emojis/wow.png").build(),
                Emoji.builder().name("사랑해").imageUrl("/emojis/love.png").build(),
                Emoji.builder().name("냠냠").imageUrl("/emojis/yum.png").build(),
                // 고양이 세트
                Emoji.builder().name("잡았다!").imageUrl("/emojis/cat_caught.png").build(),
                Emoji.builder().name("놀아줘!").imageUrl("/emojis/cat_playwithme.png").build(),
                Emoji.builder().name("고양이 더미!").imageUrl("/emojis/cat_pile.png").build(),
                Emoji.builder().name("잘 자요").imageUrl("/emojis/cat_sleepwell.png").build(),
                Emoji.builder().name("엿보기!").imageUrl("/emojis/cat_peek.png").build(),
                Emoji.builder().name("안 돼.").imageUrl("/emojis/cat_no.png").build(),
                Emoji.builder().name("다 함께!").imageUrl("/emojis/cat_together.png").build(),
                Emoji.builder().name("간다!").imageUrl("/emojis/cat_going.png").build()
        );

        emojiRepository.saveAll(catalog);
    }
}

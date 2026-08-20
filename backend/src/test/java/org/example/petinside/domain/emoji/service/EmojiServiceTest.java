package org.example.petinside.domain.emoji.service;

import org.example.petinside.domain.emoji.dto.EmojiResponse;
import org.example.petinside.domain.emoji.entity.Emoji;
import org.example.petinside.domain.emoji.repository.EmojiRepository;
import org.example.petinside.domain.subscription.entity.SubscriptionStatus;
import org.example.petinside.domain.subscription.repository.SubscriptionRepository;
import org.example.petinside.domain.user.entity.User;
import org.example.petinside.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmojiServiceTest {

    @Mock
    private EmojiRepository emojiRepository;
    @Mock
    private SubscriptionRepository subscriptionRepository;
    @Mock
    private UserRepository userRepository;

    private EmojiService emojiService;

    private User user;

    @BeforeEach
    void setUp() {
        emojiService = new EmojiService(emojiRepository, subscriptionRepository, userRepository);

        user = User.builder()
                .username("user")
                .password("encoded")
                .nickname("user-nick")
                .role("USER")
                .build();
        ReflectionTestUtils.setField(user, "id", 1L);
    }

    private Emoji emoji(Long id, String name) {
        Emoji e = Emoji.builder().name(name).imageUrl("http://emoji/" + name).build();
        ReflectionTestUtils.setField(e, "id", id);
        return e;
    }

    @Nested
    @DisplayName("getMyEmojis")
    class GetMyEmojis {

        @Test
        @DisplayName("ACTIVE 구독자면 활성 이모지 카탈로그 전체를 반환한다")
        void activeSubscriber_returnsCatalog() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(subscriptionRepository.existsByUserAndStatus(user, SubscriptionStatus.ACTIVE)).thenReturn(true);
            when(emojiRepository.findAllByIsActiveTrue()).thenReturn(List.of(emoji(1L, "웃음"), emoji(2L, "하트")));

            List<EmojiResponse> result = emojiService.getMyEmojis(1L);

            assertThat(result).hasSize(2);
            assertThat(result).extracting(EmojiResponse::getName).containsExactly("웃음", "하트");
        }

        @Test
        @DisplayName("구독자가 아니면 403 예외가 발생한다")
        void notSubscribed_throws403() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(subscriptionRepository.existsByUserAndStatus(user, SubscriptionStatus.ACTIVE)).thenReturn(false);

            assertThatThrownBy(() -> emojiService.getMyEmojis(1L))
                    .isInstanceOf(org.example.petinside.global.exception.CustomException.class)
                    .satisfies(e -> assertThat(((org.example.petinside.global.exception.CustomException) e).getStatus()).isEqualTo(403));
        }
    }

    @Nested
    @DisplayName("resolveEmojisForAttach")
    class ResolveEmojisForAttach {

        @Test
        @DisplayName("emojiIds가 비어있으면 구독 여부와 무관하게 빈 목록을 반환한다")
        void emptyEmojiIds_returnsEmptyList() {
            List<Emoji> result = emojiService.resolveEmojisForAttach(1L, List.of());

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("emojiIds가 null이면 빈 목록을 반환한다")
        void nullEmojiIds_returnsEmptyList() {
            List<Emoji> result = emojiService.resolveEmojisForAttach(1L, null);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("ACTIVE 구독자면 요청 순서를 유지한 이모지 목록을 반환한다")
        void activeSubscriber_returnsEmojisInRequestOrder() {
            Emoji heart = emoji(2L, "하트");
            Emoji smile = emoji(1L, "웃음");

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(subscriptionRepository.existsByUserAndStatus(user, SubscriptionStatus.ACTIVE)).thenReturn(true);
            when(emojiRepository.findAllById(List.of(2L, 1L))).thenReturn(List.of(smile, heart));

            List<Emoji> result = emojiService.resolveEmojisForAttach(1L, List.of(2L, 1L));

            assertThat(result).extracting(Emoji::getId).containsExactly(2L, 1L);
        }

        @Test
        @DisplayName("구독자가 아니면 403 예외가 발생한다")
        void notSubscribed_throws403() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(subscriptionRepository.existsByUserAndStatus(user, SubscriptionStatus.ACTIVE)).thenReturn(false);

            assertThatThrownBy(() -> emojiService.resolveEmojisForAttach(1L, List.of(1L)))
                    .isInstanceOf(org.example.petinside.global.exception.CustomException.class)
                    .satisfies(e -> assertThat(((org.example.petinside.global.exception.CustomException) e).getStatus()).isEqualTo(403));
        }

        @Test
        @DisplayName("존재하지 않는 emojiId가 포함되면 400 예외가 발생한다")
        void unknownEmojiId_throws400() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(subscriptionRepository.existsByUserAndStatus(user, SubscriptionStatus.ACTIVE)).thenReturn(true);
            when(emojiRepository.findAllById(List.of(999L))).thenReturn(List.of());

            assertThatThrownBy(() -> emojiService.resolveEmojisForAttach(1L, List.of(999L)))
                    .isInstanceOf(org.example.petinside.global.exception.CustomException.class)
                    .satisfies(e -> assertThat(((org.example.petinside.global.exception.CustomException) e).getStatus()).isEqualTo(400));
        }
    }
}

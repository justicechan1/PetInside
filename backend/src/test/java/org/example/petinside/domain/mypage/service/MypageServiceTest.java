package org.example.petinside.domain.mypage.service;

import org.example.petinside.domain.mypage.dto.PublicProfileResponse;
import org.example.petinside.domain.post.repository.PostRepository;
import org.example.petinside.domain.subscription.entity.SubscriptionStatus;
import org.example.petinside.domain.subscription.repository.SubscriptionRepository;
import org.example.petinside.domain.user.entity.User;
import org.example.petinside.domain.user.repository.SocialAccountRepository;
import org.example.petinside.domain.user.repository.UserRepository;
import org.example.petinside.global.exception.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MypageServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PostRepository postRepository;
    @Mock
    private SocialAccountRepository socialAccountRepository;
    @Mock
    private SubscriptionRepository subscriptionRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    private MypageService mypageService;

    private User user;

    @BeforeEach
    void setUp() {
        mypageService = new MypageService(userRepository, postRepository, socialAccountRepository, subscriptionRepository, passwordEncoder);

        user = User.builder()
                .username("author")
                .password("encoded")
                .nickname("author-nick")
                .role("USER")
                .build();
        ReflectionTestUtils.setField(user, "id", 1L);
        user.updateProfileImageUrl("http://profile/author.png");
    }

    @Nested
    @DisplayName("getPublicProfile")
    class GetPublicProfile {

        @Test
        @DisplayName("활성 구독이 없으면 membershipTier가 null이다")
        void getPublicProfile_noActiveSubscription_membershipTierIsNull() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(postRepository.countByAuthorIdAndIsDeletedFalse(1L)).thenReturn(3L);
            when(subscriptionRepository.existsByUserAndStatus(user, SubscriptionStatus.ACTIVE)).thenReturn(false);

            PublicProfileResponse response = mypageService.getPublicProfile(1L);

            assertThat(response.getId()).isEqualTo(1L);
            assertThat(response.getNickname()).isEqualTo("author-nick");
            assertThat(response.getProfileImageUrl()).isEqualTo("http://profile/author.png");
            assertThat(response.getPostCount()).isEqualTo(3L);
            assertThat(response.getBadges()).isEmpty();
            assertThat(response.getMembershipTier()).isNull();
        }

        @Test
        @DisplayName("활성 구독이 있으면 membershipTier가 SUBSCRIBER다")
        void getPublicProfile_activeSubscription_membershipTierIsSubscriber() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(postRepository.countByAuthorIdAndIsDeletedFalse(1L)).thenReturn(0L);
            when(subscriptionRepository.existsByUserAndStatus(user, SubscriptionStatus.ACTIVE)).thenReturn(true);

            PublicProfileResponse response = mypageService.getPublicProfile(1L);

            assertThat(response.getMembershipTier()).isEqualTo("SUBSCRIBER");
        }

        @Test
        @DisplayName("존재하지 않는 회원이면 404 예외가 발생한다")
        void getPublicProfile_userNotFound_throws() {
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> mypageService.getPublicProfile(999L))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> assertThat(((CustomException) e).getStatus()).isEqualTo(404));
        }
    }

    @Nested
    @DisplayName("getPublicPosts")
    class GetPublicPosts {

        @Test
        @DisplayName("존재하는 회원이면 게시글 목록을 반환한다")
        void getPublicPosts_success() {
            when(userRepository.existsById(1L)).thenReturn(true);
            Pageable pageable = PageRequest.of(0, 10);
            when(postRepository.searchMyPosts(eq(1L), isNull(), isNull(), eq(pageable)))
                    .thenReturn(new PageImpl<>(List.of()));

            var result = mypageService.getPublicPosts(1L, null, null, pageable);

            assertThat(result.getContent()).isEmpty();
        }

        @Test
        @DisplayName("존재하지 않는 회원이면 404 예외가 발생한다")
        void getPublicPosts_userNotFound_throws() {
            when(userRepository.existsById(999L)).thenReturn(false);
            Pageable pageable = PageRequest.of(0, 10);

            assertThatThrownBy(() -> mypageService.getPublicPosts(999L, null, null, pageable))
                    .isInstanceOf(CustomException.class);
        }
    }
}

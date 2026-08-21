package org.example.petinside.domain.emoji.service;

import lombok.RequiredArgsConstructor;
import org.example.petinside.domain.emoji.dto.EmojiResponse;
import org.example.petinside.domain.emoji.entity.Emoji;
import org.example.petinside.domain.emoji.repository.EmojiRepository;
import org.example.petinside.domain.subscription.entity.SubscriptionStatus;
import org.example.petinside.domain.subscription.repository.SubscriptionRepository;
import org.example.petinside.domain.user.entity.User;
import org.example.petinside.domain.user.repository.UserRepository;
import org.example.petinside.global.exception.CustomException;
import org.example.petinside.global.exception.UserNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// 이모지 도메인 서비스 - F-26(목록 조회), F-27(게시글/댓글 첨부 검증)
// F-28(회수)은 별도 로직 없음: SUBSCRIPTION.status가 EXPIRED로 바뀌면 아래 두 메서드가
// 실시간 조회하는 구독 상태가 자동으로 바뀌어 신규 첨부가 막힘(구독 도메인 내부 처리로 충분).
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmojiService {

    private final EmojiRepository emojiRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;

    // F-26 보유 이모지 목록 조회 - 활성 구독자만 카탈로그 전체를 볼 수 있음
    public List<EmojiResponse> getMyEmojis(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        if (!subscriptionRepository.existsByUserAndStatus(user, SubscriptionStatus.ACTIVE)) {
            throw new CustomException(403, "구독자만 이용 가능합니다");
        }

        return emojiRepository.findAllByIsActiveTrue().stream()
                .map(emoji -> new EmojiResponse(emoji.getId(), emoji.getImageUrl(), emoji.getName()))
                .collect(Collectors.toList());
    }

    // F-27 게시글/댓글 첨부용 이모지 검증 - 요청한 emojiIds 순서를 그대로 유지해 반환(sortOrder에 사용)
    // emojiIds가 비어있으면 구독 여부와 무관하게 통과(첨부 자체가 없으므로 검증 불필요)
    public List<Emoji> resolveEmojisForAttach(Long userId, List<Long> emojiIds) {
        if (emojiIds == null || emojiIds.isEmpty()) {
            return List.of();
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        if (!subscriptionRepository.existsByUserAndStatus(user, SubscriptionStatus.ACTIVE)) {
            throw new CustomException(403, "구독자만 이모티콘을 사용할 수 있습니다");
        }

        Map<Long, Emoji> emojiById = emojiRepository.findAllById(emojiIds).stream()
                .collect(Collectors.toMap(Emoji::getId, emoji -> emoji, (a, b) -> a, LinkedHashMap::new));

        if (emojiById.size() != new HashSet<>(emojiIds).size()) {
            throw new CustomException(400, "존재하지 않는 emojiId가 포함되어 있습니다.");
        }

        return emojiIds.stream()
                .map(emojiById::get)
                .collect(Collectors.toList());
    }
}
